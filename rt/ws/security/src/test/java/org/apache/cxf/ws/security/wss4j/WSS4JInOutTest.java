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
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

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
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.helpers.DOMUtils.NullResolver;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.junit.Test;


/**
 * Ensures that the signature round trip process works.
 */
public class WSS4JInOutTest extends AbstractSecurityTest {

    public WSS4JInOutTest() {
    }

    @Test
    public void testOrder() throws Exception {
        //make sure the interceptors get ordered correctly
        SortedSet<Phase> phases = new TreeSet<Phase>();
        phases.add(new Phase(Phase.PRE_PROTOCOL, 1));
        
        List<Interceptor> lst = new ArrayList<Interceptor>();
        lst.add(new MustUnderstandInterceptor());
        lst.add(new WSS4JInInterceptor());
        lst.add(new SAAJInInterceptor());
        PhaseInterceptorChain chain = new PhaseInterceptorChain(phases);
        chain.add(lst);
        String output = chain.toString();
        assertTrue(output.contains("MustUnderstandInterceptor, SAAJInInterceptor, WSS4JInInterceptor"));
    }
    
    @Test
    public void testSignature() throws Exception {
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

        handler.handleMessage(msg);

        doc = part;
        
        assertValid("//wsse:Security", doc);
        assertValid("//wsse:Security/ds:Signature", doc);

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
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        inHandler.setProperty(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/insecurity.properties");

        inHandler.handleMessage(inmsg);
        
        WSSecurityEngineResult result = 
            (WSSecurityEngineResult) inmsg.get(WSS4JInInterceptor.SIGNATURE_RESULT);
        assertNotNull(result);
        X509Certificate certificate = (X509Certificate)result
            .get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        assertNotNull(certificate);
    }
    
    @Test
    public void testDirectReferenceSignature() throws Exception {
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
        msg.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
        msg.put("password", "myAliasPassword");

        handler.handleMessage(msg);

        doc = part;
        
        assertValid("//wsse:Security", doc);
        // Check to see that the binary security token was inserted in the header
        assertValid("//wsse:Security/wsse:BinarySecurityToken", doc);
        assertValid("//wsse:Security/ds:Signature", doc);

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
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        inHandler.setProperty(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/insecurity.properties");

        inHandler.handleMessage(inmsg);
        
        WSSecurityEngineResult result = 
            (WSSecurityEngineResult) inmsg.get(WSS4JInInterceptor.SIGNATURE_RESULT);
        assertNotNull(result);
        X509Certificate certificate = (X509Certificate)result
            .get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        assertNotNull(certificate);
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
