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
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ListIterator;
import java.util.Properties;

import javax.ws.rs.WebApplicationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.bus.managers.PhaseManagerImpl;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class XmlSecInInterceptorTest {

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
    public void testEnvelopedSignature() throws Exception {
        Element root = process(createSignedDocument());

        assertEquals("Book", root.getLocalName());
        assertEquals("CXF", DOMUtils.getFirstElement(root).getTextContent());
    }

    @Test
    public void testWrappedEnvelopedSignatureIsRejected() throws Exception {
        Document signedDoc = createSignedDocument();
        Element signedRoot = signedDoc.getDocumentElement();
        Element signature =
            DOMUtils.getFirstChildWithName(signedRoot, Constants.SignatureSpecNS, "Signature");

        // Wrap the signed element in a new unsigned root and move the Signature
        // to be a direct child of that root
        Document doc = DOMUtils.createDocument();
        Element root = createBook(doc, "evil");
        doc.appendChild(root);
        signedRoot.removeChild(signature);
        root.appendChild(doc.importNode(signedRoot, true));
        root.appendChild(doc.importNode(signature, true));

        assertRejected(doc);
    }

    @Test
    public void testDetachedSignatureIsRejected() throws Exception {
        Document doc = createDetachedSignedDocument();

        // Add unsigned content to the unsigned root, before the signed element
        Element root = doc.getDocumentElement();
        root.insertBefore(createBook(doc, "evil"), root.getFirstChild());

        assertRejected(doc);
    }

    @Test
    public void testEnvelopingSignatureIsRejected() throws Exception {
        assertRejected(createEnvelopingSignedDocument());
    }

    @Test
    public void testBase64TransformIsRejected() throws Exception {
        Document doc = DOMUtils.createDocument();
        Element root = createBook(doc, "Q1hG");
        doc.appendChild(root);
        root.setAttributeNS(null, "Id", "_book");
        root.setIdAttributeNS(null, "Id", true);

        XMLSignature sig = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);
        root.appendChild(sig.getElement());
        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transforms.addTransform(Transforms.TRANSFORM_BASE64_DECODE);
        sig.addDocument("#_book", transforms, MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256);
        sign(sig);

        try {
            process(doc);
            fail("Failure expected on a Base64 transform");
        } catch (XMLStreamException ex) {
            assertTrue(ex.getMessage().contains("is not allowed"));
        }
    }

    private static void assertRejected(Document doc) throws Exception {
        try {
            process(doc);
            fail("Failure expected on a wrapped signature");
        } catch (WebApplicationException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        } catch (XMLStreamException ex) {
            // expected
        }
    }

    // Run the message through the XmlSecInInterceptor, read the (verified) payload, and then run
    // the interceptor it adds to the chain to check the required actions
    private static Element process(Document doc) throws Exception {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.HTTP_REQUEST_METHOD, "POST");
        message.put(SecurityConstants.SIGNATURE_CRYPTO, crypto);
        byte[] bytes = StaxUtils.toString(doc).getBytes(StandardCharsets.UTF_8);
        message.setContent(InputStream.class, new ByteArrayInputStream(bytes));
        PhaseInterceptorChain chain = new PhaseInterceptorChain(new PhaseManagerImpl().getInPhases());
        message.setInterceptorChain(chain);

        XmlSecInInterceptor interceptor = new XmlSecInInterceptor();
        interceptor.setRequireSignature(true);
        interceptor.handleMessage(message);

        Document payload = StaxUtils.read(message.getContent(XMLStreamReader.class));

        ListIterator<Interceptor<? extends Message>> iterator = chain.getIterator();
        while (iterator.hasNext()) {
            @SuppressWarnings("unchecked")
            Interceptor<Message> next = (Interceptor<Message>)iterator.next();
            next.handleMessage(message);
        }
        return payload.getDocumentElement();
    }

    private static Document createSignedDocument() throws Exception {
        Document doc = DOMUtils.createDocument();
        Element root = createBook(doc, "CXF");
        doc.appendChild(root);

        String id = "_book";
        root.setAttributeNS(null, "Id", id);
        root.setIdAttributeNS(null, "Id", true);

        XMLSignature sig = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);
        root.appendChild(sig.getElement());
        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        sig.addDocument("#" + id, transforms, MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256);
        sign(sig);
        return doc;
    }

    private static Document createEnvelopingSignedDocument() throws Exception {
        Document doc = DOMUtils.createDocument();
        Element book = createBook(doc, "CXF");

        String id = "_book";
        book.setAttributeNS(null, "Id", id);
        book.setIdAttributeNS(null, "Id", true);

        XMLSignature sig = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);
        doc.appendChild(sig.getElement());
        Element object = doc.createElementNS(Constants.SignatureSpecNS, "ds:Object");
        object.appendChild(book);
        sig.getElement().appendChild(object);
        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        sig.addDocument("#" + id, transforms, MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256);
        sign(sig);
        return doc;
    }

    private static Document createDetachedSignedDocument() throws Exception {
        Document doc = DOMUtils.createDocument();
        Element root = doc.createElementNS("http://org.apache.cxf/rs/env", "env:Envelope");
        doc.appendChild(root);
        Element book = createBook(doc, "CXF");
        root.appendChild(book);

        String id = "_book";
        book.setAttributeNS(null, "Id", id);
        book.setIdAttributeNS(null, "Id", true);

        XMLSignature sig = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);
        root.appendChild(sig.getElement());
        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        sig.addDocument("#" + id, transforms, MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256);
        sign(sig);
        return doc;
    }

    private static Element createBook(Document doc, String bookName) {
        Element book = doc.createElementNS(null, "Book");
        Element name = doc.createElementNS(null, "name");
        name.setTextContent(bookName);
        book.appendChild(name);
        return book;
    }

    private static void sign(XMLSignature sig) throws Exception {
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("alice");
        X509Certificate cert = crypto.getX509Certificates(cryptoType)[0];
        PrivateKey key = crypto.getPrivateKey("alice", "password");
        sig.addKeyInfo(cert);
        sig.sign(key);
    }
}
