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

package org.apache.cxf.ws.discovery;

import jakarta.xml.ws.EndpointReference;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ws.discovery.wsdl.HelloType;

public interface WSDiscoveryService {

    /** Registers the given EndpointReference with the service.  Also sends the UDP "Hello"
     * @param ref
     * @return the HelloType that was sent so it can be used to call unregister
     */
    HelloType register(EndpointReference ref);

    void register(HelloType ht);
    void unregister(HelloType ht);

    /**
     * Utility methods to aid in calling the register/unregister for the appropriate CXF server
     * @param server
     */
    void serverStarted(Server server);
    void serverStopped(Server server);

    /**
     * The service requires a WSDiscoveryClient in order to interact with a DiscoveryProxy, send Hello/Bye/etc..
     * @return the client being used
     */
    WSDiscoveryClient getClient();

}
