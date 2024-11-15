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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.crypto.dsig.DigestMethod;

import org.w3c.dom.Document;

import jakarta.activation.DataHandler;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPMessage;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.bus.managers.PhaseManagerImpl;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.EncryptionConstants;

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Ensures that the WSS4J with attachment is working as expected.
 */
public class WSS4JInOutWithAttachmentsTest extends AbstractSecurityTest {

    public WSS4JInOutWithAttachmentsTest() {
        // add xenc11 and dsig11 namespaces
        testUtilities.addNamespace("xenc11", EncryptionConstants.EncryptionSpec11NS);
        testUtilities.addNamespace("dsig11", Constants.SignatureSpec11NS);
    }

    /**
     * Test security headers for signing, encryption with ED25519 and X25519 keys. The test
     * assumes that the Java version is 17 or higher, because the ED25519 is supported from Java 16+ and   .
     * and the X25519 PKCS8 -parsing error is fixed from Java 12+ by java provided JCE.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testEncryptWithAgreementConcatKDFWithXECAndEDKeys() throws Exception {
        Assume.assumeTrue(getJDKVersion() >= 16);
        testEncryptWithAgreementMethod("ed25519", "x25519",
                WSS4JConstants.AGREEMENT_METHOD_X25519, WSS4JConstants.KEYDERIVATION_CONCATKDF);
    }

    @Test
    public void testEncryptWithAgreementConcatKDFWithECKeys() throws Exception {
        testEncryptWithAgreementMethod("secp256r1", "secp256r1",
                WSS4JConstants.AGREEMENT_METHOD_ECDH_ES, WSS4JConstants.KEYDERIVATION_CONCATKDF);
    }

    @Test
    public void testEncryptWithAgreementHKDFWithXECAndEDKeys() throws Exception {
        Assume.assumeTrue(getJDKVersion() >= 16);
        testEncryptWithAgreementMethod("ed25519", "x25519",
                WSS4JConstants.AGREEMENT_METHOD_X25519, WSS4JConstants.KEYDERIVATION_HKDF);
    }

    @Test
    public void testEncryptWithAgreementHKDFWithECKeys() throws Exception {
        testEncryptWithAgreementMethod("secp256r1", "secp256r1",
                WSS4JConstants.AGREEMENT_METHOD_ECDH_ES, WSS4JConstants.KEYDERIVATION_HKDF);
    }

    /**
     * Generic method for testing security headers of the SOAP with Attachment for signing and encryption with
     * agreement method using various key types configured in the keystore: wss-ecdh.properties.
     *
     * @param signAlias the alias of the signature key
     * @param encAlias the alias of the encryption key
     * @param keyAgreementMethod the key agreement method, e.g., ECDH_ES, X25519, X448
     * @param keyDerivationFunction the key derivation function, e.g., ConcatKDF, HKDF
     * @throws Exception if something goes wrong.
     */
    public void testEncryptWithAgreementMethod(String signAlias, String encAlias, String keyAgreementMethod,
                                               String keyDerivationFunction) throws Exception {

        Map<String, Object> outProperties = new HashMap<>();
        // Signature configuration (sign before encrypt)
        outProperties.put(ConfigurationConstants.ACTION,
                ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.ENCRYPTION);
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "wss-ecdh.properties");
        outProperties.put(ConfigurationConstants.USER, signAlias);
        outProperties.put(ConfigurationConstants.DERIVED_TOKEN_REFERENCE, signAlias);
        outProperties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put("password", "security");
        outProperties.put(ConfigurationConstants.SIGNATURE_PARTS, "{}cid:Attachments; "
                + "{Element}{" + WSSConstants.NS_SOAP12 + "}Body;"
                + "{Element}{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}Messaging;"
        );
        outProperties.put(ConfigurationConstants.SIG_DIGEST_ALGO, DigestMethod.SHA256);
        // ------------------------------------------
        // encryption configuration
        outProperties.put(ConfigurationConstants.ENC_PROP_FILE, "wss-ecdh.properties");
        outProperties.put(ConfigurationConstants.ENCRYPTION_USER, encAlias);
        outProperties.put(ConfigurationConstants.ENC_SYM_ALGO, WSS4JConstants.AES_256_GCM);
        outProperties.put(ConfigurationConstants.ENC_KEY_TRANSPORT, WSS4JConstants.KEYWRAP_AES128);
        outProperties.put(ConfigurationConstants.ENC_KEY_AGREEMENT_METHOD, WSS4JConstants.AGREEMENT_METHOD_ECDH_ES);
        outProperties.put(ConfigurationConstants.ENC_KEY_DERIVATION_FUNCTION, keyDerivationFunction);
        outProperties.put(ConfigurationConstants.ENC_KEY_ID, "DirectReference");
        outProperties.put(ConfigurationConstants.ENCRYPTION_PARTS, "{}cid:Attachments;");

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE
                + " " + ConfigurationConstants.ENCRYPTION);
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "wss-ecdh.properties");
        inProperties.put(ConfigurationConstants.USER, signAlias);

        // ------------------------------------------
        // encryption configuration
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "wss-ecdh.properties");
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());

        List<String> xpaths = new ArrayList<>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/xenc:EncryptedData");
        xpaths.add("//xenc:AgreementMethod");
        xpaths.add("//xenc11:KeyDerivationMethod");
        if (WSS4JConstants.KEYDERIVATION_HKDF.equals(keyDerivationFunction)) {
            xpaths.add("//rfc9231:HKDFParams");
            this.testUtilities.addNamespace("rfc9231", "http://www.w3.org/2021/04/xmldsig-more#");
        } else {
            xpaths.add("//xenc11:ConcatKDFParams");
        }
        xpaths.add("//wsse:Security/ds:Signature");

        SoapMessage inSoapMessage = makeInvocationWithAttachment(outProperties, xpaths, inProperties);
        assertNotNull(inSoapMessage);
    }

    /**
     *  Method builds and configures the WSS4j output bus with interceptors: AttachmentOutInterceptor,
     *  WSS4JOutInterceptor, StaxOutInterceptor, SAAJOutInterceptor and serialize output message with two attachment
     *  to bytearray. Then it validates existence of the xpaths in the soap header.
     *  Then it deserialize multipart message and decrypts attachments.
     *
     * @param outProperties - properties for out SoapMessage
     * @param xpaths - list of xpaths to validate
     * @param inProperties - properties for in WSS4JInInterceptor
     * @return inMsg - deserialized input message
     * @throws Exception  - if something goes wrong
     */
    protected SoapMessage makeInvocationWithAttachment(
            Map<String, Object> outProperties,
            List<String> xpaths,
            Map<String, Object> inProperties
    ) throws Exception {
        String attachmentContent1 = "Hello message: " + UUID.randomUUID();
        String attachmentContent2 = "The second Hello message:" + UUID.randomUUID();

        Document doc = readDocument("edeliver-as4-clean.xml");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Configure message
        SoapMessage outMsg = getSoapMessageForDom(doc, SOAPConstants.SOAP_1_2_PROTOCOL);
        outMsg.put(Message.CONTENT_TYPE, "multipart/related");
        // disable xop:Include where CipherData is serialized as payload in multipart/related
        outMsg.put(Message.MTOM_ENABLED, "false");
        outMsg.put(Message.ENCODING, StandardCharsets.UTF_8.name());
        outMsg.setContent(OutputStream.class, outputStream);

        // add attachments
        outMsg.setAttachments(new ArrayList<>());
        DataHandler dataHandler = new DataHandler(attachmentContent1, "text/plain");
        AttachmentImpl attachment001 = new AttachmentImpl("attachment_id_001", dataHandler);
        outMsg.getAttachments().add(attachment001);
        DataHandler dataHandler2 = new DataHandler(attachmentContent2, "text/plain");
        outMsg.getAttachments().add(new AttachmentImpl("attachment_id_002", dataHandler2));

        // add or overwrite properties
        for (String key : outProperties.keySet()) {
            outMsg.put(key, outProperties.get(key));
        }

        // Configure OUT message bus
        PhaseInterceptorChain outPhaseInterceptorChain = buildSimpleOutInterceptorChain();
        outMsg.setInterceptorChain(outPhaseInterceptorChain);

        // process message
        outPhaseInterceptorChain.doIntercept(outMsg);
        // xpath validation in output message (SOAPMessage)
        SOAPMessage soapMessage = outMsg.getContent(SOAPMessage.class);
        doc = soapMessage.getSOAPPart();
        for (String xpath : xpaths) {
            assertValid(xpath, doc);
        }
        // mime message bytes
        byte [] mimeMessageBytes = outputStream.toByteArray();
        // the output stream must not be empty
        assertTrue(mimeMessageBytes.length > 0);

        // build input message
        MessageImpl inMessage = new MessageImpl();
        SoapMessage inMsg = new SoapMessage(inMessage);
        inMsg.put(Message.CONTENT_TYPE, "multipart/related");
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inMsg);
        // set input multipart stream
        inMsg.setContent(InputStream.class, new ByteArrayInputStream(mimeMessageBytes));
        inMsg.setExchange(ex);

        // Configure IN message bus
        PhaseInterceptorChain inPhaseInterceptorChain = buildSimpleInInterceptorChain(inProperties);
        inMsg.setInterceptorChain(inPhaseInterceptorChain);
        // process message
        inPhaseInterceptorChain.doIntercept(inMsg);

        // validate in message
        Exception exc = inMsg.getContent(Exception.class);
        assertNull(exc);

        SOAPMessage inSoapMessage = inMsg.getContent(SOAPMessage.class);
        doc = inSoapMessage.getSOAPPart();
        assertNotNull(doc);
        // test attachments - processed in message must have 2 attachments with decrypted content
        assertNotNull(inSoapMessage.getAttachments());
        Iterator<Attachment> iteAtt = inMsg.getAttachments().iterator();
        assertEquals(2, inMsg.getAttachments().size());
        assertEquals(attachmentContent1, iteAtt.next().getDataHandler().getContent());
        assertEquals(attachmentContent2, iteAtt.next().getDataHandler().getContent());
        assertNotNull(inSoapMessage.getSOAPHeader());

        return inMsg;
    }

    /**
     * Method builds and configures the OUT PhaseInterceptorChain with all necessary interceptors to handle the
     * security headers for soap with attachments: AttachmentOutInterceptor, WSS4JOutInterceptor, StaxOutInterceptor,
     * SAAJOutInterceptor.
     * @return outPhaseInterceptorChain - configured out PhaseInterceptorChain
     */
    protected PhaseInterceptorChain buildSimpleOutInterceptorChain() {
        List<Interceptor<? extends Message>> outInterceptorList = new ArrayList<>();
        outInterceptorList.add(new AttachmentOutInterceptor());
        outInterceptorList.add(new WSS4JOutInterceptor());
        outInterceptorList.add(new StaxOutInterceptor());
        outInterceptorList.add(new SAAJOutInterceptor());
        PhaseManagerImpl pmOut = new PhaseManagerImpl();
        PhaseInterceptorChain outPhaseInterceptorChain = new PhaseInterceptorChain(pmOut.getOutPhases());
        outPhaseInterceptorChain.add(outInterceptorList);
        return outPhaseInterceptorChain;
    }

    /**
     * Method builds and configures the IN PhaseInterceptorChain with all necessary interceptors to handle the
     * security headers for soap with attachments: AttachmentInInterceptor, StaxInInterceptor, SAAJInInterceptor,
     * WSS4JInInterceptor.
     * @param inProperties - properties for in WSS4JInInterceptor
     * @return inPhaseInterceptorChain - configured in PhaseInterceptorChain
     */
    protected PhaseInterceptorChain buildSimpleInInterceptorChain(Map<String, Object> inProperties) {
        List<Interceptor<? extends Message>> inInterceptorList = new ArrayList<>();
        inInterceptorList.add(new AttachmentInInterceptor());
        inInterceptorList.add(new StaxInInterceptor());
        inInterceptorList.add(new SAAJInInterceptor());
        inInterceptorList.add(new WSS4JInInterceptor(inProperties));

        PhaseManagerImpl pmIn = new PhaseManagerImpl();
        PhaseInterceptorChain inPhaseInterceptorChain = new PhaseInterceptorChain(pmIn.getInPhases());
        inPhaseInterceptorChain.add(inInterceptorList);
        return inPhaseInterceptorChain;
    }

}
