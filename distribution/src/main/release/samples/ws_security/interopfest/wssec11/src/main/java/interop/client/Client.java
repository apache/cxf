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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import interopbaseaddress.interop.IPingService;
import interopbaseaddress.interop.PingService11;

import org.apache.cxf.bus.spring.SpringBusFactory;

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
        boolean local = false;

        if (argv.length > 0 && "local".equalsIgnoreCase(argv[0])) {
            local = true;
        }
        if (argv.length > 0 && ("local".equalsIgnoreCase(argv[0])
            || "ms".equalsIgnoreCase(argv[0]))) {        
            String tmp[] = new String[argv.length - 1];
            System.arraycopy(argv, 1, tmp, 0, tmp.length);
            argv = tmp;
        }

        if (argv.length < 1 || "ALL".equalsIgnoreCase(argv[0])) {
            argv = new String[] {"A",
                                 "A-NoTimestamp",
                                 "AD",
                                 "A-ES",
                                 "AD-ES",
                                 "UX",
                                 "UX-NoTimestamp",
                                 "UXD",
                                 "UX-SEES",
                                 "UXD-SEES", 
                                 "X",
                                 "X-NoTimestamp",
                                 "X-AES128",
                                 "X-AES192",
                                 "X-TripleDES",
                                 "XD",
                                 "XD-ES",
                                 "XD-SEES",
            };
        }
        //argv = new String[] {argv[7]};
        URL wsdlLocation = null;

        new SpringBusFactory().createBus("etc/client.xml");
        List<String> results = new ArrayList<String>();
        for (String portPrefix : argv) {
            try {
                final PingService11 svc;
                if (local) {
                    wsdlLocation = new URL("http://localhost:9001/" + portPrefix + "PingService?wsdl");
                }
                if (wsdlLocation == null) {
                    svc = new PingService11();
                } else {
                    svc = new PingService11(wsdlLocation);
                }
                final IPingService port = 
                    svc.getPort(
                        new QName(
                            "http://InteropBaseAddress/interop",
                            portPrefix + "_IPingService"
                        ),
                        IPingService.class
                    );
                
                
                final String output = port.echo(INPUT);
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
[1] These tests require a Kerberos token, but the MS endpoints don't document where 
    that token is supposed to come from.  The "online" tester for these also does't work.
*/
