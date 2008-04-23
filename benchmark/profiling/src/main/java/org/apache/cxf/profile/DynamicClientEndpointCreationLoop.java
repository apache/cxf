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

package org.apache.cxf.profile;

import java.net.URISyntaxException;
import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.dynamic.DynamicClientFactory;

/**
 * 
 */
public final class DynamicClientEndpointCreationLoop {
    
    private Bus bus;
    
    private DynamicClientEndpointCreationLoop() {
        CXFBusFactory busFactory = new CXFBusFactory(); 
        bus = busFactory.createBus();
    }
    
    private void iteration() throws URISyntaxException {    
        URL wsdl = getClass().getResource("/wsdl/others/dynamic_client_base64.wsdl");
        String wsdlUrl = null;
        wsdlUrl = wsdl.toURI().toString();
        DynamicClientFactory dynamicClientFactory = DynamicClientFactory.newInstance(bus);
        Client client = dynamicClientFactory.createClient(wsdlUrl);
        client.destroy();
    }
    /**
     * @param args
     * @throws URISyntaxException 
     */
    public static void main(String[] args) throws URISyntaxException {
        DynamicClientEndpointCreationLoop ecl = new DynamicClientEndpointCreationLoop();
        int count = Integer.parseInt(args[0]);
        for (int x = 0; x < count; x++) {
            ecl.iteration();
        }
    }
}
