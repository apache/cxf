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

package org.apache.cxf.samples.discovery;

import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.EndpointReference;

import org.apache.cxf.hello_world.discovery.Greeter;
import org.apache.cxf.hello_world.discovery.GreeterService;
import org.apache.cxf.ws.discovery.WSDiscoveryClient;

public final class Client {

    private Client() {
    }

    public static void main(String[] args) throws Exception {

        //Use WS-Discovery to find references to services that implement the Greeter portType
        WSDiscoveryClient client = new WSDiscoveryClient();
        List<EndpointReference> references 
            = client.probe(new QName("http://cxf.apache.org/hello_world/discovery", "Greeter"));
        client.close();
        
        GreeterService service = new GreeterService();
        //loop through all of them and have them greet me.
        for (EndpointReference ref : references) {
            Greeter g = service.getPort(ref, Greeter.class);
            System.out.println(g.greetMe("World"));
        }       
    }

}
