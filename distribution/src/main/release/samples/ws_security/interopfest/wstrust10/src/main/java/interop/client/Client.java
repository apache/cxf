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
import javax.xml.ws.Service;

import org.apache.cxf.bus.spring.SpringBusFactory;
import org.tempuri.AsymmetricFederatedService;
import org.tempuri.SymmetricFederatedService;

import interopbaseaddress.interop.IPingService;



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
        
        if (argv.length < 1 || "ALL".equals(argv[0])) {
            argv = new String[] {
                "Scenario_1_IssuedTokenOverTransport_UsernameOverTransport",
                "Scenario_2_IssuedToken_MutualCertificate10",
                "Scenario_5_IssuedTokenForCertificate_MutualCertificate11",
                "Scenario_6_IssuedTokenForCertificateSecureConversation_MutualCertificate11",       
                                 
                "Scenario_7_IssuedTokenOverTransport_UsernameOverTransport",
                "Scenario_9_IssuedTokenForCertificate_MutualCertificate11",
                "Scenario_10_IssuedTokenForCertificateSecureConversation_MutualCertificate11",
            };
        }
        //argv = new String[] {argv[0]};

        new SpringBusFactory().createBus("etc/client.xml");
        List<String> results = new ArrayList<String>(argv.length);
        
        for (String portPrefix : argv) {
            try {
                Service svc;
                if (portPrefix.charAt(10) == '_'
                    && portPrefix.charAt(9) < '7') {
                    svc = new SymmetricFederatedService();
                } else {
                    svc = new AsymmetricFederatedService();
                }
                
                final IPingService port = 
                    svc.getPort(
                        new QName(
                            "http://tempuri.org/",
                            portPrefix
                        ),
                        IPingService.class
                    );
               
                final String output = port.echo(INPUT);
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
