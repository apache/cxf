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
package org.apache.cxf.systest.sts.template;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TestParam;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.systest.sts.deployment.StaxDoubleItServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test various aspects of the RequestSecurityTokenTemplate. Make sure that if we are expecting
 * a SAML 2.0 token, that's what we get etc. Same goes for the KeyType.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class TemplateTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);
    private static final String STAX_PORT = allocatePort(StaxDoubleItServer.class);

    final TestParam test;

    public TemplateTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            TemplateTest.class.getResource("cxf-service.xml"),
            TemplateTest.class.getResource("cxf-stax-service.xml")))
        );
        assertTrue(launchServer(new STSServer("cxf-transport.xml")));
    }

    @Parameters(name = "{0}")
    public static TestParam[] data() {
        return new TestParam[] {new TestParam(PORT, false, STSPORT),
                                new TestParam(STAX_PORT, false, STSPORT),
        };
    }

    @org.junit.Test
    public void testSAML1PublicKey() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TemplateTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1PublicKeyPort");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Setup STSClient
        STSClient stsClient = createSTSClient(bus);
        String wsdlLocation = "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        stsClient.setWsdlLocation(wsdlLocation);

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.STS_CLIENT, stsClient
        );

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        doubleIt(port, 25);

        ((java.io.Closeable)port).close();
    }

    @org.junit.Test
    public void testSendSAML2ToSAML1PublicKey() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TemplateTest.class.getResource("DoubleItNoTemplate.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1PublicKeyPort");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Setup STSClient
        STSClient stsClient = createSTSClient(bus);
        stsClient.setKeyType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey");
        stsClient.setTokenType("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0");
        String wsdlLocation = "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        stsClient.setWsdlLocation(wsdlLocation);

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.STS_CLIENT, stsClient
        );

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            doubleIt(port, 25);
            fail("Failure expected on sending a SAML 2.0 token");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
    }

    @org.junit.Test
    public void testSendBearerToSAML1PublicKey() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TemplateTest.class.getResource("DoubleItNoTemplate2.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1PublicKeyPort");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Setup STSClient
        STSClient stsClient = createSTSClient(bus);
        stsClient.setKeyType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer");
        stsClient.setTokenType("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1");
        String wsdlLocation = "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        stsClient.setWsdlLocation(wsdlLocation);

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.STS_CLIENT, stsClient
        );
        ((BindingProvider)port).getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME,
                                                        "myclientkey");
        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            doubleIt(port, 25);
            fail("Failure expected on sending a SAML 1.1 Bearer token");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
    }

    @org.junit.Test
    public void testSendSAML2PublicKey() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TemplateTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2PublicKeyPort");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Setup STSClient
        STSClient stsClient = createSTSClient(bus);
        String wsdlLocation = "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        stsClient.setWsdlLocation(wsdlLocation);

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.STS_CLIENT, stsClient
        );

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        doubleIt(port, 25);

        ((java.io.Closeable)port).close();
    }

    @org.junit.Test
    public void testSendSAML1ToSAML2PublicKey() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TemplateTest.class.getResource("DoubleItNoTemplate.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2PublicKeyPort");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Setup STSClient
        STSClient stsClient = createSTSClient(bus);
        stsClient.setKeyType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey");
        stsClient.setTokenType("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1");
        String wsdlLocation = "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        stsClient.setWsdlLocation(wsdlLocation);

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.STS_CLIENT, stsClient
        );

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            doubleIt(port, 25);
            fail("Failure expected on sending a SAML 1.1 token");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
    }

    @org.junit.Test
    public void testBearerToSAML2PublicKey() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TemplateTest.class.getResource("DoubleItNoTemplate2.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2PublicKeyPort");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Setup STSClient
        STSClient stsClient = createSTSClient(bus);
        stsClient.setKeyType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer");
        stsClient.setTokenType("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0");
        String wsdlLocation = "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        stsClient.setWsdlLocation(wsdlLocation);

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.STS_CLIENT, stsClient
        );

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            doubleIt(port, 25);
            fail("Failure expected on sending a SAML 2.0 Bearer token");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
    }

    private STSClient createSTSClient(Bus bus) {
        STSClient stsClient = new STSClient(bus);
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        properties.put("ws-security.sts.token.username", "myclientkey");
        properties.put("ws-security.sts.token.properties", "clientKeystore.properties");
        properties.put("ws-security.sts.token.usecert", "true");
        stsClient.setProperties(properties);

        return stsClient;
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
