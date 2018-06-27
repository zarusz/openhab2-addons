package org.openhab.binding.yamahareceiver.internal.protocol.xml;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.openhab.binding.yamahareceiver.internal.config.YamahaBridgeConfig;
import org.openhab.binding.yamahareceiver.internal.state.PlayInfoState;
import org.openhab.binding.yamahareceiver.internal.state.PlayInfoStateListener;
import org.openhab.binding.yamahareceiver.internal.state.PresetInfoState;
import org.openhab.binding.yamahareceiver.internal.state.PresetInfoStateListener;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openhab.binding.yamahareceiver.TestModels.RX_S601D;
import static org.openhab.binding.yamahareceiver.TestModels.RX_V3900;
import static org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants.Inputs.INPUT_NET_RADIO;
import static org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants.Inputs.INPUT_SPOTIFY;
import static org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants.Inputs.INPUT_TUNER;

public class InputWithPresetControlXMLTest extends AbstractZoneControlXMLTest {

    private InputWithPresetControlXML subject;

    @Mock
    private PresetInfoStateListener presetInfoStateListener;

    @Captor
    private ArgumentCaptor<PresetInfoState> presetInfoStateArg;

    private void given(String model, String input, Consumer<ModelContext> setup) throws Exception {
        ctx.prepareForModel(model);

        DeviceInformationXML deviceInformation = new DeviceInformationXML(con, deviceInformationState);
        deviceInformation.update();

        setup.accept(ctx);

        subject = new InputWithPresetControlXML(input, con, presetInfoStateListener, deviceInformationState);
    }

    @Test
    public void given_RX_S601D_and_NET_RADIO_when_preset1_then_sendsProperCommand() throws Exception {
        given(RX_S601D, INPUT_NET_RADIO, ctx -> {
            ctx.respondWith("<NET_RADIO><Play_Control><Preset><Preset_Sel_Item>GetParam</Preset_Sel_Item></Preset></Play_Control></NET_RADIO>", "NET_RADIO_Play_Control_Preset_Preset_Sel_Item.xml");
            ctx.respondWith("<NET_RADIO><Play_Control><Preset><Preset_Sel>GetParam</Preset_Sel></Preset></Play_Control></NET_RADIO>", "NET_RADIO_Play_Control_Preset_Preset_Sel.xml");
        });

        // when
        subject.selectItemByPresetNumber(1);

        // then
        verify(presetInfoStateListener).presetInfoUpdated(presetInfoStateArg.capture());
        PresetInfoState state = presetInfoStateArg.getValue();

        verify(con).send(eq("<NET_RADIO><Play_Control><Preset><Preset_Sel>1</Preset_Sel></Preset></Play_Control></NET_RADIO>"));
        assertEquals("1 : NET RADIO  Chilli ZET PL", state.presetChannelNames.get(0).getName());
        assertEquals(1, state.presetChannelNames.get(0).getValue());
        assertEquals("2 : NET RADIO  Polskie Radio 24", state.presetChannelNames.get(1).getName());
        assertEquals(2, state.presetChannelNames.get(1).getValue());
    }

    @Test
    public void given_RX_V3900_and_NET_RADIO_when_preset1_then_sendsProperCommand() throws Exception {
        given(RX_V3900, INPUT_NET_RADIO, ctx -> {
            ctx.respondWith("<NET_USB><Play_Control><Preset><Preset_Sel_Item>GetParam</Preset_Sel_Item></Preset></Play_Control></NET_USB>", "NET_USB_Play_Control_Preset_Preset_Sel_Item.xml");
            ctx.respondWith("<NET_USB><Play_Control><Preset><Preset_Sel>GetParam</Preset_Sel></Preset></Play_Control></NET_USB>", "NET_USB_Play_Control_Preset_Preset_Sel.xml");
        });

        // when
        subject.selectItemByPresetNumber(1);

        // then
        verify(con).send(eq("<NET_USB><Play_Control><Preset><Preset_Sel>1</Preset_Sel></Preset></Play_Control></NET_USB>"));
    }

    @Test
    public void given_RX_V3900_and_TUNER_when_preset1_then_sendsProperCommand() throws Exception {
        given(RX_V3900, INPUT_TUNER, ctx -> {
            ctx.respondWith("<Tuner><Play_Control><Preset><Preset_Sel_Item>GetParam</Preset_Sel_Item></Preset></Play_Control></Tuner>", "Tuner_Play_Control_Preset_Preset_Sel_Item.xml");
            ctx.respondWith("<Tuner><Play_Control><Preset><Preset_Sel>GetParam</Preset_Sel></Preset></Play_Control></Tuner>", "Tuner_Play_Control_Preset_Preset_Sel.xml");
        });

        // when
        subject.selectItemByPresetNumber(101);
        subject.selectItemByPresetNumber(212);

        // then
        verify(con).send(eq("<Tuner><Play_Control><Preset><Preset_Sel>A1</Preset_Sel></Preset></Play_Control></Tuner>"));
        verify(con).send(eq("<Tuner><Play_Control><Preset><Preset_Sel>B12</Preset_Sel></Preset></Play_Control></Tuner>"));
    }

    @Test
    public void given_RX_V3900_and_TUNER_when_update_then_populatesStateCorrectly() throws Exception {
        given(RX_V3900, INPUT_TUNER, ctx -> {
            ctx.respondWith("<Tuner><Play_Control><Preset><Preset_Sel_Item>GetParam</Preset_Sel_Item></Preset></Play_Control></Tuner>", "Tuner_Play_Control_Preset_Preset_Sel_Item.xml");
            ctx.respondWith("<Tuner><Play_Control><Preset><Preset_Sel>GetParam</Preset_Sel></Preset></Play_Control></Tuner>", "Tuner_Play_Control_Preset_Preset_Sel.xml");
        });

        // when
        subject.update();

        // then
        verify(presetInfoStateListener).presetInfoUpdated(presetInfoStateArg.capture());
        PresetInfoState state = presetInfoStateArg.getValue();

        // A1
        assertEquals(101, state.presetChannel);

        assertEquals(16, state.presetChannelNames.size());
        assertEquals("A1", state.presetChannelNames.get(0).getName());
        assertEquals(101, state.presetChannelNames.get(0).getValue());
        assertEquals("A2", state.presetChannelNames.get(1).getName());
        assertEquals(102, state.presetChannelNames.get(1).getValue());
    }
}
