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

package org.apache.cxf.ws.rm;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;

/**
 * Holder for utility methods relating to contexts.
 */

public final class RMContextUtils {

    /**
     * Prevents instantiation.
     */
    protected RMContextUtils() {
    }

    /**
     * @return a generated UUID
     */
    public static String generateUUID() {
        return org.apache.cxf.ws.addressing.ContextUtils.generateUUID();
    }

    /**
     * Determine if message is currently being processed on server side.
     * 
     * @param message the current Message
     * @return true if message is currently being processed on server side
     */
    public static boolean isServerSide(Message message) {
        return message.getExchange().getDestination() != null;
    }
    
    /**
     * Checks if the action String belongs to an RM protocol message.
     * 
     * @param action the action
     * @return true if the action is not one of the RM protocol actions.
     */
    public static boolean isRMProtocolMessage(String action) {
        return RM10Constants.ACTIONS.contains(action) || RM11Constants.ACTIONS.contains(action);
    }

    /**
     * Retrieve the RM properties from the current message.
     * 
     * @param message the current message
     * @param outbound true if the message direction is outbound
     * @return the RM properties
     */
    public static RMProperties retrieveRMProperties(Message message, boolean outbound) {
        if (outbound) {
            return (RMProperties)message.get(getRMPropertiesKey(true));
        } else {
            Message m = null;
            if (MessageUtils.isOutbound(message)) {
                // the in properties are only available on the in message
                m = message.getExchange().getInMessage();
                if (null == m) {
                    m = message.getExchange().getInFaultMessage();
                }
            } else {
                m = message;
            }
            if (null != m) {
                return (RMProperties)m.get(getRMPropertiesKey(false));
            }
        }
        return null;

    }

    /**
     * Store the RM properties in the current message.
     * 
     * @param message the current message
     * @param rmps the RM properties
     * @param outbound if the message direction is outbound
     */
    public static void storeRMProperties(Message message, RMProperties rmps, boolean outbound) {
        String key = getRMPropertiesKey(outbound);
        message.put(key, rmps);
    }

    /**
     * Retrieves the addressing properties from the current message.
     * 
     * @param message the current message
     * @param isProviderContext true if the binding provider request context
     *            available to the client application as opposed to the message
     *            context visible to handlers
     * @param isOutbound true if the message is outbound
     * @return the current addressing properties
     */
    public static AddressingPropertiesImpl retrieveMAPs(Message message, boolean isProviderContext,
                                                        boolean isOutbound) {
        return org.apache.cxf.ws.addressing.ContextUtils.retrieveMAPs(message, isProviderContext, isOutbound);
    }

    /**
     * Store MAPs in the message.
     * 
     * @param maps the MAPs to store
     * @param message the current message
     * @param isOutbound true if the message is outbound
     * @param isRequestor true if the current messaging role is that of
     *            requestor
     * @param handler true if HANDLER scope, APPLICATION scope otherwise
     */
    public static void storeMAPs(AddressingProperties maps, Message message, boolean isOutbound,
                                 boolean isRequestor) {
        org.apache.cxf.ws.addressing.ContextUtils.storeMAPs(maps, message, isOutbound, isRequestor);
    }

    public static String getRMPropertiesKey(boolean outbound) {
        return outbound
            ? RMMessageConstants.RM_PROPERTIES_OUTBOUND : RMMessageConstants.RM_PROPERTIES_INBOUND;
    }
}
