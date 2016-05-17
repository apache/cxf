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

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.DOMUtils.NullResolver;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.junit.Test;


/**
 * A number of tests for fault codes that are thrown from WSS4JInInterceptor.
 */
public class WSS4JFaultCodeTest extends AbstractSecurityTest {

    public WSS4JFaultCodeTest() {
    }

    /**
     * Test for WSS4JInInterceptor when it receives a message with no security header. 
     */
    @Test
    public void testNoSecurity() throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        SoapMessage msg = getSoapMessageForDom(doc);
        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();
        
        byte[] docbytes = getMessageBytes(doc);
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
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        inHandler.setProperty(WSHandlerConstants.DEC_PROP_FILE, "insecurity.properties");
        inHandler.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, TestPwdCallback.class.getName());
        
        inmsg.put(SecurityConstants.RETURN_SECURITY_ERROR, Boolean.TRUE);
        
        try {
            inHandler.handleMessage(inmsg);
            fail("Expected failure on an message with no security header");
        } catch (SoapFault fault) {
            assertTrue(fault.getReason().startsWith(
                "An error was discovered processing the <wsse:Security> header"));
            QName faultCode = new QName(WSConstants.WSSE_NS, "InvalidSecurity");
            assertTrue(fault.getFaultCode().equals(faultCode));
        }
    }
    
    /**
     * Test that an invalid Timestamp gets mapped to a proper fault code 
     */
    @Test
    public void testInvalidTimestamp() throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = getSoapMessageForDom(doc);

        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.TIMESTAMP);
        msg.put(WSHandlerConstants.TTL_TIMESTAMP, "1");

        handler.handleMessage(msg);

        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();
        
        assertValid("//wsse:Security", doc);

        byte[] docbytes = getMessageBytes(doc);
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
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.TIMESTAMP);
        inHandler.setProperty(WSHandlerConstants.TTL_TIMESTAMP, "1");
        inmsg.put(SecurityConstants.RETURN_SECURITY_ERROR, Boolean.TRUE);

        try {
            //
            // Sleep for over a second to make the timestamp invalid
            //
            Thread.sleep(1250);
            inHandler.handleMessage(inmsg);
            fail("Expected failure on an invalid Timestamp");
        } catch (SoapFault fault) {
            assertTrue(fault.getReason().contains("Invalid timestamp"));
            QName faultCode = new QName(WSConstants.WSSE_NS, "MessageExpired");
            assertTrue(fault.getFaultCode().equals(faultCode));
        }
    }
    
    /**
     * Test that an action mismatch gets mapped to a proper fault code 
     */
    @Test
    public void testActionMismatch() throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = getSoapMessageForDom(doc);

        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.TIMESTAMP);

        handler.handleMessage(msg);

        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();
        
        assertValid("//wsse:Security", doc);

        byte[] docbytes = getMessageBytes(doc);
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
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(WSHandlerConstants.ACTION, 
            WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.USERNAME_TOKEN);
        inHandler.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, TestPwdCallback.class.getName());

        inmsg.put(SecurityConstants.RETURN_SECURITY_ERROR, Boolean.TRUE);
        
        try {
            inHandler.handleMessage(inmsg);
            fail("Expected failure on an action mismatch");
        } catch (SoapFault fault) {
            assertTrue(fault.getReason().startsWith(
                "An error was discovered processing the <wsse:Security> header"));
            QName faultCode = new QName(WSConstants.WSSE_NS, "InvalidSecurity");
            assertTrue(fault.getFaultCode().equals(faultCode));
        }
    }
    
    // TODO - See CXF-6900.
    @Test
    @org.junit.Ignore
    public void testSignedEncryptedSOAP12Fault() throws Exception {
        Document doc = readDocument("wsse-response-fault.xml");

        SoapMessage msg = getSoapMessageForDom(doc);
        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();
        
        byte[] docbytes = getMessageBytes(doc);
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
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(WSHandlerConstants.ACTION, 
                              WSHandlerConstants.SIGNATURE + " "  + WSHandlerConstants.ENCRYPT);
        inHandler.setProperty(WSHandlerConstants.DEC_PROP_FILE, "insecurity.properties");
        inHandler.setProperty(WSHandlerConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        inHandler.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, TestPwdCallback.class.getName());
        inHandler.setProperty(
            WSHandlerConstants.PW_CALLBACK_CLASS, 
            "org.apache.cxf.ws.security.wss4j.TestPwdCallback"
        );
        
        inmsg.put(SecurityConstants.RETURN_SECURITY_ERROR, Boolean.TRUE);
        
        try {
            inHandler.handleMessage(inmsg);
            fail("Expected failure on a SOAP Fault");
        } catch (SoapFault fault) {
            fault.printStackTrace();
            // TODO assertTrue(fault.getReason().startsWith(
               // "An error was discovered processing the <wsse:Security> header"));
            QName faultCode = new QName(WSConstants.WSSE_NS, "InvalidSecurity");
            assertTrue(fault.getFaultCode().equals(faultCode));
        }
    }
    
}
