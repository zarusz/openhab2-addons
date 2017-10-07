package org.openhab.binding.yamahareceiver.internal.protocol;

import org.openhab.binding.yamahareceiver.YamahaReceiverBindingConstants;

import java.io.IOException;
import java.util.Collection;

/**
 * Service that exposes basic protocol operations
 *
 * @author Tomasz Maruszak
 */
public interface ProtocolService {

    /**
     * Retrieves all available inputs
     * @param zone
     * @return
     * @throws IOException
     * @throws ReceivedMessageParseException
     */
    Collection<InputDto> getInputs(YamahaReceiverBindingConstants.Zone zone)
            throws IOException, ReceivedMessageParseException;

    class InputDto {

        private final String param;
        private final boolean writable;

        public InputDto(String param, boolean writable) {
            this.param = param;
            this.writable = writable;
        }

        public String getParam() {
            return param;
        }

        public boolean isWritable() {
            return writable;
        }

        @Override
        public String toString() {
            return "InputDto{" +
                    "param='" + param + '\'' +
                    ", writable=" + writable +
                    '}';
        }
    }
}
