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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.ws.rs.WebApplicationException;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.xml.security.encryption.XMLCipher;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class XmlEncInHandlerTest {

    private static final String ENC_NS = "http://www.w3.org/2001/04/xmlenc#";

    private static Crypto crypto;

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
    }

    @Test
    public void testDefaultEncryption() throws Exception {
        Document doc = encrypt(null);
        Element encryptionMethod =
            DOMUtils.getFirstChildWithName(doc.getDocumentElement(), ENC_NS, "EncryptionMethod");
        assertEquals(XMLCipher.AES_256_GCM, encryptionMethod.getAttributeNS(null, "Algorithm"));

        assertEquals("CXF", decrypt(doc, null).getTextContent());
    }

    @Test
    public void testCbcEncryptionIsRejectedByDefault() throws Exception {
        try {
            decrypt(encrypt(XMLCipher.AES_256), null);
            fail("Failure expected on AES-CBC content encryption");
        } catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testConfiguredCbcEncryption() throws Exception {
        assertEquals("CXF", decrypt(encrypt(XMLCipher.AES_256), cbcProperties()).getTextContent());
    }

    @Test
    public void testConfiguredCbcEncryptionErrorsDoNotRevealPadding() throws Exception {
        Document doc = encrypt(XMLCipher.AES_256);
        Element cipherData = DOMUtils.getFirstChildWithName(doc.getDocumentElement(), ENC_NS, "CipherData");
        Element cipherValue = DOMUtils.getFirstChildWithName(cipherData, ENC_NS, "CipherValue");
        // keep the IV and the first block, then try every value of the last IV byte: the
        // padding of the decrypted block is only valid for some of them
        byte[] ivAndBlock = Arrays.copyOf(Base64Utility.decode(cipherValue.getTextContent().trim()), 32);

        Set<Object> errors = new HashSet<>();
        for (int i = 0; i < 256; i++) {
            ivAndBlock[15] = (byte)i;
            cipherValue.setTextContent(Base64Utility.encode(ivAndBlock));
            try {
                decrypt(doc, cbcProperties());
                fail("Failure expected on a modified ciphertext");
            } catch (WebApplicationException ex) {
                assertEquals(400, ex.getResponse().getStatus());
                errors.add(ex.getResponse().getEntity());
            }
        }
        assertEquals(Collections.singleton("Payload can not be decrypted"), errors);
    }

    private static EncryptionProperties cbcProperties() {
        EncryptionProperties encProps = new EncryptionProperties();
        encProps.setEncryptionSymmetricKeyAlgo(XMLCipher.AES_256);
        return encProps;
    }

    private static Document encrypt(String symEncAlgo) throws Exception {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(SecurityConstants.ENCRYPT_CRYPTO, crypto);
        message.put(SecurityConstants.ENCRYPT_USERNAME, "alice");

        Document doc = DOMUtils.createDocument();
        Element book = doc.createElementNS(null, "Book");
        Element name = doc.createElementNS(null, "name");
        name.setTextContent("CXF");
        book.appendChild(name);
        doc.appendChild(book);

        XmlEncOutInterceptor interceptor = new XmlEncOutInterceptor();
        if (symEncAlgo != null) {
            interceptor.setSymmetricEncAlgorithm(symEncAlgo);
        }
        return interceptor.encryptDocument(message, doc);
    }

    private static Element decrypt(Document doc, EncryptionProperties encProps) throws Exception {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(SecurityConstants.ENCRYPT_CRYPTO, crypto);
        message.put(SecurityConstants.CALLBACK_HANDLER, (CallbackHandler)callbacks -> {
            for (Callback callback : callbacks) {
                ((WSPasswordCallback)callback).setPassword("password");
            }
        });
        byte[] bytes = StaxUtils.toString(doc).getBytes(StandardCharsets.UTF_8);
        message.setContent(InputStream.class, new ByteArrayInputStream(bytes));

        XmlEncInHandler handler = new XmlEncInHandler();
        handler.setEncryptionProperties(encProps);
        handler.decryptContent(message);
        return StaxUtils.read(message.getContent(XMLStreamReader.class)).getDocumentElement();
    }
}
