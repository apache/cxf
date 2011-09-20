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
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.token.canceller.SCTCanceller;
import org.apache.cxf.sts.token.validator.SCTValidator;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;

public class RequestParserUnitTest extends org.junit.Assert {

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
        WebServiceContextImpl wsContext = new WebServiceContextImpl(msgContext);
        
        // Process the security header and store the results in the message context
        WSSecurityEngine securityEngine = new WSSecurityEngine();
        RequestData reqData = new RequestData();
        reqData.setCallbackHandler(new PasswordCallbackHandler());
        
        List<WSSecurityEngineResult> engineResultList = 
            securityEngine.processSecurityHeader(secHeaderElement, reqData);
        List<WSHandlerResult> resultsList = new ArrayList<WSHandlerResult>();
        resultsList.add(new WSHandlerResult("actor", engineResultList));
        msgContext.put(WSHandlerConstants.RECV_RESULTS, resultsList);
        
        parser.parseRequest(request, wsContext);
        
        SCTCanceller sctCanceller = new SCTCanceller();
        assertTrue(sctCanceller.canHandleToken(parser.getTokenRequirements().getCancelTarget()));
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
        WebServiceContextImpl wsContext = new WebServiceContextImpl(msgContext);
        
        // Process the security header and store the results in the message context
        WSSecurityEngine securityEngine = new WSSecurityEngine();
        RequestData reqData = new RequestData();
        reqData.setCallbackHandler(new PasswordCallbackHandler());
        
        List<WSSecurityEngineResult> engineResultList = 
            securityEngine.processSecurityHeader(secHeaderElement, reqData);
        List<WSHandlerResult> resultsList = new ArrayList<WSHandlerResult>();
        resultsList.add(new WSHandlerResult("actor", engineResultList));
        msgContext.put(WSHandlerConstants.RECV_RESULTS, resultsList);
        
        parser.parseRequest(request, wsContext);
        
        SCTValidator sctValidator = new SCTValidator();
        assertTrue(sctValidator.canHandleToken(parser.getTokenRequirements().getValidateTarget()));
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

}
