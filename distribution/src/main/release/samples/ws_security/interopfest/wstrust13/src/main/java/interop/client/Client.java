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

package interop.client;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.bus.spring.SpringBusFactory;
import org.tempuri.IPingServiceContract;
import org.tempuri.PingService;

public final class Client {
    
    private static final String INPUT = "foo";
    
    /**
     * This class is not instantiated
     */
    private Client() {
    }

    /**

    /**
     * The main entrypoint into the application
     */
    public static void main(String argv[])
        throws Exception {
        
        if (argv.length < 1 || "ALL".equalsIgnoreCase(argv[0])) {
            argv = new String[] {
                "CustomBinding_IPingServiceContract",
                "OasisScenario2Binding_IPingServiceContract",  
                "CustomBinding_IPingServiceContract1",
                "OasisScenario4Binding_IPingServiceContract",
                "CustomBinding_IPingServiceContract2", 
//                "CustomBinding_IPingServiceContract3", //NOT WORKING - [1]
                "CustomBinding_IPingServiceContract4", 
//                "CustomBinding_IPingServiceContract6", //NOT WORKING - [1]
//                "CustomBinding_IPingServiceContract5", //NOT WORKING - [2]
//                "CustomBinding_IPingServiceContract7", //NOT WORKING - service not running on given port
//                "CustomBinding_IPingServiceContract8", //Hanging?
//                "CustomBinding_IPingServiceContract9", //NOT WORKING - [3]
                "CustomBinding_IPingServiceContract10",
            };
        }
        //argv = new String[] {argv[3]};
        //argv = new String[] {"CustomBinding_IPingServiceContract8"};
            
            
        new SpringBusFactory().createBus("etc/client.xml");
        List<String> results = new ArrayList<String>(argv.length);
        
        for (String portPrefix : argv) {
            try {
                final PingService svc = new PingService();
                final IPingServiceContract port = 
                    svc.getPort(
                        new QName(
                            "http://tempuri.org/",
                            portPrefix
                        ),
                        IPingServiceContract.class
                    );
               
                final String output = port.ping(INPUT);
                if (!INPUT.equals(output)) {
                    System.err.println(
                        "Expected " + INPUT + " but got " + output
                    );
                    results.add("Unexpected output " + output);
                } else {
                    System.out.println("OK!");
                    results.add("OK!");
                }
            } catch (Throwable t) {
                t.printStackTrace();
                results.add("Exception: " + t);
            }
        }
        for (int x = 0; x < argv.length; x++) {
            System.out.println(argv[x] + ": " + results.get(x));
        }
    }

}
/*
[1] The policy in PingService.wsdl seems to be wrong.   The sp:RequestSecurityTokenTemplate for 
    it states:
     <trust:KeyType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey</trust:KeyType>
    but the "sample" produced from their online tool sends SymetricKey

[2] OasisScenario9/10 (CustomBinding_IPingServiceContract5) isn't working yet due to WSS4J 
    not supporting using RSAKeyValue (KeyInfo, WS-SecurityPolicy/KeyValueToken) things for 
    creating signatures in version 1.5.5 which was the current version when writing this
    sample.  1.5.6 does support RSAKeyValue, but this sample hasn't been updated yet.
 
[3] The trust token is being retrieved sucessfully.  It's not able to negotiate the https 
    connection with the final endpoint.  Not sure which exact keys to use yet. 
 */
