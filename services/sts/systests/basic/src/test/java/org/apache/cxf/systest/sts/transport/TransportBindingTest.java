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
package org.apache.cxf.systest.sts.transport;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.systest.sts.TLSClientParametersUtils;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TestParam;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.systest.sts.deployment.StaxDoubleItServer;
import org.apache.cxf.systest.sts.deployment.StaxSTSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.wss4j.common.WSS4JConstants;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the TransportBinding. The CXF client gets a token from the STS over TLS, and then
 * sends it to the CXF endpoint over TLS.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class TransportBindingTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);
    static final String STAX_STSPORT2 = allocatePort(StaxSTSServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);
    private static final String STAX_PORT = allocatePort(StaxDoubleItServer.class);

    final TestParam test;

    public TransportBindingTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            TransportBindingTest.class.getResource("cxf-service.xml"),
            TransportBindingTest.class.getResource("cxf-stax-service.xml")))
        );
        assertTrue(launchServer(new STSServer(
            "cxf-transport.xml",
            "stax-cxf-transport.xml"
        )));
    }

    @Parameters(name = "{0}")
    public static TestParam[] data() {
        return new TestParam[] {new TestParam(PORT, false, STSPORT),
                                new TestParam(PORT, true, STSPORT),
                                new TestParam(STAX_PORT, false, STSPORT),
                                new TestParam(STAX_PORT, true, STSPORT),

                                new TestParam(PORT, false, STAX_STSPORT),
                                new TestParam(PORT, true, STAX_STSPORT),
                                new TestParam(STAX_PORT, false, STAX_STSPORT),
                                new TestParam(STAX_PORT, true, STAX_STSPORT),
        };
    }

    @org.junit.Test
    public void testSAML1() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType transportSaml1Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml1Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml1Port);
        }

        doubleIt(transportSaml1Port, 25);

        ((java.io.Closeable)transportSaml1Port).close();
    }

    @org.junit.Test
    public void testSAML2() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Port");
        DoubleItPortType transportSaml2Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml2Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml2Port);
        }

        doubleIt(transportSaml2Port, 25);

        ((java.io.Closeable)transportSaml2Port).close();
    }

    @org.junit.Test
    public void testSAML2ViaCode() throws Exception {

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Port");
        DoubleItPortType transportSaml2Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml2Port);
        }

        // TLS configuration
        TLSClientParameters tlsParams = TLSClientParametersUtils.getTLSClientParameters();

        Client client = ClientProxy.getClient(transportSaml2Port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.setTlsClientParameters(tlsParams);

        // STSClient configuration
        Bus clientBus = BusFactory.newInstance().createBus();
        STSClient stsClient = new STSClient(clientBus);

        // HTTPS configuration for the STSClient
        stsClient.setTlsClientParameters(tlsParams);

        // Use a local WSDL or else we run into problems retrieving the WSDL over HTTPS
        // due to lack of TLS config when creating the client
        URL stsWsdl = TransportBindingTest.class.getResource("../deployment/ws-trust-1.4-service.wsdl");
        stsClient.setWsdlLocation(stsWsdl.toString());
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> props = new HashMap<>();
        props.put("security.username", "alice");
        props.put("security.callback-handler", "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        props.put("security.sts.token.username", "myclientkey");
        props.put("security.sts.token.properties", "clientKeystore.properties");
        props.put("security.sts.token.usecert", "false");
        stsClient.setProperties(props);

        ((BindingProvider)transportSaml2Port).getRequestContext().put("security.sts.client", stsClient);

        // Update ports
        updateAddressPort(stsClient.getClient(), test.getStsPort());

        doubleIt(transportSaml2Port, 25);

        ((java.io.Closeable)transportSaml2Port).close();
        clientBus.shutdown(true);
    }

    /**
     * In this test-case, the client sends another cert to the STS for inclusion in the
     * SAML Assertion and connects via 2-way TLS as normal to the service provider. The
     * service provider will fail, as the TLS cert does not match the cert provided in
     * the SAML Assertion.
     */
    @org.junit.Test
    public void testUnknownClient() throws Exception {
        createBus(getClass().getResource("cxf-bad-client.xml").toString());

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType transportSaml1Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml1Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml1Port);
        }

        try {
            doubleIt(transportSaml1Port, 35);
            fail("Expected failure on an unknown client");
        } catch (jakarta.xml.ws.soap.SOAPFaultException fault) {
            // expected
        }

        ((java.io.Closeable)transportSaml1Port).close();
    }

    @org.junit.Test
    public void testSAML1Endorsing() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1EndorsingPort");
        DoubleItPortType transportSaml1Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml1Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml1Port);
        }

        doubleIt(transportSaml1Port, 25);

        ((java.io.Closeable)transportSaml1Port).close();
    }

    /**
     * In this test-case, the client sends a request for a Security Token with no
     * AppliesTo address (configured in Spring on the STSClient object). The STS fails as
     * it will not issue a token to an unknown address.
     */
    @org.junit.Test
    public void testUnknownAddress() throws Exception {
        createBus(getClass().getResource("cxf-bad-client.xml").toString());

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1EndorsingPort");
        DoubleItPortType transportSaml1Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml1Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml1Port);
        }

        try {
            doubleIt(transportSaml1Port, 35);
            //fail("Expected failure on an unknown address");
        } catch (jakarta.xml.ws.soap.SOAPFaultException fault) {
            // expected
        }

        ((java.io.Closeable)transportSaml1Port).close();
    }

    @org.junit.Test
    public void testSAML2Dispatch() throws Exception {

        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Port");

        Dispatch<DOMSource> dispatch =
            service.createDispatch(portQName, DOMSource.class, Service.Mode.PAYLOAD);
        updateAddressPort(dispatch, test.getPort());

        // Setup STSClient
        STSClient stsClient = createDispatchSTSClient(bus);
        String wsdlLocation = "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport?wsdl";
        stsClient.setWsdlLocation(wsdlLocation);

        // Creating a DOMSource Object for the request
        DOMSource request = createDOMRequest();

        // Make a successful request
        Client client = ((DispatchImpl<DOMSource>) dispatch).getClient();
        client.getRequestContext().put(SecurityConstants.USERNAME, "alice");
        client.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);

        if (test.isStreaming()) {
            client.getRequestContext().put(SecurityConstants.ENABLE_STREAMING_SECURITY, "true");
            client.getResponseContext().put(SecurityConstants.ENABLE_STREAMING_SECURITY, "true");
        }

        DOMSource response = dispatch.invoke(request);
        assertNotNull(response);
    }

    @org.junit.Test
    public void testSAML2DispatchLocation() throws Exception {

        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Port");

        Dispatch<DOMSource> dispatch =
            service.createDispatch(portQName, DOMSource.class, Service.Mode.PAYLOAD);
        updateAddressPort(dispatch, test.getPort());

        // Setup STSClient
        STSClient stsClient = createDispatchSTSClient(bus);
        String location = "https://localhost:" + test.getStsPort() + "/SecurityTokenService/Transport";
        stsClient.setLocation(location);
        stsClient.setPolicy("classpath:/org/apache/cxf/systest/sts/issuer/sts-transport-policy.xml");

        // Creating a DOMSource Object for the request
        DOMSource request = createDOMRequest();

        // Make a successful request
        Client client = ((DispatchImpl<DOMSource>) dispatch).getClient();
        client.getRequestContext().put(SecurityConstants.USERNAME, "alice");
        client.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);

        if (test.isStreaming()) {
            client.getRequestContext().put(SecurityConstants.ENABLE_STREAMING_SECURITY, "true");
            client.getResponseContext().put(SecurityConstants.ENABLE_STREAMING_SECURITY, "true");
        }

        DOMSource response = dispatch.invoke(request);
        assertNotNull(response);
    }

    @org.junit.Test
    public void testSAML2X509Endorsing() throws Exception {

        // Only works for DOM (clients)
        if (test.isStreaming()) {
            return;
        }

        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2X509EndorsingPort");
        DoubleItPortType transportSaml1Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml1Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml1Port);
        }

        doubleIt(transportSaml1Port, 25);

        ((java.io.Closeable)transportSaml1Port).close();
    }

    @org.junit.Test
    public void testSAML2SymmetricEndorsing() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2SymmetricEndorsingPort");
        DoubleItPortType transportSaml1Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml1Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml1Port);
        }

        doubleIt(transportSaml1Port, 25);

        ((java.io.Closeable)transportSaml1Port).close();
    }

    @org.junit.Test
    public void testSAML2SymmetricEndorsingDerived() throws Exception {

        // Only works for DOM (clients)
        if (test.isStreaming()) {
            return;
        }

        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TransportBindingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2SymmetricEndorsingDerivedPort");
        DoubleItPortType transportSaml1Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml1Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml1Port);
        }

        doubleIt(transportSaml1Port, 25);

        ((java.io.Closeable)transportSaml1Port).close();
    }

    private DOMSource createDOMRequest() throws ParserConfigurationException {
        // Creating a DOMSource Object for the request
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document requestDoc = db.newDocument();
        Element root = requestDoc.createElementNS("http://www.example.org/schema/DoubleIt", "ns2:DoubleIt");
        root.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns:ns2", "http://www.example.org/schema/DoubleIt");
        Element number = requestDoc.createElementNS(null, "numberToDouble");
        number.setTextContent("25");
        root.appendChild(number);
        requestDoc.appendChild(root);
        return new DOMSource(requestDoc);
    }

    private STSClient createDispatchSTSClient(Bus bus) {
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
