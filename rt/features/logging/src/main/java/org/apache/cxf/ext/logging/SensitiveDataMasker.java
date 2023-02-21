package org.apache.cxf.ext.logging;

import org.apache.cxf.message.Message;

public interface SensitiveDataMasker {

    /**
     * Hides sensitive data in message.
     * It's up to concrete implementation to decide how they are hidden. Just some possible examples:
     * <ul>
     *     <li>replace with some string, e.g. ***hidden***</li>
     *     <li>remove value</li>
     *     <li>remove whole element or attribute</li>
     * </ul>
     *
     * @param message
     * @param originalLogString
     * @return log entry with sensitive data hidden.
     */
    default String maskSensitiveElements(final Message message, String originalLogString) {
        return originalLogString;
    }
}
