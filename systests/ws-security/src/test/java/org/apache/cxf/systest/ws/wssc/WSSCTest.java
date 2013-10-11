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

package org.apache.cxf.systest.ws.wssc;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * SecureConversation tests.
 * 
 * It tests both DOM + StAX clients against the DOM server
 */
public class WSSCTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);

       
    private static final String OUT = "CXF : ping";
    private static wssec.wssc.PingService svc;
    private static Bus bus;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
        
        bus = new SpringBusFactory().createBus("org/apache/cxf/systest/ws/wssc/client.xml");
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        
        svc = new wssec.wssc.PingService();
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        bus.shutdown(true);
        stopAllServers();
    }
    
    
    @Test
    public void testSecureConversationMutualCertificate10SignEncryptIPingService() throws Exception {
        runTest(false, "SecureConversation_MutualCertificate10SignEncrypt_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "SecureConversation_MutualCertificate10SignEncrypt_IPingService");
    }

    @Test
    public void testACIPingService() throws Exception {
        runTest(false, "AC_IPingService");
        runTest(true, "AC_IPingService");
    }

    @Test
    public void testADCIPingService() throws Exception {
        runTest(false, "ADC_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "ADC_IPingService");
    }

    @Test
    public void testADCESIPingService() throws Exception {
        runTest(false, "ADC-ES_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "ADC-ES_IPingService");
    }

    @Test
    public void testAIPingService() throws Exception {
        runTest(false, "_A_IPingService");
        runTest(true, "_A_IPingService");
    }

    @Test
    public void testADIPingService() throws Exception {
        runTest(false, "_AD_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "_AD_IPingService");
    }

    @Test
    public void testADESIPingService() throws Exception {
        runTest(false, "_AD-ES_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "_AD-ES_IPingService");
    }

    @Test
    public void testUXCIPingService() throws Exception {
        runTest(false, "UXC_IPingService");
        runTest(true, "UXC_IPingService");
    }

    @Test
    public void testUXDCIPingService() throws Exception {
        runTest(false, "UXDC_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "UXDC_IPingService");
    }

    @Test
    public void testUXDCSEESIPingService() throws Exception {
        runTest(false, "UXDC-SEES_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "UXDC-SEES_IPingService");
    }

    @Test
    public void testUXIPingService() throws Exception {
        runTest(false, "_UX_IPingService");
        runTest(true, "_UX_IPingService");
    }

    @Test
    public void testUXDIPingService() throws Exception {
        runTest(false, "_UXD_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "_UXD_IPingService");
    }

    @Test
    public void testUXDSEESIPingService() throws Exception {
        runTest(false, "_UXD-SEES_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "_UXD-SEES_IPingService");
    }

    @Test
    public void testXCIPingService() throws Exception {
        runTest(false, "XC_IPingService");
        runTest(true, "XC_IPingService");
    }

    @Test
    public void testXDCIPingService() throws Exception {
        runTest(false, "XDC_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "XDC_IPingService");
    }

    @Test
    public void testXDCIPingService1() throws Exception {
        runTest(false, "XDC_IPingService1");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "XDC_IPingService1");
    }

    @Test
    public void testXDCESIPingService() throws Exception {
        runTest(false, "XDC-ES_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "XDC-ES_IPingService");
    }

    @Test
    public void testXDCSEESIPingService() throws Exception {
        runTest(false, "XDC-SEES_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "XDC-SEES_IPingService");
    }

    @Test
    public void testXIPingService() throws Exception {
        runTest(false, "_X_IPingService");
        runTest(true, "_X_IPingService");
    }

    @Test
    public void testX10IPingService() throws Exception {
        runTest(false, "_X10_IPingService");
        runTest(true, "_X10_IPingService");
    }

    @Test
    public void testXDIPingService() throws Exception {
        runTest(false, "_XD_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "_XD_IPingService");
    }

    @Test
    public void testXDSEESIPingService() throws Exception {
        runTest(false, "_XD-SEES_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "_XD-SEES_IPingService");
    }

    @Test
    public void testXDESIPingService() throws Exception {
        runTest(false, "_XD-ES_IPingService");
        // TODO Hanging due to Derived Keys...
        // runTest(true, "_XD-ES_IPingService");
    }


    private void runTest(boolean streaming, String ... argv) throws Exception {
        for (String portPrefix : argv) {
            final wssec.wssc.IPingService port = 
                svc.getPort(
                    new QName(
                        "http://WSSec/wssc",
                        portPrefix
                    ),
                    wssec.wssc.IPingService.class
                );
           
            ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                            "http://localhost:" + PORT + "/" + portPrefix);
            if (portPrefix.charAt(0) == '_') {
                //MS would like the _ versions to send a cancel
                ((BindingProvider)port).getRequestContext()
                    .put(SecurityConstants.STS_TOKEN_DO_CANCEL, Boolean.TRUE);
            }
            
            if (streaming) {
                ((BindingProvider)port).getRequestContext().put(
                    SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
                );
                ((BindingProvider)port).getResponseContext().put(
                    SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
                );
            }
            
            wssec.wssc.PingRequest params = new wssec.wssc.PingRequest();
            org.xmlsoap.ping.Ping ping = new org.xmlsoap.ping.Ping();
            ping.setOrigin("CXF");
            ping.setScenario("Scenario5");
            ping.setText("ping");
            params.setPing(ping);
            try {
                wssec.wssc.PingResponse output = port.ping(params);
                assertEquals(OUT, output.getPingResponse().getText());
            } catch (Exception ex) {
                throw new Exception("Error doing " + portPrefix, ex);
            }
            ((java.io.Closeable)port).close();
        }
    }

    
}
