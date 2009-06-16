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
import javax.xml.ws.BindingProvider;

import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ws.security.SecurityConstants;
import org.xmlsoap.ping.Ping;

import interopbaseaddress.interop.IPingService;
import interopbaseaddress.interop.PingRequest;
import interopbaseaddress.interop.PingResponse;
import interopbaseaddress.interop.PingService;


public final class Client {
    
    private static final String OUT = "CXF : ping";
    
    /**
     * This class is not instantiated
     */
    private Client() {
    }

    /**
     * The main entrypoint into the application
     */
    public static void main(String argv[])
        throws Exception {

        boolean useLocalWCFServices = false;
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

        if (argv.length < 1 || "".equals(argv[0]) 
            || argv[0] == null || "ALL".equals(argv[0])) {
            argv = new String[] {
                //don't have the certs available to be able to connect 
                //"SecureConversation_UserNameOverTransport_IPingService",
                
                "SecureConversation_MutualCertificate10SignEncrypt_IPingService",
                "AC_IPingService",
                "ADC_IPingService",
                "ADC-ES_IPingService",   
                "_A_IPingService",
                "_AD_IPingService",
                "_AD-ES_IPingService",
                
                "UXC_IPingService", 
                "UXDC_IPingService",
                "UXDC-SEES_IPingService",
                "_UX_IPingService",
                "_UXD_IPingService",
                "_UXD-SEES_IPingService",
                
                
                "XC_IPingService", 
                "XDC_IPingService", 
                "XDC_IPingService1", 
                "XDC-ES_IPingService", 
                "XDC-SEES_IPingService", 
                "_X_IPingService", 
                "_X10_IPingService", 
                "_XD_IPingService", 
                "_XD-SEES_IPingService", 
                "_XD-ES_IPingService",
                
                //Kerberos token - not sure where the token comes from or how these works
                //"KC_IPingService", 
                //"KDC_IPingService",
                //"KC10_IPingService",
                //"KDC10_IPingService",
                //"_K10_IPingService",
                //"_KD10_IPingService",
                //"_K_IPingService",
                //"_KD_IPingService",
                //"_KD-SEES_IPingService",
            };
        }
        //argv = new String[] {argv[0]};
        //argv = new String[] {"_X10_IPingService"};
        
        
        new SpringBusFactory().createBus("etc/client.xml");
        List<String> results = new ArrayList<String>(argv.length);
        URL wsdlLocation = null;
        
        for (String portPrefix : argv) {
            try {
                PingService svc;
                if (local) {
                    wsdlLocation = new URL("http://localhost:9001/" + portPrefix + "?wsdl");
                }
                boolean isLocal = false;
                try {
                    if (wsdlLocation != null) {
                        wsdlLocation.getContent();
                        isLocal = true;     
                    }
                } catch (Exception e) {
                    isLocal = false;
                }
                
                if (isLocal) {
                    svc = new PingService(wsdlLocation);
                } else {
                    svc = new PingService();
                }
                final IPingService port = 
                    svc.getPort(
                        new QName(
                            "http://InteropBaseAddress/interop",
                            portPrefix
                        ),
                        IPingService.class
                    );
               
                if (useLocalWCFServices) {
                    ((BindingProvider)port).getRequestContext()
                        .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, getEndpointName(portPrefix));
                }
                
                if (portPrefix.charAt(0) == '_') {
                    //MS would like the _ versions to send a cancel
                    ((BindingProvider)port).getRequestContext()
                        .put(SecurityConstants.STS_TOKEN_DO_CANCEL, Boolean.TRUE);
                }
                PingRequest params = new PingRequest();
                Ping ping = new Ping();
                ping.setOrigin("CXF");
                ping.setScenario("Scenario5");
                ping.setText("ping");
                params.setPing(ping);
                PingResponse output = port.ping(params);
                if (!OUT.equals(output.getPingResponse().getText())) {
                    System.err.println(
                        "Expected " + OUT + " but got " + output.getPingResponse().getText()
                    );
                    results.add("Unexpected output " + output.getPingResponse().getText());
                } else {
                    System.out.println(portPrefix + ": OK!");
                    results.add("OK!");
                }
            } catch (Throwable t) {
                t.printStackTrace();
                results.add("Exception: " + t);
            }
            //blasting the MS endpoints tends to cause a hang
            //pause a sec to allow it to recover
            Thread.sleep(1000);
            System.gc();
        }
        for (int x = 0; x < argv.length; x++) {
            System.out.println(argv[x] + ": " + results.get(x));
        }
    }


    private static String getEndpointName(String testName) {
        if ("SecureConversation_UserNameOverTransport_IPingService".equals(testName)) {
            return "https://localhost/Security_WsSecurity_Service_Indigo/WSSecureConversation.svc"
                + "/SecureConversation_UserNameOverTransport";
        }
        return "http://localhost/Security_WsSecurity_Service_Indigo/WSSecureConversation.svc/" 
            + testName.substring(0, testName.indexOf("_IPingService"));
    }    
}

