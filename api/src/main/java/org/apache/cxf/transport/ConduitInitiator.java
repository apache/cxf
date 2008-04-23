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

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * Factory for Conduits.
 */
public interface ConduitInitiator {
    /**
     * Initiate an outbound Conduit.
     * 
     * @param targetInfo the endpoint info of the target 
     * @return a suitable new or pre-existing Conduit
     */
    Conduit getConduit(EndpointInfo targetInfo) throws IOException;

    /**
     * Initiate an outbound Conduit.
     * 
     * @param localInfo the endpoint info for a local endpoint on which the
     * the configuration should be based
     * @param target the target EPR
     * @return a suitable new or pre-existing Conduit
     */
    Conduit getConduit(EndpointInfo localInfo,
                       EndpointReferenceType target) throws IOException;
    
    Set<String> getUriPrefixes();
    List<String> getTransportIds();
}
