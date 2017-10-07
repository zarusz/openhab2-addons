package org.openhab.binding.yamahareceiver.internal.protocol.xml;

import org.openhab.binding.yamahareceiver.internal.protocol.AbstractConnection;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import static org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants.INPUT_MUSIC_CAST_LINK;
import static org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants.INPUT_NETRADIO;

public abstract class AbstractInputControlXML {

    protected final WeakReference<AbstractConnection> comReference;
    protected final String inputID;

    private static final Map<String, String> INPUT_TO_ELEMENT;

    static {
        INPUT_TO_ELEMENT = new HashMap<>();
        INPUT_TO_ELEMENT.put(INPUT_NETRADIO, "NET_RADIO");
        INPUT_TO_ELEMENT.put(INPUT_MUSIC_CAST_LINK, "MusicCast_Link");
    }

    protected AbstractInputControlXML(String inputID, AbstractConnection com) {
        this.inputID = inputID;
        this.comReference = new WeakReference<>(com);
    }

    /**
     * Wraps the XML message with the inputID tags. Example with inputID=NET_RADIO:
     * <NET_RADIO>message</NET_RADIO>.
     *
     * @param message XML message
     * @return
     */
    protected String wrInput(String message) {
        String elementName = INPUT_TO_ELEMENT.getOrDefault(inputID, inputID);
        return String.format("<%s>%s</%s>", elementName, message, elementName);
    }
}
