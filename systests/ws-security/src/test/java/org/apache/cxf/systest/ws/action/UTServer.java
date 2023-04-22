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

package org.apache.cxf.systest.ws.action;

import java.util.HashMap;
import java.util.Map;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.systest.ws.common.DoubleItPortTypeImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;

/**
 * Test adding the WSS4JInInterceptor in code.
 */
public class UTServer extends AbstractBusTestServerBase {

    public static final String PORT = allocatePort(UTServer.class);

    protected void run() {
        DoubleItPortTypeImpl implementor = new DoubleItPortTypeImpl();
        implementor.setEnforcePrincipal(false);
        String address = "http://localhost:" + PORT + "/DoubleItUsernameToken3";
        EndpointImpl jaxWsEndpoint = (EndpointImpl)Endpoint.publish(address, implementor);

        Map<String, Object> properties = new HashMap<>();
        properties.put("action", "UsernameToken");
        properties.put("passwordCallbackClass", "org.apache.cxf.systest.ws.common.UTPasswordCallback");
        WSS4JInInterceptor wss4jInInterceptor = new WSS4JInInterceptor(properties);

        jaxWsEndpoint.getServer().getEndpoint().getInInterceptors().add(wss4jInInterceptor);
    }
}
