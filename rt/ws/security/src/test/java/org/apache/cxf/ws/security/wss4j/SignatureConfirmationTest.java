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
package org.apache.cxf.ws.security.wss4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.DOMUtils.NullResolver;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;


/**
 * This a test of the Signature Confirmation functionality that is contained in the
 * WS-Security 1.1 specification. The requestor signs an outbound SOAP message and saves
 * the signature. The responder processes the inbound SOAP message and saves the received
 * signature. Then in the responding message the received signature is attached in the
 * form of a wsse11:SignatureConfirmation blob. The requestor processes this blob and
 * checks to make sure that the signature value contained therein matches the saved value.
 */
public class SignatureConfirmationTest extends AbstractSecurityTest {
    
    public SignatureConfirmationTest() {
    }
    
    @org.junit.Test
    @SuppressWarnings("unchecked")
    public void testSignatureConfirmationRequest() throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);
        
        SOAPMessage saajMsg = MessageFactory.newInstance().createMessage();
        SOAPPart part = saajMsg.getSOAPPart();
        part.setContent(new DOMSource(doc));
        saajMsg.saveChanges();

        msg.setContent(SOAPMessage.class, saajMsg);

        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        msg.put(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "myalias");
        msg.put("password", "myAliasPassword");
        //
        // This is necessary to convince the WSS4JOutInterceptor that we're
        // functioning as a requestor
        //
        msg.put(org.apache.cxf.message.Message.REQUESTOR_ROLE, true);

        handler.handleMessage(msg);
        doc = part;
        
        assertValid("//wsse:Security", doc);
        assertValid("//wsse:Security/ds:Signature", doc);

        byte[] docbytes = getMessageBytes(doc);
        //
        // Save the signature for future confirmation
        //
        Object sigv = msg.get(WSHandlerConstants.SEND_SIGV);
        assert sigv != null;
        assert sigv instanceof List;
        assert ((List<Object>)sigv).size() != 0;
        List<Object> sigSaved = (List<Object>)sigv;
        
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(docbytes));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setValidating(false);
        dbf.setIgnoringComments(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);

        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new NullResolver());
        doc = StaxUtils.read(db, reader, false);

        WSS4JInInterceptor inHandler = new WSS4JInInterceptor();

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        inHandler.setProperty(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/insecurity.properties");

        inHandler.handleMessage(inmsg);
        
        //
        // Check that the inbound signature result was saved
        //
        WSSecurityEngineResult result = 
            (WSSecurityEngineResult) inmsg.get(WSS4JInInterceptor.SIGNATURE_RESULT);
        assertNotNull(result);
        
        List<Object> sigReceived = (List<Object>)inmsg.get(WSHandlerConstants.RECV_RESULTS);
        assert sigReceived != null;
        assert sigReceived.size() != 0;
        
        testSignatureConfirmationResponse(sigSaved, sigReceived);
    }
    
   
    private void testSignatureConfirmationResponse(
        List<Object> sigSaved,
        List<Object> sigReceived
    ) throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);
        
        SOAPMessage saajMsg = MessageFactory.newInstance().createMessage();
        SOAPPart part = saajMsg.getSOAPPart();
        part.setContent(new DOMSource(doc));
        saajMsg.saveChanges();

        msg.setContent(SOAPMessage.class, saajMsg);

        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.TIMESTAMP);
        msg.put(WSHandlerConstants.RECV_RESULTS, sigReceived);
        
        handler.handleMessage(msg);

        doc = part;
        
        assertValid("//wsse:Security", doc);
        // assertValid("//wsse:Security/wsse11:SignatureConfirmation", doc);

        byte[] docbytes = getMessageBytes(doc);
        // System.out.println(new String(docbytes));
        
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(docbytes));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setValidating(false);
        dbf.setIgnoringComments(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);

        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new NullResolver());
        doc = StaxUtils.read(db, reader, false);

        WSS4JInInterceptor inHandler = new WSS4JInInterceptor();

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.TIMESTAMP);
        inmsg.put(WSHandlerConstants.SEND_SIGV, sigSaved);

        inHandler.handleMessage(inmsg);
    }
    
    
    private byte[] getMessageBytes(Document doc) throws Exception {
        // XMLOutputFactory factory = XMLOutputFactory.newInstance();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // XMLStreamWriter byteArrayWriter =
        // factory.createXMLStreamWriter(outputStream);
        XMLStreamWriter byteArrayWriter = StaxUtils.createXMLStreamWriter(outputStream);

        StaxUtils.writeDocument(doc, byteArrayWriter, false);

        byteArrayWriter.flush();
        return outputStream.toByteArray();
    }
}
