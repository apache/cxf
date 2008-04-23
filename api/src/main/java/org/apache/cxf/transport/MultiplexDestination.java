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

package org.apache.cxf.transport;

import java.util.Map;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * A MultiplexDestination is a transport-level endpoint capable of receiving
 * unsolicited incoming messages from different peers for multiple targets 
 * identified by a unique id.
 * The disambiguation of targets is handled by higher layers as the target
 * address is made available as a context property or as a WS-A-To header
 */

public interface MultiplexDestination extends Destination {
    
    /**
     * @return the a reference containing the id that is 
     * associated with this Destination
     */
    EndpointReferenceType getAddressWithId(String id);
    
    /**
     * @param contextMap for this invocation. Obtained for example from 
     * JAX-WS WebServiceContext.getMessageContext(). The context will
     * either contain the WS-A To content and/or some property that 
     * identifies the target address, eg MessageContext.PATH_INFO for
     * the current invocation
     * @return the id associated with the current invocation
     */
    String getId(Map contextMap);
}
