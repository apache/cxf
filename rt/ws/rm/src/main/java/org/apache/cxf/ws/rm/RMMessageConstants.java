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

/**
 * A container for WS-RM message constants.
 */
public final class RMMessageConstants {
    
    /**
     * Used to cache outbound RM properties in message.
     */
    public static final String RM_PROPERTIES_OUTBOUND = 
        "org.apache.cxf.ws.rm.outbound";
    
    /**
     * Used to cache inbound RM properties in message.
     */
    public static final String RM_PROPERTIES_INBOUND = 
        "org.apache.cxf.ws.rm.inbound";
    
    public static final String ORIGINAL_REQUESTOR_ROLE =
        "org.apache.cxf.client.original";
    
    public static final String SAVED_CONTENT =
        "org.apache.cxf.ws.rm.content";
    
    /**
     * Prevents instantiation. 
     */
    private RMMessageConstants() {
    }
}
