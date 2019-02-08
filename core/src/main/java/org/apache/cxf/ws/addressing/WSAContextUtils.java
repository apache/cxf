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


/**
 * Holder for utility methods relating to contexts.
 */
public final class WSAContextUtils {
    public static final String REPLYTO_PROPERTY =
        "org.apache.cxf.ws.addressing.replyto";
    public static final String DECOUPLED_ENDPOINT_BASE_PROPERTY =
        "org.apache.cxf.ws.addressing.decoupled.endpoint.base";

    private static final String USING_PROPERTY =
        "org.apache.cxf.ws.addressing.using";

    /**
     * Prevents instantiation.
     */
    private WSAContextUtils() {
    }

    /**
     * Retrieve UsingAddressing override flag from the context
     *
     * @param message the current message
     * @return true if UsingAddressing should be overridden
     */
    public static boolean retrieveUsingAddressing(Message message) {
        Boolean override = (Boolean)message.get(USING_PROPERTY);
        return override == null || override.booleanValue();
    }



}
