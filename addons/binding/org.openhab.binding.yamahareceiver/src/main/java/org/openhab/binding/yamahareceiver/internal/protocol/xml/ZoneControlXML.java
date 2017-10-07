/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.yamahareceiver.internal.protocol.xml;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.function.Supplier;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants;
import org.openhab.binding.yamahareceiver.internal.protocol.InputConverter;
import org.openhab.binding.yamahareceiver.internal.protocol.AbstractConnection;
import org.openhab.binding.yamahareceiver.internal.protocol.ReceivedMessageParseException;
import org.openhab.binding.yamahareceiver.internal.protocol.ZoneControl;
import org.openhab.binding.yamahareceiver.internal.state.ZoneControlState;
import org.openhab.binding.yamahareceiver.internal.state.ZoneControlStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants.VOLUME_MIN;
import static org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants.VOLUME_RANGE;

/**
 * The zone protocol class is used to control one zone of a Yamaha receiver with HTTP/xml.
 * No state will be saved in here, but in {@link ZoneControlState} instead.
 *
 * @author David Gräff - Refactored
 * @author Eric Thill
 * @author Ben Jones
 * @author Tomasz Maruszak - Refactoring, input mapping fix, Straight surround fix
 *
 */
public class ZoneControlXML implements ZoneControl {

    private final Logger logger = LoggerFactory.getLogger(ZoneControlXML.class);

    private static final String ON = "On";
    private static final String OFF = "Off";
    private static final String STRAIGHT = "Straight";

    private final ZoneControlStateListener observer;
    private final Supplier<InputConverter> inputConverterSupplier;
    private final WeakReference<AbstractConnection> comReference;
    private final YamahaReceiverBindingConstants.Zone zone;

    public ZoneControlXML(AbstractConnection xml, YamahaReceiverBindingConstants.Zone zone,
                          ZoneControlStateListener observer, Supplier<InputConverter> inputConverterSupplier) {
        this.comReference = new WeakReference<>(xml);
        this.zone = zone;
        this.observer = observer;
        this.inputConverterSupplier = inputConverterSupplier;
    }

    /**
     * Return the zone
     */
    public YamahaReceiverBindingConstants.Zone getZone() {
        return zone;
    }

    @Override
    public void setPower(boolean on) throws IOException, ReceivedMessageParseException {
        String cmd = on ? ON : "Standby";
        comReference.get().send(XMLUtils.wrZone(zone, "<Power_Control><Power>" + cmd + "</Power></Power_Control>"));
        update();
    }

    /**
     * Sets the absolute volume in decibel.
     *
     * @param volume Absolute value in decibel ([-80,+12]).
     * @throws IOException
     */
    @Override
    public void setVolumeDB(float volume) throws IOException, ReceivedMessageParseException {
        if (volume < VOLUME_MIN) {
            volume = VOLUME_MIN;
        }
        if (volume > YamahaReceiverBindingConstants.VOLUME_MAX) {
            volume = YamahaReceiverBindingConstants.VOLUME_MAX;
        }
        int vol = (int) volume * 10;
        comReference.get().send(XMLUtils.wrZone(zone,
                "<Volume><Lvl><Val>" + String.valueOf(vol) + "</Val><Exp>1</Exp><Unit>dB</Unit></Lvl></Volume>"));

        update();
    }

    /**
     * Sets the volume in percent
     *
     * @param volume
     * @throws IOException
     */
    @Override
    public void setVolume(float volume) throws IOException, ReceivedMessageParseException {
        if (volume < 0) {
            volume = 0;
        }
        if (volume > 100) {
            volume = 100;
        }
        // Compute value in db
        setVolumeDB(volume * VOLUME_RANGE / 100.0f + VOLUME_MIN);
    }

    /**
     * Increase or decrease the volume by the given percentage.
     *
     * @param percent
     * @throws IOException
     */
    @Override
    public void setVolumeRelative(ZoneControlState state, float percent)
            throws IOException, ReceivedMessageParseException {
        setVolume(state.volume + percent);
    }

    @Override
    public void setMute(boolean mute) throws IOException, ReceivedMessageParseException {
        String cmd = mute ? ON : OFF;
        comReference.get().send(XMLUtils.wrZone(zone, "<Volume><Mute>" + cmd + "</Mute></Volume>"));
        update();
    }

    @Override
    public void setInput(String name) throws IOException, ReceivedMessageParseException {
        name = inputConverterSupplier.get().toCommandName(name);
        comReference.get().send(XMLUtils.wrZone(zone, "<Input><Input_Sel>" + name + "</Input_Sel></Input>"));
        update();
    }

    @Override
    public void setSurroundProgram(String name) throws IOException, ReceivedMessageParseException {
        String cmd = name.equalsIgnoreCase(STRAIGHT)
                ? "<Straight>On</Straight>"
                : String.format("<Sound_Program>%s</Sound_Program>", name) ;

        comReference.get().send(XMLUtils.wrZone(zone,"<Surround><Program_Sel><Current>" + cmd + "</Current></Program_Sel></Surround>"));
        update();
    }

    @Override
    public void update() throws IOException, ReceivedMessageParseException {
        if (observer == null) {
            return;
        }

        AbstractConnection com = comReference.get();
        String response = com.sendReceive(XMLUtils.wrZone(zone, "<Basic_Status>GetParam</Basic_Status>"));
        Document doc = XMLUtils.xml(response);
        if (doc.getFirstChild() == null) {
            throw new ReceivedMessageParseException("<Basic_Status>GetParam failed: " + response);
        }
        Node basicStatus = XMLUtils.getNode(doc.getFirstChild(), zone + "/Basic_Status");

        String value;

        ZoneControlState state = new ZoneControlState();

        value = XMLUtils.getNodeContentOrDefault(basicStatus, "Power_Control/Power", "");
        state.power = ON.equalsIgnoreCase(value);

        value = XMLUtils.getNodeContentOrDefault(basicStatus, "Volume/Mute", "");
        state.mute = ON.equalsIgnoreCase(value);

        value = XMLUtils.getNodeContentOrDefault(basicStatus, "Volume/Lvl/Val", String.valueOf(VOLUME_MIN));
        state.volume = Float.parseFloat(value) * .1f; // in DB
        state.volume = (state.volume + -VOLUME_MIN) * 100.0f / VOLUME_RANGE; // in percent
        if (state.volume < 0 || state.volume > 100) {
            logger.error("Received volume is out of range: {}", state.volume);
            state.volume = 0;
        }

        value = XMLUtils.getNodeContentOrDefault(basicStatus, "Input/Input_Sel", "");
        state.inputID = inputConverterSupplier.get().fromStateName(value);
        if (StringUtils.isBlank(state.inputID)) {
            throw new ReceivedMessageParseException("Expected inputID. Failed to read Input/Input_Sel");
        }

        // Some receivers may use Src_Name instead?
        value = XMLUtils.getNodeContentOrDefault(basicStatus, "Input/Input_Sel_Item_Info/Title", "");
        state.inputName = value;

        value = XMLUtils.getNodeContentOrDefault(basicStatus, "Surround/Program_Sel/Current/Straight", "");
        boolean straightOn = ON.equalsIgnoreCase(value);

        value = XMLUtils.getNodeContentOrDefault(basicStatus, "Surround/Program_Sel/Current/Sound_Program", "");
        // Surround is either in straight mode or sound program
        state.surroundProgram = straightOn ? STRAIGHT : value;

        logger.trace("Zone {} state - power: {}, mute: {}, volume: {}, input: {}, surroundProgram: {}",
                zone, state.power, state.mute, state.volume, state.inputID, state.surroundProgram);

        observer.zoneStateChanged(state);
    }
}
