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

import org.w3c.dom.Document;

import jakarta.xml.soap.SOAPMessage;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.helpers.CastUtils;
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
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.EncryptionConstants;

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Ensures that the signature round trip process works.
 */
public class WSS4JInOutTest extends AbstractSecurityTest {

    public WSS4JInOutTest() {
        // add xenc11 and dsig11 namespaces
        testUtilities.addNamespace("xenc11", EncryptionConstants.EncryptionSpec11NS);
        testUtilities.addNamespace("dsig11", Constants.SignatureSpec11NS);
    }

    @Test
    public void testOrder() throws Exception {
        //make sure the interceptors get ordered correctly
        SortedSet<Phase> phases = new TreeSet<>();
        phases.add(new Phase(Phase.PRE_PROTOCOL, 1));

        List<Interceptor<? extends Message>> lst = new ArrayList<>();
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
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        outProperties.put(ConfigurationConstants.USER, "myalias");
        outProperties.put("password", "myAliasPassword");

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");

        List<String> xpaths = new ArrayList<>();
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
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        outProperties.put(ConfigurationConstants.USER, "myalias");
        outProperties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put("password", "myAliasPassword");

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");

        List<String> xpaths = new ArrayList<>();
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
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        outProperties.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        outProperties.put(ConfigurationConstants.USER, "myalias");
        outProperties.put("password", "myAliasPassword");

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());

        List<String> xpaths = new ArrayList<>();
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
    public void testEncryptionWithAgreementMethodsX448() throws Exception {
        Assume.assumeTrue(getJDKVersion() >= 16);
        testEncryptionWithAgreementMethod("x448", "//dsig11:DEREncodedKeyValue");
    }

    @Test
    public void testEncryptionWithAgreementMethodsX25519() throws Exception {
        Assume.assumeTrue(getJDKVersion() >= 16);
        testEncryptionWithAgreementMethod("x25519", "//dsig11:DEREncodedKeyValue");
    }

    @Test
    public void testEncryptionWithAgreementMethodsECP256r1() throws Exception {
        testEncryptionWithAgreementMethod("secp256r1", "//dsig11:ECKeyValue");
    }

    @Test
    public void testEncryptionWithAgreementMethodsECP521r1() throws Exception {
        testEncryptionWithAgreementMethod("secp521r1", "//dsig11:ECKeyValue");
    }

    /**
     * Helper method to Test encryption using the specified agreement method with various keys
     */
    public void testEncryptionWithAgreementMethod(String alias, String keyElement) throws Exception {

        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        outProperties.put(ConfigurationConstants.ENC_PROP_FILE, "wss-ecdh.properties");
        outProperties.put(ConfigurationConstants.USER, alias);
        outProperties.put(ConfigurationConstants.ENC_KEY_TRANSPORT, WSS4JConstants.KEYWRAP_AES128);
        outProperties.put(ConfigurationConstants.ENC_KEY_AGREEMENT_METHOD, WSS4JConstants.AGREEMENT_METHOD_ECDH_ES);
        outProperties.put(ConfigurationConstants.ENC_KEY_DERIVATION_FUNCTION, WSS4JConstants.KEYDERIVATION_CONCATKDF);

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "wss-ecdh.properties");
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());
        // assertion of existence of elements
        List<String> xpaths = new ArrayList<>();
        xpaths.add(keyElement);
        xpaths.add("//wsse:Security");
        xpaths.add("//s:Body/xenc:EncryptedData");
        xpaths.add("//xenc:AgreementMethod");
        xpaths.add("//xenc11:KeyDerivationMethod");
        xpaths.add("//xenc11:ConcatKDFParams");
        xpaths.add("//xenc:OriginatorKeyInfo");
        xpaths.add("//xenc:RecipientKeyInfo");

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
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.USERNAME_TOKEN + " " + ConfigurationConstants.ENCRYPTION
        );
        outProperties.put(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        outProperties.put(ConfigurationConstants.USER, "alice");
        outProperties.put("password", "alicePassword");
        outProperties.put(ConfigurationConstants.ENCRYPTION_USER, "myalias");
        outProperties.put(
            ConfigurationConstants.ENCRYPTION_PARTS,
            "{Content}{" + WSS4JConstants.WSSE_NS + "}UsernameToken"
        );

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.USERNAME_TOKEN + " " + ConfigurationConstants.ENCRYPTION
        );
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());

        List<String> xpaths = new ArrayList<>();
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
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        outProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_TEXT);
        outProperties.put(ConfigurationConstants.USER, "alice");
        outProperties.put("password", "alicePassword");

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        inProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_DIGEST);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());

        List<String> xpaths = new ArrayList<>();
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

        msg.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        msg.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        msg.put(ConfigurationConstants.USER, "myalias");
        msg.put("password", "myAliasPassword");

        handler.handleMessage(msg);

        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();

        assertValid("//wsse:Security", doc);
        assertValid("//wsse:Security/ds:Signature", doc);

        byte[] docbytes = getMessageBytes(doc);
        StaxUtils.read(new ByteArrayInputStream(docbytes));

        final Map<String, Object> properties = new HashMap<>();
        properties.put(
            WSS4JInInterceptor.PROCESSOR_MAP,
            createCustomProcessorMap()
        );
        WSS4JInInterceptor inHandler = new WSS4JInInterceptor(properties);

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(ConfigurationConstants.ACTION, WSHandlerConstants.NO_SECURITY);

        inHandler.handleMessage(inmsg);

        List<WSHandlerResult> results = getResults(inmsg);
        assertTrue(results != null && results.size() == 1);
        List<WSSecurityEngineResult> signatureResults =
            results.get(0).getActionResults().get(WSConstants.SIGN);
        assertTrue(signatureResults == null || signatureResults.isEmpty());
    }

    @Test
    public void testCustomProcessorObject() throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = getSoapMessageForDom(doc);

        msg.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        msg.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        msg.put(ConfigurationConstants.USER, "myalias");
        msg.put("password", "myAliasPassword");

        handler.handleMessage(msg);

        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();

        assertValid("//wsse:Security", doc);
        assertValid("//wsse:Security/ds:Signature", doc);

        byte[] docbytes = getMessageBytes(doc);
        StaxUtils.read(new ByteArrayInputStream(docbytes));

        final Map<String, Object> properties = new HashMap<>();
        final Map<QName, Object> customMap = new HashMap<>();
        customMap.put(
            new QName(
                WSS4JConstants.SIG_NS,
                WSS4JConstants.SIG_LN
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

        inHandler.setProperty(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);

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
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        outProperties.put(ConfigurationConstants.USER, "alice");
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        outProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        outProperties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(ConfigurationConstants.USE_SINGLE_CERTIFICATE, "false");

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "cxfca.properties");

        List<String> xpaths = new ArrayList<>();
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
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.USERNAME_TOKEN + " " + ConfigurationConstants.SIGNATURE);
        outProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_TEXT);
        outProperties.put(ConfigurationConstants.USER, "alice");

        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        outProperties.put(ConfigurationConstants.SIGNATURE_USER, "myalias");
        outProperties.put(
            ConfigurationConstants.PW_CALLBACK_CLASS,
            "org.apache.cxf.ws.security.wss4j.TestPwdCallback"
        );

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.USERNAME_TOKEN + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_TEXT);
        inProperties.put(
            ConfigurationConstants.PW_CALLBACK_CLASS,
            "org.apache.cxf.ws.security.wss4j.TestPwdCallback"
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");

        List<String> xpaths = new ArrayList<>();
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
        final Map<QName, String> ret = new HashMap<>();
        ret.put(
            new QName(
                WSS4JConstants.SIG_NS,
                WSS4JConstants.SIG_LN
            ),
            null
        );
        return ret;
    }

    private List<WSHandlerResult> getResults(SoapMessage inmsg) {
        return CastUtils.cast((List<?>)inmsg.get(WSHandlerConstants.RECV_RESULTS));
    }

    // FOR DEBUGGING ONLY
    /*private*/ static String serialize(Document doc) {
        return StaxUtils.toString(doc);
    }
}
