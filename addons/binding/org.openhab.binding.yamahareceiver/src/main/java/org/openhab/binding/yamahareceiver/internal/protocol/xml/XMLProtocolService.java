package org.openhab.binding.yamahareceiver.internal.protocol.xml;

import org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants;
import org.openhab.binding.yamahareceiver.internal.protocol.AbstractConnection;
import org.openhab.binding.yamahareceiver.internal.protocol.ProtocolService;
import org.openhab.binding.yamahareceiver.internal.protocol.ReceivedMessageParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class XMLProtocolService implements ProtocolService {

    private final Logger logger = LoggerFactory.getLogger(XMLProtocolService.class);

    private final WeakReference<AbstractConnection> comReference;

    public XMLProtocolService(AbstractConnection com) {
        this.comReference = new WeakReference<>(com);
    }

    @Override
    public Collection<InputDto> getInputs(YamahaReceiverBindingConstants.Zone zone)
            throws IOException, ReceivedMessageParseException {

        String response = comReference.get().sendReceive(XMLUtils.wrZone(zone, "<Input><Input_Sel_Item>GetParam</Input_Sel_Item></Input>"));
        Document doc = XMLUtils.xml(response);
        if (doc.getFirstChild() == null) {
            throw new ReceivedMessageParseException("<Input><Input_Sel_Item>GetParam failed: " + response);
        }
        Node inputSelItem = XMLUtils.getNode(doc.getFirstChild(), zone + "/Input/Input_Sel_Item");
        NodeList items = inputSelItem.getChildNodes();

        List<InputDto> inputs = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String param = item.getElementsByTagName("Param").item(0).getTextContent();
            boolean writable = item.getElementsByTagName("RW").item(0).getTextContent().contains("W");
            inputs.add(new InputDto(param, writable));
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Zone {} - inputs: {}", zone, inputs.stream().map(InputDto::toString).collect(Collectors.joining(", ")));
        }

        return inputs;
    }
}
