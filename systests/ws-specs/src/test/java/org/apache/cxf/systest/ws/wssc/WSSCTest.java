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
import org.apache.cxf.systest.ws.wssc.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class WSSCTest extends AbstractBusClientServerTestBase {

       
    private static final String OUT = "CXF : ping";
    private static wssec.wssc.PingService svc;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
        
        final Bus bus = 
            new SpringBusFactory().createBus("org/apache/cxf/systest/ws/wssc/client/client.xml");
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        
        svc = new wssec.wssc.PingService();
    }
    
    
    @Test
    public void testSecureConversationMutualCertificate10SignEncryptIPingService() throws Exception {
        runTest("SecureConversation_MutualCertificate10SignEncrypt_IPingService");
    }

    @Test
    public void testACIPingService() throws Exception {
        runTest("AC_IPingService");
    }

    @Test
    public void testADCIPingService() throws Exception {
        runTest("ADC_IPingService");
    }

    @Test
    public void testADCESIPingService() throws Exception {
        runTest("ADC-ES_IPingService");
    }

    @Test
    public void testAIPingService() throws Exception {
        runTest("_A_IPingService");
    }

    @Test
    public void testADIPingService() throws Exception {
        runTest("_AD_IPingService");
    }

    @Test
    public void testADESIPingService() throws Exception {
        runTest("_AD-ES_IPingService");
    }

    @Test
    public void testUXCIPingService() throws Exception {
        runTest("UXC_IPingService");
    }

    @Test
    public void testUXDCIPingService() throws Exception {
        runTest("UXDC_IPingService");
    }

    @Test
    public void testUXDCSEESIPingService() throws Exception {
        runTest("UXDC-SEES_IPingService");
    }

    @Test
    public void testUXIPingService() throws Exception {
        runTest("_UX_IPingService");
    }

    @Test
    public void testUXDIPingService() throws Exception {
        runTest("_UXD_IPingService");
    }

    @Test
    public void testUXDSEESIPingService() throws Exception {
        runTest("_UXD-SEES_IPingService");
    }

    @Test
    public void testXCIPingService() throws Exception {
        runTest("XC_IPingService");
    }

    @Test
    public void testXDCIPingService() throws Exception {
        runTest("XDC_IPingService");
    }

    @Test
    public void testXDCIPingService1() throws Exception {
        runTest("XDC_IPingService1");
    }

    @Test
    public void testXDCESIPingService() throws Exception {
        runTest("XDC-ES_IPingService");
    }

    @Test
    public void testXDCSEESIPingService() throws Exception {
        runTest("XDC-SEES_IPingService");
    }

    @Test
    public void testXIPingService() throws Exception {
        runTest("_X_IPingService");
    }

    @Test
    public void testX10IPingService() throws Exception {
        runTest("_X10_IPingService");
    }

    @Test
    public void testXDIPingService() throws Exception {
        runTest("_XD_IPingService");
    }

    @Test
    public void testXDSEESIPingService() throws Exception {
        runTest("_XD-SEES_IPingService");
    }

    @Test
    public void testXDESIPingService() throws Exception {
        runTest("_XD-ES_IPingService");
    }




    private void runTest(String ... argv) throws Exception {
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
                                                            "http://localhost:9001/" + portPrefix);
            if (portPrefix.charAt(0) == '_') {
                //MS would like the _ versions to send a cancel
                ((BindingProvider)port).getRequestContext()
                    .put(SecurityConstants.STS_TOKEN_DO_CANCEL, Boolean.TRUE);
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
        }
    }

    
}
