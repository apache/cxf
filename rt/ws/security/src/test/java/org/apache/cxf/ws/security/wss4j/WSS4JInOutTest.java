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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.xml.namespace.QName;
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
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
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
    
    @Test
    @SuppressWarnings("unchecked")
    public void testEncryption() throws Exception {
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
        
        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        msg.put(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        msg.put(WSHandlerConstants.ENC_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "myalias");
        msg.put("password", "myAliasPassword");

        handler.handleMessage(msg);
        doc = part;

        assertValid("//wsse:Security", doc);
        assertValid("//s:Body/xenc:EncryptedData", doc);

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

        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        inHandler.setProperty(WSHandlerConstants.DEC_PROP_FILE, "META-INF/cxf/insecurity.properties");
        inHandler.setProperty(
            WSHandlerConstants.PW_CALLBACK_CLASS, 
            "org.apache.cxf.ws.security.wss4j.TestPwdCallback"
        );

        inHandler.handleMessage(inmsg);
        //
        // Check that the EncryptedData is no longer there
        //
        assertInvalid("//s:Body/xenc:EncryptedData", saajMsg.getSOAPPart());
        //
        // There should be exactly 1 (WSS4J) HandlerResult
        //
        final java.util.List<WSHandlerResult> handlerResults = 
            (java.util.List<WSHandlerResult>) inmsg.get(WSHandlerConstants.RECV_RESULTS);
        assertNotNull(handlerResults);
        assertSame(handlerResults.size(), 1);
        //
        // This should contain exactly 1 protection result
        //
        final java.util.List<Object> protectionResults =
            (java.util.List<Object>) handlerResults.get(0).getResults();
        assertNotNull(protectionResults);
        assertSame(protectionResults.size(), 1);
        //
        // This result should contain a reference to the decrypted element,
        // which should contain the soap:Body Qname
        //
        final java.util.Map<String, Object> result =
            (java.util.Map<String, Object>) protectionResults.get(0);
        final java.util.List<WSDataRef> protectedElements =
            (java.util.List<WSDataRef>) 
                result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS);
        assertNotNull(protectedElements);
        assertSame(protectedElements.size(), 1);
        assertEquals(
            protectedElements.get(0).getName(),
            new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/envelope/",
                "Body"
            )
        );
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testEncryptedUsernameToken() throws Exception {
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
        
        msg.put(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.USERNAME_TOKEN + " " + WSHandlerConstants.ENCRYPT
        );
        msg.put(WSHandlerConstants.ENC_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "alice");
        msg.put("password", "alicePassword");
        msg.put(WSHandlerConstants.ENCRYPTION_USER, "myalias");
        msg.put(
            WSHandlerConstants.ENCRYPTION_PARTS, 
            "{Content}{" + WSConstants.WSSE_NS + "}UsernameToken"
        );

        handler.handleMessage(msg);
        doc = part;

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
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.USERNAME_TOKEN + " " + WSHandlerConstants.ENCRYPT
        );
        inHandler.setProperty(WSHandlerConstants.DEC_PROP_FILE, "META-INF/cxf/insecurity.properties");
        inHandler.setProperty(
            WSHandlerConstants.PW_CALLBACK_CLASS, 
            "org.apache.cxf.ws.security.wss4j.TestPwdCallback"
        );

        inHandler.handleMessage(inmsg);
        //
        // Check that the EncryptedData is no longer there
        //
        assertInvalid("//s:Body/xenc:EncryptedData", saajMsg.getSOAPPart());
        //
        // There should be exactly 1 (WSS4J) HandlerResult
        //
        final java.util.List<WSHandlerResult> handlerResults = 
            (java.util.List<WSHandlerResult>) inmsg.get(WSHandlerConstants.RECV_RESULTS);
        assertNotNull(handlerResults);
        assertSame(handlerResults.size(), 1);
        
        //
        // This should contain exactly 2 protection results
        //
        final java.util.List<Object> protectionResults =
            (java.util.List<Object>) handlerResults.get(0).getResults();
        assertNotNull(protectionResults);
        assertSame(protectionResults.size(), 2);
    }
    
    @Test
    public void testCustomProcessor() throws Exception {
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

        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(
            WSS4JInInterceptor.PROCESSOR_MAP,
            createCustomProcessorMap()
        );
        WSS4JInInterceptor inHandler = new WSS4JInInterceptor(properties);

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.NO_SECURITY);

        inHandler.handleMessage(inmsg);
        
        WSSecurityEngineResult result = 
            (WSSecurityEngineResult) inmsg.get(WSS4JInInterceptor.SIGNATURE_RESULT);
        assertNull(result);
    }
    
    
    @Test
    public void testCustomProcessorObject() throws Exception {
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

        final Map<String, Object> properties = new HashMap<String, Object>();
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        customMap.put(
            new QName(
                WSConstants.SIG_NS,
                WSConstants.SIG_LN
            ),
            new CustomProcessor()
        );
        properties.put(
            WSS4JInInterceptor.PROCESSOR_MAP,
            customMap
        );
        WSS4JInInterceptor inHandler = new WSS4JInInterceptor(properties);

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);

        inHandler.handleMessage(inmsg);
        
        WSSecurityEngineResult result = 
            (WSSecurityEngineResult) inmsg.get(WSS4JInInterceptor.SIGNATURE_RESULT);
        assertNotNull(result);
        
        Object obj = result.get("foo");
        assertNotNull(obj);
        assertEquals(obj.getClass().getName(), CustomProcessor.class.getName());
    }
    
    private byte[] getMessageBytes(Document doc) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLStreamWriter byteArrayWriter = StaxUtils.createXMLStreamWriter(outputStream);
        StaxUtils.writeDocument(doc, byteArrayWriter, false);
        byteArrayWriter.flush();
        return outputStream.toByteArray();
    }

    /**
     * @return      a processor map suitable for custom processing of
     *              signatures (in this case, the actual processor is
     *              null, which will cause the WSS4J runtime to do no
     *              processing on the input)
     */
    private Map<QName, String>
    createCustomProcessorMap() {
        final Map<QName, String> ret = new HashMap<QName, String>();
        ret.put(
            new QName(
                WSConstants.SIG_NS,
                WSConstants.SIG_LN
            ),
            null
        );
        return ret;
    }
    
    
    // FOR DEBUGGING ONLY
    /*private*/ static String serialize(Document doc) {
        return XMLUtils.toString(doc);
    }
}
