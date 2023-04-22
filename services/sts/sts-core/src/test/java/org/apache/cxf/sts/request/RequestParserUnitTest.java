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
package org.apache.cxf.sts.request;

import java.io.StringReader;
import java.util.Collections;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.token.canceller.SCTCanceller;
import org.apache.cxf.sts.token.validator.SCTValidator;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RequestParserUnitTest {

    private static final String SECURITY_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><wsse:Security "
        + "xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\""
        + " xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\""
        + "><wsse:UsernameToken wsu:Id=\"UsernameToken-5\"><wsse:Username>alice</wsse:Username>"
        + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username"
        + "-token-profile-1.0#PasswordText\">clarinet</wsse:Password>"
        + "</wsse:UsernameToken><wsc:SecurityContextToken "
        + "xmlns:wsc=\"http://schemas.xmlsoap.org/ws/2005/02/sc\" "
        + "xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" "
        + "wsu:Id=\"sct\"><wsc:Identifier>check</wsc:Identifier></wsc:SecurityContextToken></wsse:Security>";

    private static final String SECURITY_HEADER_X509 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><wsse:Security "
        + "xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\""
        + " xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\""
        + "><wsse:UsernameToken wsu:Id=\"UsernameToken-5\"><wsse:Username>alice</wsse:Username>"
        + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username"
        + "-token-profile-1.0#PasswordText\">clarinet</wsse:Password>"
        + "</wsse:UsernameToken><wsse:BinarySecurityToken "
        + "xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" "
        + "EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0"
        + "#Base64Binary\" "
        + "ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0"
        + "#X509v3\" " + "wsu:Id=\"x509\">"
        + "MIIEFjCCA3+gAwIBAgIJAJORWX2Xsa8DMA0GCSqGSIb3DQEBBQUAMIG5MQswCQYDVQQGEwJVUzERMA8GA1UECBMITmV3IFlvcm"
        + "sxFjAUBgNVBAcTDU5pYWdhcmEgRmFsbHMxLDAqBgNVBAoTI1NhbXBsZSBDbGllbnQgLS0gTk9UIEZPUiBQUk9EVUNUSU9OMRYw"
        + "FAYDVQQLEw1JVCBEZXBhcnRtZW50MRcwFQYDVQQDEw53d3cuY2xpZW50LmNvbTEgMB4GCSqGSIb3DQEJARYRY2xpZW50QGNsaW"
        + "VudC5jb20wHhcNMTEwMjA5MTgzMDI3WhcNMjEwMjA2MTgzMDI3WjCBuTELMAkGA1UEBhMCVVMxETAPBgNVBAgTCE5ldyBZb3Jr"
        + "MRYwFAYDVQQHEw1OaWFnYXJhIEZhbGxzMSwwKgYDVQQKEyNTYW1wbGUgQ2xpZW50IC0tIE5PVCBGT1IgUFJPRFVDVElPTjEWMB"
        + "QGA1UECxMNSVQgRGVwYXJ0bWVudDEXMBUGA1UEAxMOd3d3LmNsaWVudC5jb20xIDAeBgkqhkiG9w0BCQEWEWNsaWVudEBjbGll"
        + "bnQuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDauFNVqi4B2+u/PC9ktDkn82bglEQYcL4o5JRUhQVEhTK2iEloz1"
        + "Rvo/qyfDhBPc1lzIUn4ams+DKBSSjZMCgop3XbeCXzIVP784ruC8HF5QrYsXUQfTc7lzqafXZXH8Bk89gSScA1fFme6TpvYzM0"
        + "zjBETSXADtKOs9oKB2VOIwIDAQABo4IBIjCCAR4wHQYDVR0OBBYEFFIz+0BSZlLtXkA/udRjRgphtREuMIHuBgNVHSMEgeYwge"
        + "OAFFIz+0BSZlLtXkA/udRjRgphtREuoYG/pIG8MIG5MQswCQYDVQQGEwJVUzERMA8GA1UECBMITmV3IFlvcmsxFjAUBgNVBAcT"
        + "DU5pYWdhcmEgRmFsbHMxLDAqBgNVBAoTI1NhbXBsZSBDbGllbnQgLS0gTk9UIEZPUiBQUk9EVUNUSU9OMRYwFAYDVQQLEw1JVC"
        + "BEZXBhcnRtZW50MRcwFQYDVQQDEw53d3cuY2xpZW50LmNvbTEgMB4GCSqGSIb3DQEJARYRY2xpZW50QGNsaWVudC5jb22CCQCT"
        + "kVl9l7GvAzAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBQUAA4GBAEjEr9QfaYsZf7ELnqB++OkWcKxpMt1Yj/VOyL99AekkVT"
        + "M+rRHCU9Bu+tncMNsfy8mIXUC1JqKQ+Cq5RlaDh/ujzt6i17G7uSGd6U1U/DPZBqTm3Dxwl1cMAGU/CoAKTWE+o+fS4Q2xHv7L"
        + "1KiXQQc9EWJ4C34Ik45fB6g3DiTj</wsse:BinarySecurityToken></wsse:Security>";

    private static final String CANCEL_SCT_REFERENCE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<wst:RequestSecurityToken xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">"
        + "<wst:TokenType>http://schemas.xmlsoap.org/ws/2005/02/sc/sct</wst:TokenType>"
        + "<wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Cancel</wst:RequestType>"
        + "<wst:CancelTarget>"
        + "<wsse:SecurityTokenReference "
        + "xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">"
        + "<wsse:Reference URI=\"#sct\"></wsse:Reference></wsse:SecurityTokenReference>"
        + "</wst:CancelTarget>" + "</wst:RequestSecurityToken>";

    private static final String VALIDATE_SCT_REFERENCE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<wst:RequestSecurityToken xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">"
        + "<wst:TokenType>http://schemas.xmlsoap.org/ws/2005/02/sc/sct</wst:TokenType>"
        + "<wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Validate</wst:RequestType>"
        + "<wst:ValidateTarget>"
        + "<wsse:SecurityTokenReference "
        + "xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">"
        + "<wsse:Reference URI=\"#sct\"></wsse:Reference></wsse:SecurityTokenReference>"
        + "</wst:ValidateTarget>" + "</wst:RequestSecurityToken>";

    private static final String USE_KEY_X509_REFERENCE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<wst:RequestSecurityToken xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">"
        + "<wst:TokenType>http://schemas.xmlsoap.org/ws/2005/02/sc/sct</wst:TokenType>"
        + "<wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue</wst:RequestType>"
        + "<wst:UseKey>"
        + "<wsse:SecurityTokenReference "
        + "xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">"
        + "<wsse:Reference URI=\"#x509\"></wsse:Reference></wsse:SecurityTokenReference>"
        + "</wst:UseKey>" + "</wst:RequestSecurityToken>";

    /**
     * Test for fetching (and cancelling) a referenced SecurityContextToken.
     */
    @org.junit.Test
    public void testCancelSCT() throws Exception {
        Element secHeaderElement = (Element) parseStringToElement(SECURITY_HEADER).getFirstChild();
        RequestSecurityTokenType request = createJaxbObject(CANCEL_SCT_REFERENCE);
        RequestParser parser = new RequestParser();

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgContext = new WrappedMessageContext(msg);

        // Process the security header and store the results in the message context
        WSSecurityEngine securityEngine = new WSSecurityEngine();
        RequestData reqData = new RequestData();
        reqData.setCallbackHandler(new PasswordCallbackHandler());

        WSHandlerResult results =
            securityEngine.processSecurityHeader(secHeaderElement, reqData);
        msgContext.put(WSHandlerConstants.RECV_RESULTS, Collections.singletonList(results));

        RequestRequirements requestRequirements = parser.parseRequest(request, msgContext, null, null);

        SCTCanceller sctCanceller = new SCTCanceller();
        assertTrue(sctCanceller.canHandleToken(requestRequirements.getTokenRequirements().getCancelTarget()));
    }

    /**
     * Test for fetching (and validating) a referenced SecurityContextToken.
     */
    @org.junit.Test
    public void testValidateSCT() throws Exception {
        Element secHeaderElement = (Element) parseStringToElement(SECURITY_HEADER).getFirstChild();
        RequestSecurityTokenType request = createJaxbObject(VALIDATE_SCT_REFERENCE);
        RequestParser parser = new RequestParser();

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgContext = new WrappedMessageContext(msg);

        // Process the security header and store the results in the message context
        WSSecurityEngine securityEngine = new WSSecurityEngine();
        RequestData reqData = new RequestData();
        reqData.setCallbackHandler(new PasswordCallbackHandler());

        WSHandlerResult results =
            securityEngine.processSecurityHeader(secHeaderElement, reqData);
        msgContext.put(WSHandlerConstants.RECV_RESULTS, Collections.singletonList(results));

        RequestRequirements requestRequirements = parser.parseRequest(request, msgContext, null, null);

        SCTValidator sctValidator = new SCTValidator();
        assertTrue(sctValidator.canHandleToken(requestRequirements.getTokenRequirements().getValidateTarget()));
    }

    /**
     * Test for fetching (and validating) a referenced BinarySecurityToken from a UseKey Element.
     */
    @org.junit.Test
    public void testUseKeyX509() throws Exception {
        Element secHeaderElement = (Element) parseStringToElement(SECURITY_HEADER_X509).getFirstChild();
        RequestSecurityTokenType request = createJaxbObject(USE_KEY_X509_REFERENCE);
        RequestParser parser = new RequestParser();

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgContext = new WrappedMessageContext(msg);

        // Process the security header and store the results in the message context
        WSSecurityEngine securityEngine = new WSSecurityEngine();
        RequestData reqData = new RequestData();
        reqData.setSigVerCrypto(getCrypto());
        reqData.setCallbackHandler(new PasswordCallbackHandler());

        WSHandlerResult results =
            securityEngine.processSecurityHeader(secHeaderElement, reqData);
        msgContext.put(WSHandlerConstants.RECV_RESULTS, Collections.singletonList(results));

        RequestRequirements requestRequirements = parser.parseRequest(request, msgContext, null, null);

        assertNotNull(requestRequirements.getKeyRequirements().getReceivedCredential().getX509Cert());
    }

    private Document parseStringToElement(String str) throws Exception {
        DocumentBuilderFactory builderFac = DocumentBuilderFactory.newInstance();
        builderFac.setNamespaceAware(true);
        builderFac.setValidating(false);
        builderFac.setIgnoringElementContentWhitespace(true);
        DocumentBuilder docBuilder = builderFac.newDocumentBuilder();
        return docBuilder.parse(new InputSource(new StringReader(str)));
    }

    private RequestSecurityTokenType createJaxbObject(String str) throws Exception {
        JAXBContext jaxbContext =
            JAXBContext.newInstance("org.apache.cxf.ws.security.sts.provider.model");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement<?> jaxbElement =
            (JAXBElement<?>) unmarshaller.unmarshal(new InputSource(new StringReader(str)));
        return (RequestSecurityTokenType) jaxbElement.getValue();
    }

    private Crypto getCrypto() throws WSSecurityException {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "keys/stsstore.jks");

        return CryptoFactory.getInstance(properties);
    }

}
