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

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.interceptor.SoapActionInInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSUtils;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SecureConversation tests.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class WSSCTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String PORT2 = allocatePort(Server.class, 2);
    static final String STAX_PORT = allocatePort(StaxServer.class);
    static final String STAX_PORT2 = allocatePort(StaxServer.class, 2);

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
        final boolean clearAction;

        TestParam(String p, String port, boolean b) {
            prefix = p;
            this.port = port;
            streaming = b;
            clearAction = false;
        }
        TestParam(String p, String port, boolean b, boolean a) {
            prefix = p;
            this.port = port;
            streaming = b;
            clearAction = a;
        }
        public String toString() {
            return prefix + ":" 
                + port + ((STAX_PORT.equals(port) || STAX_PORT2.equals(port)) ? "(stax)" : "") 
                + ":" + (streaming ? "streaming" : "dom")
                + (clearAction ? "/no SOAPAction" : "");
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
    public static Collection<TestParam> data() {
        return Arrays.asList(new TestParam[] {
            new TestParam("SecureConversation_UserNameOverTransport_IPingService", STAX_PORT2, true),
            new TestParam("SecureConversation_UserNameOverTransport_IPingService", PORT2, false),
            new TestParam("SecureConversation_MutualCertificate10SignEncrypt_IPingService", PORT, false),
            new TestParam("AC_IPingService", PORT, false),
            new TestParam("ADC_IPingService", PORT, false),
            new TestParam("ADC-ES_IPingService", PORT, false),
            new TestParam("_A_IPingService", PORT, false),
            new TestParam("_AD_IPingService", PORT, false),
            new TestParam("_AD-ES_IPingService", PORT, false),
            new TestParam("UXC_IPingService", PORT, false),
            new TestParam("UXDC_IPingService", PORT, false),
            new TestParam("UXDC-SEES_IPingService", PORT, false),
            new TestParam("_UX_IPingService", PORT, false),
            new TestParam("_UXD_IPingService", PORT, false),
            new TestParam("_UXD-SEES_IPingService", PORT, false),
            new TestParam("XC_IPingService", PORT, false),
            new TestParam("XDC_IPingService", PORT, false),
            new TestParam("XDC_IPingService1", PORT, false),
            new TestParam("XDC-ES_IPingService", PORT, false),
            new TestParam("XDC-SEES_IPingService", PORT, false),
            new TestParam("_X_IPingService", PORT, false),
            new TestParam("_X10_IPingService", PORT, false),
            new TestParam("_XD_IPingService", PORT, false),
            new TestParam("_XD-SEES_IPingService", PORT, false),
            new TestParam("_XD-ES_IPingService", PORT, false),

            new TestParam("SecureConversation_UserNameOverTransport_IPingService", PORT2, true),
            // TODO Endorsing streaming not supported
            // new TestParam("SecureConversation_MutualCertificate10SignEncrypt_IPingService", PORT, true),
            new TestParam("AC_IPingService", PORT, true),
            new TestParam("ADC_IPingService", PORT, true),
            new TestParam("ADC-ES_IPingService", PORT, true),
            new TestParam("_A_IPingService", PORT, true),
            new TestParam("_AD_IPingService", PORT, true),
            new TestParam("_AD-ES_IPingService", PORT, true),
            new TestParam("UXC_IPingService", PORT, true),
            new TestParam("UXDC_IPingService", PORT, true),
            new TestParam("UXDC-SEES_IPingService", PORT, true),
            new TestParam("_UX_IPingService", PORT, true),
            new TestParam("_UXD_IPingService", PORT, true),
            new TestParam("_UXD-SEES_IPingService", PORT, true),
            // TODO Streaming endorsing not working
            // new TestParam("XC_IPingService", PORT, true),
            // new TestParam("XDC_IPingService", PORT, true),
            // new TestParam("XDC_IPingService1", PORT, true),
            // new TestParam("XDC-ES_IPingService", PORT, true),
            // new TestParam("XDC-SEES_IPingService", PORT, true),
            // new TestParam("_X_IPingService", PORT, true),
            new TestParam("_X10_IPingService", PORT, true),
            // TODO Streaming endorsing not working
            // new TestParam("_XD_IPingService", PORT, true),
            // new TestParam("_XD-SEES_IPingService", PORT, true),
            // new TestParam("_XD-ES_IPingService", PORT, true),

            new TestParam("SecureConversation_UserNameOverTransport_IPingService", STAX_PORT2, false),
            // TODO StAX Policy Validation error caused by incorrect DOM message
            // new TestParam("SecureConversation_MutualCertificate10SignEncrypt_IPingService",
            //               STAX_PORT, false),
            new TestParam("AC_IPingService", STAX_PORT, false),
            new TestParam("ADC_IPingService", STAX_PORT, false),
            new TestParam("ADC-ES_IPingService", STAX_PORT, false),
            new TestParam("_A_IPingService", STAX_PORT, false),
            new TestParam("_AD_IPingService", STAX_PORT, false),
            new TestParam("_AD-ES_IPingService", STAX_PORT, false),
            new TestParam("UXC_IPingService", STAX_PORT, false),
            new TestParam("UXDC_IPingService", STAX_PORT, false),
            new TestParam("UXDC-SEES_IPingService", STAX_PORT, false),
            new TestParam("_UX_IPingService", STAX_PORT, false),
            new TestParam("_UXD_IPingService", STAX_PORT, false),
            new TestParam("_UXD-SEES_IPingService", STAX_PORT, false),
            new TestParam("XC_IPingService", STAX_PORT, false),
            new TestParam("XDC_IPingService", STAX_PORT, false),
            new TestParam("XDC_IPingService1", STAX_PORT, false),
            new TestParam("XDC-ES_IPingService", STAX_PORT, false),
            new TestParam("XDC-SEES_IPingService", STAX_PORT, false),
            new TestParam("_X_IPingService", STAX_PORT, false),
            new TestParam("_X10_IPingService", STAX_PORT, false),
            new TestParam("_XD_IPingService", STAX_PORT, false),
            new TestParam("_XD-SEES_IPingService", STAX_PORT, false),
            new TestParam("_XD-ES_IPingService", STAX_PORT, false),

            // TODO Endorsing derived keys not supported.
            // new TestParam("SecureConversation_MutualCertificate10SignEncrypt_IPingService",
            //               STAX_PORT, true),
            new TestParam("AC_IPingService", STAX_PORT, true),
            new TestParam("ADC_IPingService", STAX_PORT, true),
            new TestParam("ADC-ES_IPingService", STAX_PORT, true),
            new TestParam("_A_IPingService", STAX_PORT, true),
            new TestParam("_AD_IPingService", STAX_PORT, true),
            new TestParam("_AD-ES_IPingService", STAX_PORT, true),
            new TestParam("UXC_IPingService", STAX_PORT, true),
            new TestParam("UXDC_IPingService", STAX_PORT, true),
            new TestParam("UXDC-SEES_IPingService", STAX_PORT, true),
            new TestParam("_UX_IPingService", STAX_PORT, true),
            new TestParam("_UXD_IPingService", STAX_PORT, true),
            new TestParam("_UXD-SEES_IPingService", STAX_PORT, true),
            // TODO Streaming endorsing not working
            // new TestParam("XC_IPingService", STAX_PORT, true),
            // new TestParam("XDC_IPingService", STAX_PORT, true),
            // new TestParam("XDC_IPingService1", STAX_PORT, true),
            // new TestParam("XDC-ES_IPingService", STAX_PORT, true),
            // new TestParam("XDC-SEES_IPingService", STAX_PORT, true),
            // new TestParam("_X_IPingService", STAX_PORT, true),
            new TestParam("_X10_IPingService", STAX_PORT, true),
            // TODO Streaming endorsing not working
            // new TestParam("_XD_IPingService", STAX_PORT, true),
            // new TestParam("_XD-SEES_IPingService", STAX_PORT, true),
            // new TestParam("_XD-ES_IPingService", STAX_PORT, true),

            new TestParam("AC_IPingService", PORT, false, true),
            new TestParam("AC_IPingService", PORT, true, true),
            new TestParam("AC_IPingService", STAX_PORT, false, true),
            new TestParam("AC_IPingService", STAX_PORT, true, true),
        });
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
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

        if (PORT2.equals(test.port) || STAX_PORT2.equals(test.port)) {
            ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                        "https://localhost:" + test.port + "/" + test.prefix);
        } else {
            ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                    "http://localhost:" + test.port + "/" + test.prefix);
        }

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
        if (test.clearAction) {
            AbstractPhaseInterceptor<Message> clearActionInterceptor
                = new AbstractPhaseInterceptor<Message>(Phase.POST_LOGICAL) {
                    public void handleMessage(Message message) throws Fault {
                        STSClient client = STSUtils.getClient(message, "sct");
                        client.getOutInterceptors().add(this);
                        message.put(SecurityConstants.STS_CLIENT, client);
                        String s = (String)message.get(SoapBindingConstants.SOAP_ACTION);
                        if (s == null) {
                            s = SoapActionInInterceptor.getSoapAction(message);
                        }
                        if (s != null && s.contains("RST/SCT")) {
                            message.put(SoapBindingConstants.SOAP_ACTION, "");
                        }
                    }
                };
            clearActionInterceptor.addBefore(SoapPreProtocolOutInterceptor.class.getName());
            ((Client)port).getOutInterceptors().add(clearActionInterceptor);
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
