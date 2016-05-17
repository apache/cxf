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
import java.security.Principal;
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
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils.NullResolver;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
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
        
        List<Interceptor<? extends Message>> lst = new ArrayList<Interceptor<? extends Message>>();
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
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        outProperties.put(WSHandlerConstants.SIG_PROP_FILE, "outsecurity.properties");
        outProperties.put(WSHandlerConstants.USER, "myalias");
        outProperties.put("password", "myAliasPassword");
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        inProperties.put(WSHandlerConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/ds:Signature");

        List<WSHandlerResult> handlerResults = 
            getResults(makeInvocation(outProperties, xpaths, inProperties));
        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.SIGN).get(0);
         
        X509Certificate certificate = 
            (X509Certificate) actionResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        assertNotNull(certificate);
    }
    
    @Test
    public void testDirectReferenceSignature() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        outProperties.put(WSHandlerConstants.SIG_PROP_FILE, "outsecurity.properties");
        outProperties.put(WSHandlerConstants.USER, "myalias");
        outProperties.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put("password", "myAliasPassword");
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        inProperties.put(WSHandlerConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/wsse:BinarySecurityToken");
        xpaths.add("//wsse:Security/ds:Signature");

        List<WSHandlerResult> handlerResults = 
            getResults(makeInvocation(outProperties, xpaths, inProperties));
        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.SIGN).get(0);
         
        X509Certificate certificate = 
            (X509Certificate) actionResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        assertNotNull(certificate);
    }
    
    @Test
    public void testEncryption() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        outProperties.put(WSHandlerConstants.ENC_PROP_FILE, "outsecurity.properties");
        outProperties.put(WSHandlerConstants.USER, "myalias");
        outProperties.put("password", "myAliasPassword");
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        inProperties.put(WSHandlerConstants.DEC_PROP_FILE, "insecurity.properties");
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback()); 
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//s:Body/xenc:EncryptedData");

        List<WSHandlerResult> handlerResults = 
            getResults(makeInvocation(outProperties, xpaths, inProperties));

        assertNotNull(handlerResults);
        assertSame(handlerResults.size(), 1);
        //
        // This should contain exactly 1 protection result
        //
        final java.util.List<WSSecurityEngineResult> protectionResults =
            handlerResults.get(0).getResults();
        assertNotNull(protectionResults);
        assertSame(protectionResults.size(), 1);
        //
        // This result should contain a reference to the decrypted element,
        // which should contain the soap:Body Qname
        //
        final java.util.Map<String, Object> result =
            protectionResults.get(0);
        final java.util.List<WSDataRef> protectedElements =
            CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
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
    public void testEncryptedUsernameToken() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(
            WSHandlerConstants.ACTION,
            WSHandlerConstants.USERNAME_TOKEN + " " + WSHandlerConstants.ENCRYPT
        );
        outProperties.put(WSHandlerConstants.ENC_PROP_FILE, "outsecurity.properties");
        outProperties.put(WSHandlerConstants.USER, "alice");
        outProperties.put("password", "alicePassword");
        outProperties.put(WSHandlerConstants.ENCRYPTION_USER, "myalias");
        outProperties.put(
            WSHandlerConstants.ENCRYPTION_PARTS, 
            "{Content}{" + WSConstants.WSSE_NS + "}UsernameToken"
        );
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.USERNAME_TOKEN + " " + WSHandlerConstants.ENCRYPT
        );
        inProperties.put(WSHandlerConstants.DEC_PROP_FILE, "insecurity.properties");
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback()); 
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");

        SoapMessage inmsg = makeInvocation(outProperties, xpaths, inProperties);
        List<WSHandlerResult> handlerResults = getResults(inmsg);

        assertNotNull(handlerResults);
        assertSame(handlerResults.size(), 1);
        
        //
        // This should contain exactly 2 protection results
        //
        final java.util.List<WSSecurityEngineResult> protectionResults =
            handlerResults.get(0).getResults();
        assertNotNull(protectionResults);
        assertSame(protectionResults.size(), 2);
        
        final Principal p1 = (Principal)protectionResults.get(0).get(WSSecurityEngineResult.TAG_PRINCIPAL);
        final Principal p2 = (Principal)protectionResults.get(1).get(WSSecurityEngineResult.TAG_PRINCIPAL);
        assertTrue(p1 instanceof UsernameTokenPrincipal || p2 instanceof UsernameTokenPrincipal);
        
        Principal utPrincipal = p1 instanceof UsernameTokenPrincipal ? p1 : p2;
        
        SecurityContext securityContext = inmsg.get(SecurityContext.class);
        assertNotNull(securityContext);
        assertSame(securityContext.getUserPrincipal(), utPrincipal);
    }
    
    @Test
    public void testUsernameToken() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        outProperties.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        outProperties.put(WSHandlerConstants.USER, "alice");
        outProperties.put("password", "alicePassword");
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        inProperties.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback()); 
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");

        //
        // This should fail, as we are requiring a digest password type
        //
        try {
            makeInvocation(outProperties, xpaths, inProperties);
            fail("Failure expected on the wrong password type");
        } catch (org.apache.cxf.interceptor.Fault fault) {
            // expected
        }
    }
    
    @Test
    public void testCustomProcessor() throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = getSoapMessageForDom(doc);

        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        msg.put(WSHandlerConstants.SIG_PROP_FILE, "outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "myalias");
        msg.put("password", "myAliasPassword");

        handler.handleMessage(msg);

        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();
        
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
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.NO_SECURITY);

        inHandler.handleMessage(inmsg);
        
        List<WSHandlerResult> results = getResults(inmsg);
        assertTrue(results != null && results.size() == 1);
        List<WSSecurityEngineResult> signatureResults = 
            results.get(0).getActionResults().get(WSConstants.SIGN);
        assertTrue(signatureResults == null || signatureResults.size() == 0);
    }
    
    @Test
    public void testCustomProcessorObject() throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = getSoapMessageForDom(doc);

        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        msg.put(WSHandlerConstants.SIG_PROP_FILE, "outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "myalias");
        msg.put("password", "myAliasPassword");

        handler.handleMessage(msg);

        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();
        
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
            CustomProcessor.class
        );
        properties.put(
            WSS4JInInterceptor.PROCESSOR_MAP,
            customMap
        );
        WSS4JInInterceptor inHandler = new WSS4JInInterceptor(properties);

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);
        
        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);

        inHandler.handleMessage(inmsg);
        
        List<WSHandlerResult> results = getResults(inmsg);
        assertTrue(results != null && results.size() == 1);
        List<WSSecurityEngineResult> signatureResults = 
            results.get(0).getActionResults().get(WSConstants.SIGN);
        assertTrue(signatureResults.size() == 1);
        
        Object obj = signatureResults.get(0).get("foo");
        assertNotNull(obj);
        assertEquals(obj.getClass().getName(), CustomProcessor.class.getName());
    }
    
    @Test
    public void testPKIPath() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        outProperties.put(WSHandlerConstants.USER, "alice");
        outProperties.put(WSHandlerConstants.SIG_PROP_FILE, "alice.properties");
        outProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        outProperties.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(WSHandlerConstants.USE_SINGLE_CERTIFICATE, "false");
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        inProperties.put(WSHandlerConstants.SIG_VER_PROP_FILE, "cxfca.properties");
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/ds:Signature");

        List<WSHandlerResult> handlerResults = 
            getResults(makeInvocation(outProperties, xpaths, inProperties));
        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.SIGN).get(0);
         
        X509Certificate[] certificates = 
            (X509Certificate[]) actionResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATES);
        assertNotNull(certificates);
        assertEquals(certificates.length, 2);
    }
    
    @Test
    public void testUsernameTokenSignature() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.USERNAME_TOKEN + " " + WSHandlerConstants.SIGNATURE);
        outProperties.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        outProperties.put(WSHandlerConstants.USER, "alice");
        
        outProperties.put(WSHandlerConstants.SIG_PROP_FILE, "outsecurity.properties");
        outProperties.put(WSHandlerConstants.SIGNATURE_USER, "myalias");
        outProperties.put(
            WSHandlerConstants.PW_CALLBACK_CLASS, 
            "org.apache.cxf.ws.security.wss4j.TestPwdCallback"
        );
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.USERNAME_TOKEN + " " + WSHandlerConstants.SIGNATURE
        );
        inProperties.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        inProperties.put(
            WSHandlerConstants.PW_CALLBACK_CLASS, 
            "org.apache.cxf.ws.security.wss4j.TestPwdCallback"
        );
        inProperties.put(WSHandlerConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/ds:Signature");
        xpaths.add("//wsse:Security/wsse:UsernameToken");

        makeInvocation(outProperties, xpaths, inProperties);
    }
    
    /**
     * @return      a processor map suitable for custom processing of
     *              signatures (in this case, the actual processor is
     *              null, which will cause the WSS4J runtime to do no
     *              processing on the input)
     */
    private Map<QName, String> createCustomProcessorMap() {
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
    
    private List<WSHandlerResult> getResults(SoapMessage inmsg) {
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)inmsg.get(WSHandlerConstants.RECV_RESULTS));
        return handlerResults;
    }
    
    // FOR DEBUGGING ONLY
    /*private*/ static String serialize(Document doc) {
        return StaxUtils.toString(doc);
    }
}
