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
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants;
import org.openhab.binding.yamahareceiver.internal.protocol.*;
import org.openhab.binding.yamahareceiver.internal.state.AvailableInputState;
import org.openhab.binding.yamahareceiver.internal.state.AvailableInputStateListener;
import org.openhab.binding.yamahareceiver.internal.state.ZoneControlState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The zone protocol class is used to control one zone of a Yamaha receiver with HTTP/xml.
 * No state will be saved in here, but in {@link ZoneControlState} instead.
 *
 * @author David Gräff - Initial contribution
 * @author Tomasz Maruszak - Refactoring
 * @author Tomasz Maruszak - Input mapping fix
 *
 */
public class ZoneAvailableInputsXML implements ZoneAvailableInputs {
    private final Logger logger = LoggerFactory.getLogger(ZoneAvailableInputsXML.class);

    private final AvailableInputStateListener observer;
    private final Supplier<InputConverter> inputConverterSupplier;
    private final YamahaReceiverBindingConstants.Zone zone;
    private final ProtocolService protocol;

    public ZoneAvailableInputsXML(ProtocolService protocolService,
                                  YamahaReceiverBindingConstants.Zone zone,
                                  AvailableInputStateListener observer,
                                  Supplier<InputConverter> inputConverterSupplier) {

        this.zone = zone;
        this.observer = observer;
        this.inputConverterSupplier = inputConverterSupplier;
        this.protocol = protocolService;
    }

    /**
     * Return the zone
     */
    public YamahaReceiverBindingConstants.Zone getZone() {
        return zone;
    }

    public void update() throws IOException, ReceivedMessageParseException {
        if (observer == null) {
            return;
        }

        Collection<XMLProtocolService.InputDto> inputs = protocol.getInputs(zone);

        AvailableInputState state = new AvailableInputState();

        inputs.stream().filter(XMLProtocolService.InputDto::isWritable).forEach(x -> {
            String inputName = inputConverterSupplier.get().fromStateName(x.getParam());
            state.availableInputs.put(inputName, x.getParam());
        });

        if (logger.isTraceEnabled()) {
            logger.trace("Zone {} - available inputs: {}", zone, state.availableInputs.keySet().stream().collect(Collectors.joining(", ")));
        }

        observer.availableInputsChanged(state);
    }
}
