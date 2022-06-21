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
package org.apache.cxf.systest.sts.custom;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Element;

import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.processor.SAMLTokenProcessor;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test sends a custom parameter indicating the "realm" of the user, which is interpreted by the
 * STS's CustomUTValidator.
 */
public class CustomParameterTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);

    private static final String SAML2_TOKEN_TYPE =
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            CustomParameterTest.class.getResource("cxf-service.xml"))));
        assertTrue(launchServer(new STSServer(
            CustomParameterTest.class.getResource("cxf-sts.xml"))));
    }

    // Here the custom parameter in the RST is parsed by the CustomUTValidator
    @org.junit.Test
    public void testCustomParameterInRSTValidator() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = CustomParameterTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportCustomParameterPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportClaimsPort, PORT);

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, STSPORT);

        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("https://localhost:" + STSPORT + "/SecurityTokenService/UT?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}UT_Port");

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(SecurityConstants.CALLBACK_HANDLER, "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        properties.put("security.sts.token.username", "myclientkey");
        properties.put("security.sts.token.properties", "clientKeystore.properties");
        properties.put("security.sts.token.usecert", "true");
        stsClient.setProperties(properties);

        ((BindingProvider)transportClaimsPort).getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);

        // Successful test

        // Add custom content to the RST
        stsClient.setCustomContent("<realm xmlns=\"http://cxf.apache.org/custom\">custom-realm</realm>");
        doubleIt(transportClaimsPort, 25);

        ((java.io.Closeable)transportClaimsPort).close();
    }

    // Here the custom parameter in the RST is parsed by the CustomUTValidator
    @org.junit.Test
    public void testCustomParameterInRST2Validator() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = CustomParameterTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportCustomParameterPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportClaimsPort, PORT);

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, STSPORT);

        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("https://localhost:" + STSPORT + "/SecurityTokenService/UT?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}UT_Port");

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(SecurityConstants.CALLBACK_HANDLER, "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        properties.put("security.sts.token.username", "myclientkey");
        properties.put("security.sts.token.properties", "clientKeystore.properties");
        properties.put("security.sts.token.usecert", "true");
        stsClient.setProperties(properties);

        ((BindingProvider)transportClaimsPort).getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);

        // Failing test

        // Add custom content to the RST
        stsClient.setCustomContent("<realm xmlns=\"http://cxf.apache.org/custom\">custom-unknown-realm</realm>");
        try {
            doubleIt(transportClaimsPort, 25);
            fail("Failure expected on the wrong realm");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)transportClaimsPort).close();
    }

    // Here the custom parameter in the RST is parsed by the CustomClaimsHandler
    @org.junit.Test
    public void testCustomParameterInRSTClaimsHandler() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = CustomParameterTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportCustomParameterClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportClaimsPort, PORT);

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, STSPORT);

        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("https://localhost:" + STSPORT + "/SecurityTokenService/Transport?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(SecurityConstants.CALLBACK_HANDLER, "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        properties.put("security.sts.token.username", "myclientkey");
        properties.put("security.sts.token.properties", "clientKeystore.properties");
        properties.put("security.sts.token.usecert", "true");
        stsClient.setProperties(properties);

        ((BindingProvider)transportClaimsPort).getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);

        // Successful test

        // Add custom content to the RST
        stsClient.setCustomContent("<realm xmlns=\"http://cxf.apache.org/custom\">custom-realm</realm>");
        doubleIt(transportClaimsPort, 25);

        ((java.io.Closeable)transportClaimsPort).close();
    }

    // Here the custom parameter in the RST is parsed by the CustomClaimsHandler
    @org.junit.Test
    public void testCustomParameterInRSTClaimsHandler2() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = CustomParameterTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportCustomParameterClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportClaimsPort, PORT);

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, STSPORT);

        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("https://localhost:" + STSPORT + "/SecurityTokenService/Transport?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(SecurityConstants.CALLBACK_HANDLER, "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        properties.put("security.sts.token.username", "myclientkey");
        properties.put("security.sts.token.properties", "clientKeystore.properties");
        properties.put("security.sts.token.usecert", "true");
        stsClient.setProperties(properties);

        ((BindingProvider)transportClaimsPort).getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);

        // Failing test

        // Add custom content to the RST
        stsClient.setCustomContent("<realm xmlns=\"http://cxf.apache.org/custom\">custom-unknown-realm</realm>");
        try {
            doubleIt(transportClaimsPort, 25);
            fail("Failure expected on the wrong realm");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)transportClaimsPort).close();
    }

    @org.junit.Test
    public void testCustomParameterToRESTInterface() throws Exception {
        String address = "https://localhost:" + STSPORT + "/SecurityTokenServiceREST/token";
        WebClient client = WebClient.create(address, getClass().getResource("cxf-client.xml").toString());

        client.type("application/xml").accept("application/xml");

        // Create RequestSecurityToken
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        String namespace = STSUtils.WST_NS_05_12;
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);

        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Issue");
        writer.writeEndElement();

        writer.writeStartElement("wst", "TokenType", namespace);
        writer.writeCharacters(SAML2_TOKEN_TYPE);
        writer.writeEndElement();

        writer.writeStartElement("wst", "Claims", namespace);
        writer.writeAttribute("Dialect", "http://schemas.xmlsoap.org/ws/2005/05/identity");
        writer.writeStartElement("ic", "ClaimType", "http://schemas.xmlsoap.org/ws/2005/05/identity");
        writer.writeAttribute("Uri", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");
        writer.writeEndElement();
        writer.writeEndElement();

        // Add custom content to the RST
        writer.writeStartElement("", "realm", "http://cxf.apache.org/custom");
        writer.writeCharacters("custom-realm");
        writer.writeEndElement();

        writer.writeEndElement();

        Response response = client.post(new DOMSource(writer.getDocument().getDocumentElement()));

        RequestSecurityTokenResponseType securityResponse =
            response.readEntity(RequestSecurityTokenResponseType.class);

        Element assertion = validateSAMLSecurityTokenResponse(securityResponse, true);
        assertTrue(DOM2Writer.nodeToString(assertion).contains("admin-user"));
    }

    private Element validateSAMLSecurityTokenResponse(
         RequestSecurityTokenResponseType securityResponse, boolean saml2
    ) throws Exception {
        RequestedSecurityTokenType requestedSecurityToken = getRequestedSecurityToken(securityResponse);
        assertNotNull(requestedSecurityToken);

        // Process the token
        List<WSSecurityEngineResult> results =
            processToken((Element)requestedSecurityToken.getAny());

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertNotNull(assertion);
        if (saml2) {
            assertTrue(assertion.getSaml2() != null && assertion.getSaml1() == null);
        } else {
            assertTrue(assertion.getSaml2() == null && assertion.getSaml1() != null);
        }
        assertTrue(assertion.isSigned());

        return (Element)results.get(0).get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
    }

    private RequestedSecurityTokenType getRequestedSecurityToken(RequestSecurityTokenResponseType securityResponse) {
        for (Object obj : securityResponse.getAny()) {
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>)obj;
                if ("RequestedSecurityToken".equals(jaxbElement.getName().getLocalPart())) {
                    return (RequestedSecurityTokenType)jaxbElement.getValue();
                }
            }
        }
        return null;
    }

    private List<WSSecurityEngineResult> processToken(Element assertionElement)
        throws Exception {
        RequestData requestData = new RequestData();
        requestData.setDisableBSPEnforcement(true);
        CallbackHandler callbackHandler = new org.apache.cxf.systest.sts.common.CommonCallbackHandler();
        requestData.setCallbackHandler(callbackHandler);
        Crypto crypto = CryptoFactory.getInstance("serviceKeystore.properties");
        requestData.setDecCrypto(crypto);
        requestData.setSigVerCrypto(crypto);
        requestData.setWsDocInfo(new WSDocInfo(assertionElement.getOwnerDocument()));

        Processor processor = new SAMLTokenProcessor();
        return processor.handleToken(assertionElement, requestData);
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
