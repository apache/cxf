/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.interceptor;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

/**
 * Helpers shared by the binding-specific fault out interceptors.
 */
public final class FaultUtils {

    /**
     * Message property that, when set, is used verbatim as the fault string.
     */
    public static final String FORCED_FAULT_STRING = "forced.faultstring";

    static final String DEFAULT_FAULT_STRING = "Fault occurred while processing.";

    private FaultUtils() {
    }

    /**
     * Returns the human-readable fault string to send to the peer for the given fault.
     * <p>
     * By default only the fault's own message is returned. Details of the underlying
     * cause are only included when {@link Message#EXCEPTION_MESSAGE_CAUSE_ENABLED}
     * is enabled on the message, so that internal exception details are not disclosed
     * to remote callers unless explicitly configured.
     */
    public static String getFaultString(Message message, Fault fault) {
        Object forced = message.get(FORCED_FAULT_STRING);
        if (forced != null) {
            return (String) forced;
        }
        boolean config = MessageUtils.getContextualBoolean(message, Message.EXCEPTION_MESSAGE_CAUSE_ENABLED, false);
        Throwable cause = fault.getCause();
        if (fault.getMessage() != null) {
            if (config && cause != null
                && cause.getMessage() != null && !fault.getMessage().equals(cause.getMessage())) {
                return fault.getMessage() + " Caused by: " + cause.getMessage();
            }
            return fault.getMessage();
        } else if (config && cause != null) {
            if (cause.getMessage() != null) {
                return cause.getMessage();
            }
            return cause.toString();
        } else {
            return DEFAULT_FAULT_STRING;
        }
    }
}
