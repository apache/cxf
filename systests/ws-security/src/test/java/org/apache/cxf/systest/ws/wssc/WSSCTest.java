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

import java.util.Arrays;
import java.util.Collection;

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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * SecureConversation tests.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class WSSCTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String STAX_PORT = allocatePort(StaxServer.class);

    private static final String OUT = "CXF : ping";
    private static wssec.wssc.PingService svc;
    private static Bus bus;
    
    final TestParam test;
    
    public WSSCTest(TestParam type) {
        this.test = type;
    }
    
    static class TestParam {
        final String prefix;
        final boolean streaming;
        final String port;
        
        public TestParam(String p, String port, boolean b) {
            prefix = p;
            this.port = port;
            streaming = b;
        }
        public String toString() {
            return prefix + ":" + port + ":" + (streaming ? "streaming" : "dom");
        }
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxServer.class, true)
        );
        
        bus = new SpringBusFactory().createBus("org/apache/cxf/systest/ws/wssc/client.xml");
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        
        svc = new wssec.wssc.PingService();
    }
    
    @Parameters(name = "{0}")
    public static Collection<TestParam[]> data() {
        return Arrays.asList(new TestParam[][] {
            {new TestParam("SecureConversation_MutualCertificate10SignEncrypt_IPingService", PORT, false)},
            {new TestParam("AC_IPingService", PORT, false)},
            {new TestParam("ADC_IPingService", PORT, false)},
            {new TestParam("ADC-ES_IPingService", PORT, false)},
            {new TestParam("_A_IPingService", PORT, false)},
            {new TestParam("_AD_IPingService", PORT, false)},
            {new TestParam("_AD-ES_IPingService", PORT, false)},
            {new TestParam("UXC_IPingService", PORT, false)},
            {new TestParam("UXDC_IPingService", PORT, false)},
            {new TestParam("UXDC-SEES_IPingService", PORT, false)},
            {new TestParam("_UX_IPingService", PORT, false)},
            {new TestParam("_UXD_IPingService", PORT, false)},
            {new TestParam("_UXD-SEES_IPingService", PORT, false)},
            {new TestParam("XC_IPingService", PORT, false)},
            {new TestParam("XDC_IPingService", PORT, false)},
            {new TestParam("XDC_IPingService1", PORT, false)},
            {new TestParam("XDC-ES_IPingService", PORT, false)},
            {new TestParam("XDC-SEES_IPingService", PORT, false)},
            {new TestParam("_X_IPingService", PORT, false)},
            {new TestParam("_X10_IPingService", PORT, false)},
            {new TestParam("_XD_IPingService", PORT, false)},
            {new TestParam("_XD-SEES_IPingService", PORT, false)},
            {new TestParam("_XD-ES_IPingService", PORT, false)},
                
            // TODO Endorsing streaming not supported
            // {new TestParam("SecureConversation_MutualCertificate10SignEncrypt_IPingService", PORT, true)},
            {new TestParam("AC_IPingService", PORT, true)},
            {new TestParam("ADC_IPingService", PORT, true)},
            // TODO Error with "EncryptBeforeSigning" ordering.
            // {new TestParam("ADC-ES_IPingService", PORT, true)},
            {new TestParam("_A_IPingService", PORT, true)},
            {new TestParam("_AD_IPingService", PORT, true)},
            // TODO Error with "EncryptBeforeSigning" ordering.
            // {new TestParam("_AD-ES_IPingService", PORT, true)},
            {new TestParam("UXC_IPingService", PORT, true)},
            {new TestParam("UXDC_IPingService", PORT, true)},
            {new TestParam("UXDC-SEES_IPingService", PORT, true)},
            {new TestParam("_UX_IPingService", PORT, true)},
            {new TestParam("_UXD_IPingService", PORT, true)},
            {new TestParam("_UXD-SEES_IPingService", PORT, true)},
            // TODO Streaming endorsing not working
            // {new TestParam("XC_IPingService", PORT, true)},
            // {new TestParam("XDC_IPingService", PORT, true)},
            // {new TestParam("XDC_IPingService1", PORT, true)},
            // {new TestParam("XDC-ES_IPingService", PORT, true)},
            // {new TestParam("XDC-SEES_IPingService", PORT, true)},
            // {new TestParam("_X_IPingService", PORT, true)},
            {new TestParam("_X10_IPingService", PORT, true)},
            // TODO Streaming endorsing not working
            // {new TestParam("_XD_IPingService", PORT, true)},
            // {new TestParam("_XD-SEES_IPingService", PORT, true)},
            // {new TestParam("_XD-ES_IPingService", PORT, true)},

            // TODO Endorsing derived keys not supported.
            // {new TestParam("SecureConversation_MutualCertificate10SignEncrypt_IPingService", 
            //               STAX_PORT, false)},
            {new TestParam("AC_IPingService", STAX_PORT, false)},
            {new TestParam("ADC_IPingService", STAX_PORT, false)},
            {new TestParam("ADC-ES_IPingService", STAX_PORT, false)},
            {new TestParam("_A_IPingService", STAX_PORT, false)},
            {new TestParam("_AD_IPingService", STAX_PORT, false)},
            {new TestParam("_AD-ES_IPingService", STAX_PORT, false)},
            {new TestParam("UXC_IPingService", STAX_PORT, false)},
            {new TestParam("UXDC_IPingService", STAX_PORT, false)},
            {new TestParam("UXDC-SEES_IPingService", STAX_PORT, false)},
            {new TestParam("_UX_IPingService", STAX_PORT, false)},
            {new TestParam("_UXD_IPingService", STAX_PORT, false)},
            {new TestParam("_UXD-SEES_IPingService", STAX_PORT, false)},
            {new TestParam("XC_IPingService", STAX_PORT, false)},
            {new TestParam("XDC_IPingService", STAX_PORT, false)},
            {new TestParam("XDC_IPingService1", STAX_PORT, false)},
            {new TestParam("XDC-ES_IPingService", STAX_PORT, false)},
            {new TestParam("XDC-SEES_IPingService", STAX_PORT, false)},
            {new TestParam("_X_IPingService", STAX_PORT, false)},
            {new TestParam("_X10_IPingService", STAX_PORT, false)},
            {new TestParam("_XD_IPingService", STAX_PORT, false)},
            {new TestParam("_XD-SEES_IPingService", STAX_PORT, false)},
            {new TestParam("_XD-ES_IPingService", STAX_PORT, false)},

            // TODO Endorsing derived keys not supported.
            // {new TestParam("SecureConversation_MutualCertificate10SignEncrypt_IPingService", 
            //               STAX_PORT, true)},
            {new TestParam("AC_IPingService", STAX_PORT, true)},
            {new TestParam("ADC_IPingService", STAX_PORT, true)},
            // TODO Error with "EncryptBeforeSigning" ordering.
            // {new TestParam("ADC-ES_IPingService", STAX_PORT, true)},
            {new TestParam("_A_IPingService", STAX_PORT, true)},
            {new TestParam("_AD_IPingService", STAX_PORT, true)},
            // TODO Error with "EncryptBeforeSigning" ordering.
            // {new TestParam("_AD-ES_IPingService", STAX_PORT, true)},
            {new TestParam("UXC_IPingService", STAX_PORT, true)},
            {new TestParam("UXDC_IPingService", STAX_PORT, true)},
            {new TestParam("UXDC-SEES_IPingService", STAX_PORT, true)},
            {new TestParam("_UX_IPingService", STAX_PORT, true)},
            {new TestParam("_UXD_IPingService", STAX_PORT, true)},
            {new TestParam("_UXD-SEES_IPingService", STAX_PORT, true)},
            // TODO Streaming endorsing not working 
            // {new TestParam("XC_IPingService", STAX_PORT, true)},
            // {new TestParam("XDC_IPingService", STAX_PORT, true)},
            // {new TestParam("XDC_IPingService1", STAX_PORT, true)},
            // {new TestParam("XDC-ES_IPingService", STAX_PORT, true)},
            // {new TestParam("XDC-SEES_IPingService", STAX_PORT, true)},
            // {new TestParam("_X_IPingService", STAX_PORT, true)},
            {new TestParam("_X10_IPingService", STAX_PORT, true)},
            // TODO Streaming endorsing not working 
            // {new TestParam("_XD_IPingService", STAX_PORT, true)},
            // {new TestParam("_XD-SEES_IPingService", STAX_PORT, true)},
            // {new TestParam("_XD-ES_IPingService", STAX_PORT, true)},
                
        });
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        bus.shutdown(true);
        stopAllServers();
    }
  
    @Test
    public void testSecureConversation() throws Exception {
        final wssec.wssc.IPingService port = 
            svc.getPort(
                new QName("http://WSSec/wssc", test.prefix),
                wssec.wssc.IPingService.class
            );

        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                        "http://localhost:" + test.port + "/" + test.prefix);
        if (test.prefix.charAt(0) == '_') {
            //MS would like the _ versions to send a cancel
            ((BindingProvider)port).getRequestContext()
                .put(SecurityConstants.STS_TOKEN_DO_CANCEL, Boolean.TRUE);
        }

        if (test.streaming) {
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
            throw new Exception("Error doing " + test.prefix, ex);
        }
        ((java.io.Closeable)port).close();
    }

    
}
