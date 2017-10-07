package org.openhab.binding.yamahareceiver.internal.protocol.xml;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants;
import org.openhab.binding.yamahareceiver.internal.protocol.InputConverter;
import org.openhab.binding.yamahareceiver.internal.protocol.ProtocolService;
import org.openhab.binding.yamahareceiver.internal.protocol.ReceivedMessageParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants.INPUT_BLUETOOTH;
import static org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants.INPUT_NETRADIO;
import static org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants.INPUT_SPOTIFY;

/**
 * XML implementation of {@link InputConverter}.
 *
 * @author Tomasz Maruszak - Input mapping fix
 *
 */
public class InputConverterXML implements InputConverter {

    private Logger logger = LoggerFactory.getLogger(InputConverterXML.class);

    /**
     * User defined mapping for state to input name.
     */
    private final Map<String, String> inputMap;
    /**
     * Holds all the inputs names that should NOT be transformed by the {@link #convertNameToID(String)} method.
     */
    private final Set<String> inputsWithoutMapping;

    public InputConverterXML(ProtocolService protocolService, String inputMapConfig) {

        logger.trace("User defined mapping: {}", inputMapConfig);
        this.inputMap = createMapFromSetting(inputMapConfig);

        try {
            this.inputsWithoutMapping = createInputsWithoutMapping(protocolService);
            logger.trace("These inputs will not be mapped: {}", inputsWithoutMapping);
        } catch (IOException | ReceivedMessageParseException e) {
            throw new RuntimeException("Could not communicate with the device", e);
        }
    }

    /**
     * Creates a map from a string representation: "KEY1=VALUE1,KEY2=VALUE2"
     *
     * @param setting
     * @return
     */
    private Map<String, String> createMapFromSetting(String setting) {
        Map<String, String> map = new HashMap<>();

        if (!StringUtils.isEmpty(setting)) {
            String[] entries = setting.split(","); // will contain KEY=VALUE entires
            for (String entry : entries) {
                String[] keyValue = entry.split("="); // split the KEY=VALUE string
                if (keyValue.length != 2) {
                    logger.warn("Invalid setting: {} entry: {} - KEY=VALUE format was expected", setting, entry);
                } else {
                    String key = keyValue[0];
                    String value = keyValue[1];

                    if (map.putIfAbsent(key, value) != null) {
                        logger.warn("Invalid setting: {} entry: {} - key: {} was already provided before", setting, entry, key);
                    }
                }
            }
        }
        return map;
    }

    private Set<String> createInputsWithoutMapping(ProtocolService protocolService) throws IOException, ReceivedMessageParseException {
        // Tested on RX-S601D, RX-V479
        Set<String> inputsWithoutMapping = Stream
                .of(INPUT_SPOTIFY, INPUT_BLUETOOTH)
                .collect(Collectors.toSet());

        Set<String> nativeInputNames = protocolService.getInputs(YamahaReceiverBindingConstants.Zone.Main_Zone)
                .stream()
                .filter(x -> x.isWritable())
                .map(x -> x.getParam()).collect(Collectors.toSet());

        // When native input returned matches any of 'HDMIx', 'AUDIOx' or 'NET RADIO', ensure no conversion happens.
        // Tested on RX-S601D, RX-V479
        nativeInputNames.stream()
                .filter(x -> startsWithAndLength(x, "HDMI", 1) || startsWithAndLength(x, "AUDIO", 1) || x.equals(INPUT_NETRADIO))
                .forEach(x -> inputsWithoutMapping.add(x));

        return inputsWithoutMapping;
    }

    private static boolean startsWithAndLength(String str, String prefix, int extraLength) {
        // Should be faster then regex
        return str != null && str.length() == prefix.length() + extraLength && str.startsWith(prefix);
    }

    @Override
    public String toCommandName(String name) {
        // Note: conversation of outgoing command might be needed in the future
        logger.trace("Converting from {} to command name {}", name, name);
        return name;
   }

    @Override
    public String fromStateName(String name) {

        String convertedName;
        String method;

        if (inputMap.containsKey(name)) {
            // Step 1: Check if the user defined custom mapping for their AVR
            convertedName = inputMap.get(name);
            method = "user defined mapping";
        } else if (inputsWithoutMapping.contains(name)) {
            // Step 2: Check if input should not be mapped at all
            convertedName = name;
            method = "no conversion rule";
        } else {
            // Step 3: Fallback to legacy logic
            convertedName = convertNameToID(name);
            method = "legacy mapping";
        }
        logger.trace("Converting from state name {} to {} - as per {}", name, convertedName, method);
        return convertedName;
    }

    /**
     * The xml protocol expects HDMI_1, NET_RADIO as xml nodes, while the actual input IDs are
     * HDMI 1, Net Radio. We offer this conversion method therefore.
     **
     * @param name The inputID like "Net Radio".
     * @return An xml node / xml protocol compatible name like NET_RADIO.
     */
    public String convertNameToID(String name) {
        // Replace whitespace with an underscore. The ID is what is used for xml tags and the AVR doesn't like
        // whitespace in xml tags.
        name = name.replace(" ", "_").toUpperCase();
        // Workaround if the receiver returns "HDMI2" instead of "HDMI_2". We can't really change the input IDs in the
        // thing type description, because we still need to send "HDMI_2" for an input change to the receiver.
        if (name.length() >= 5 && name.startsWith("HDMI") && name.charAt(4) != '_') {
            // Adds the missing underscore.
            name = name.replace("HDMI", "HDMI_");
        }
        return name;
    }
}
