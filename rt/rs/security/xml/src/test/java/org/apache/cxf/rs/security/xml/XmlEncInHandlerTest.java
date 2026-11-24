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
package org.apache.cxf.rs.security.xml;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Properties;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.ws.rs.WebApplicationException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.xml.security.encryption.XMLCipher;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Every failure that depends on the encrypted content must produce the same error for the client.
 */
public class XmlEncInHandlerTest {

    private static final String DECRYPTION_FAILED = "Payload can not be decrypted";

    private static Crypto crypto;
    private static X509Certificate cert;

    @BeforeClass
    public static void setUpCrypto() throws Exception {
        org.apache.xml.security.Init.init();
        Properties props = new Properties();
        props.put("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");
        props.put("org.apache.wss4j.crypto.merlin.keystore.type", "jks");
        props.put("org.apache.wss4j.crypto.merlin.keystore.password", "password");
        props.put("org.apache.wss4j.crypto.merlin.keystore.alias", "alice");
        props.put("org.apache.wss4j.crypto.merlin.keystore.file", "alice.jks");
        crypto = CryptoFactory.getInstance(props);
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("alice");
        cert = crypto.getX509Certificates(cryptoType)[0];
    }

    @Test
    public void testPayloadDecryptionFailure() {
        AbstractXmlEncInHandler handler = new AbstractXmlEncInHandler() {
            @Override
            protected byte[] getSymmetricKeyBytes(Message message, Element encDataElement) {
                return new byte[16];
            }
        };
        assertDecryptionFailed(() -> handler.decryptContent(createMessage()));
    }

    @Test
    public void testDecryptedPayloadParseFailure() {
        AbstractXmlEncInHandler handler = new AbstractXmlEncInHandler() {
            @Override
            protected byte[] getSymmetricKeyBytes(Message message, Element encDataElement) {
                return new byte[16];
            }

            @Override
            protected byte[] decryptPayload(Element root, byte[] secretKeyBytes, String symEncAlgo) {
                return "<not-xml".getBytes(StandardCharsets.UTF_8);
            }
        };
        assertDecryptionFailed(() -> handler.decryptContent(createMessage()));
    }

    @Test
    public void testEncryptedKeyBase64Failure() {
        assertDecryptionFailed(() -> decryptSymmetricKey("!!! not base64 !!!"));
    }

    @Test
    public void testEncryptedKeyUnwrapFailure() {
        String garbage = Base64.getEncoder().encodeToString(new byte[256]);
        assertDecryptionFailed(() -> decryptSymmetricKey(garbage));
    }

    private static void decryptSymmetricKey(String base64EncodedKey) throws Exception {
        AbstractXmlEncInHandler handler = new AbstractXmlEncInHandler() { };
        Message message = createMessage();
        CallbackHandler callback = (Callback[] callbacks) -> {
            for (Callback c : callbacks) {
                ((WSPasswordCallback)c).setPassword("password");
            }
        };
        message.put(SecurityConstants.CALLBACK_HANDLER, callback);
        handler.decryptSymmetricKey(base64EncodedKey, cert, crypto, XMLCipher.RSA_OAEP, null, message);
    }

    private static void assertDecryptionFailed(ThrowingRunnable runnable) {
        try {
            runnable.run();
            fail("Expected the request to be rejected");
        } catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
            assertEquals(DECRYPTION_FAILED, ex.getResponse().getEntity());
        } catch (Exception ex) {
            throw new AssertionError("Unexpected exception", ex);
        }
    }

    private static Message createMessage() {
        Document doc = DOMUtils.createDocument();
        Element encData = doc.createElementNS(AbstractXmlSecInHandler.ENC_NS, "xenc:EncryptedData");
        Element encMethod = doc.createElementNS(AbstractXmlSecInHandler.ENC_NS, "xenc:EncryptionMethod");
        encMethod.setAttributeNS(null, "Algorithm", XMLCipher.AES_128);
        encData.appendChild(encMethod);
        Element cipherData = doc.createElementNS(AbstractXmlSecInHandler.ENC_NS, "xenc:CipherData");
        Element cipherValue = doc.createElementNS(AbstractXmlSecInHandler.ENC_NS, "xenc:CipherValue");
        cipherValue.setTextContent(Base64.getEncoder().encodeToString(new byte[48]));
        cipherData.appendChild(cipherValue);
        encData.appendChild(cipherData);
        doc.appendChild(encData);

        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.HTTP_REQUEST_METHOD, "POST");
        message.setContent(XMLStreamReader.class, new W3CDOMStreamReader(doc.getDocumentElement()));
        return message;
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
