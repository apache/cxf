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

package org.apache.cxf.systest.ws.wssec10;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.wssec10.server.Server;
import org.apache.cxf.systest.ws.wssec11.WSSecurity11Common;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

import wssec.wssec10.IPingService;
import wssec.wssec10.PingService;


/**
 *
 */
public class WSSecurity10Test extends AbstractBusClientServerTestBase {
    
    private static final String INPUT = "foo";
    private static boolean unrestrictedPoliciesInstalled;
    
    static {
        unrestrictedPoliciesInstalled = WSSecurity11Common.checkUnrestrictedPoliciesInstalled();
    };    

    @BeforeClass
    public static void startServers() throws Exception {

        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
    }

    @Test
    public void testClientServer() {

        String[] argv = new String[] {
            "UserNameOverTransport",
            "MutualCertificate10SignEncrypt",
            "MutualCertificate10SignEncryptRsa15TripleDes"
        };
        //argv = new String[] {argv[1]};
        Bus bus = null;
        if (unrestrictedPoliciesInstalled) {
            bus = new SpringBusFactory().createBus("org/apache/cxf/systest/ws/wssec10/client/client.xml");
        } else {
            bus = new SpringBusFactory().createBus(
                    "org/apache/cxf/systest/ws/wssec10/client/client_restricted.xml");
        }
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        List<String> results = new ArrayList<String>();
        URL wsdlLocation = null;
        for (String portPrefix : argv) {
            try {
                PingService svc = null; 
                wsdlLocation = getWsdlLocation(portPrefix); 
                svc = new PingService(wsdlLocation);
                final IPingService port = 
                    svc.getPort(
                        new QName(
                            "http://WSSec/wssec10",
                            portPrefix + "_IPingService"
                        ),
                        IPingService.class
                    );
                
                final String output = port.echo(INPUT);
                assertTrue("Input, " + INPUT + " not equal to Output " + output, 
                        INPUT.equals(output));
                if (!INPUT.equals(output)) {
                    System.err.println(
                        "Expected " + INPUT + " but got " + output
                    );
                    results.add("Expected " + INPUT + " but got " + output);
                } else {
                    System.out.println("OK!");
                    results.add("OK");
                }
            } catch (Throwable t) {
                results.add("Exception: " + t);
                t.printStackTrace();
                assertTrue("Caught excetion: " + t, 
                        false);
            }
        }
        for (int x = 0; x < argv.length; x++) {
            System.out.println(argv[x] + ": " + results.get(x));
        }
    }
    
    private static URL getWsdlLocation(String portPrefix) {
        try {
            if ("UserNameOverTransport".equals(portPrefix)) {
                return new URL("https://localhost:9001/" + portPrefix + "?wsdl");
            } else if ("MutualCertificate10SignEncrypt".equals(portPrefix)) {
                return new URL("http://localhost:9002/" + portPrefix + "?wsdl");
            } else if ("MutualCertificate10SignEncryptRsa15TripleDes".equals(portPrefix)) {
                return new URL("http://localhost:9000/" + portPrefix + "?wsdl");
            }
        } catch (MalformedURLException mue) {
            return null;
        }
        return null;
    }

    
}
