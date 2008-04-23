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

package org.apache.cxf.ws.addressing;


import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Conduit;


/**
 * Holder for utility methods relating to contexts.
 */
public final class WSAContextUtils {

    private static final String TO_PROPERTY =
        "org.apache.cxf.ws.addressing.to";
    private static final String REPLYTO_PROPERTY =
        "org.apache.cxf.ws.addressing.replyto";
    private static final String USING_PROPERTY =
        "org.apache.cxf.ws.addressing.using";    

    /**
     * Prevents instantiation.
     */
    private WSAContextUtils() {
    }

    /**
     * Store UsingAddressing override flag in the context
     *
     * @param override true if UsingAddressing should be overridden
     * @param message the current message
     */   
    public static void storeUsingAddressing(boolean override, Message message) {
        message.put(USING_PROPERTY, Boolean.valueOf(override));
    }
    
    /**
     * Retrieve UsingAddressing override flag from the context
     *
     * @param message the current message
     * @return true if UsingAddressing should be overridden
     */   
    public static boolean retrieveUsingAddressing(Message message) {
        Boolean override = (Boolean)message.get(USING_PROPERTY);
        return override == null || (override != null && override.booleanValue());
    }

    /**
     * Store To EPR in the context
     *
     * @param to the To EPR
     * @param message the current message
     */   
    public static void storeTo(EndpointReferenceType to,
                               Message message) {
        message.put(TO_PROPERTY, to);
    }
    
    /**
     * Retrieve To EPR from the context.
     *
     * @param conduit the Conduit if available
     * @param message the current message
     * @return the retrieved EPR
     */
    public static EndpointReferenceType retrieveTo(Conduit conduit,
                                                   Message message) {
        EndpointReferenceType to = null;
        if (conduit != null) {
            to = conduit.getTarget();
        } else {
            to = (EndpointReferenceType)message.get(TO_PROPERTY);
        }
        return to;
    }
    
    /**
     * Store ReplyTo EPR in the context
     *
     * @param replyTo the ReplyTo EPR
     * @param message the current message
     */   
    public static void storeReplyTo(EndpointReferenceType replyTo,
                                    Message message) {
        message.put(REPLYTO_PROPERTY, replyTo);
    }

    /**
     * Retrieve ReplyTo EPR from the context.
     *
     * @param conduit the Conduit if available
     * @param message the current message
     * @return the retrieved EPR
     */
    public static EndpointReferenceType retrieveReplyTo(Conduit conduit,
                                                        Message message) {
        return conduit != null
               ? conduit.getBackChannel().getAddress()
               : (EndpointReferenceType)message.get(REPLYTO_PROPERTY); 
    }
}
