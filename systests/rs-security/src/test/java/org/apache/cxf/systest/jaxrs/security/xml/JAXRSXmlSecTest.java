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

package org.apache.cxf.systest.jaxrs.security.xml;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.common.RSSecurityUtils;
import org.apache.cxf.rs.security.xml.EncryptionProperties;
import org.apache.cxf.rs.security.xml.SignatureProperties;
import org.apache.cxf.rs.security.xml.XmlEncInInterceptor;
import org.apache.cxf.rs.security.xml.XmlEncOutInterceptor;
import org.apache.cxf.rs.security.xml.XmlSecInInterceptor;
import org.apache.cxf.rs.security.xml.XmlSecOutInterceptor;
import org.apache.cxf.rs.security.xml.XmlSigInInterceptor;
import org.apache.cxf.rs.security.xml.XmlSigOutInterceptor;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.xml.security.encryption.XMLCipher;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(value = org.junit.runners.Parameterized.class)
public class JAXRSXmlSecTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerXmlSec.PORT;
    public static final String STAX_PORT = StaxBookServerXmlSec.PORT;

    final TestParam test;

    public JAXRSXmlSecTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerXmlSec.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(StaxBookServerXmlSec.class, true));
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam[]> data() {

        return Arrays.asList(new TestParam[][] {{new TestParam(PORT, false)},
                                                {new TestParam(STAX_PORT, false)},
                                                {new TestParam(PORT, true)},
                                                {new TestParam(STAX_PORT, true)},
        });
    }

    @Test
    public void testPostBookWithEnvelopedSigAndProxy() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsig";
        doTestSignatureProxy(address, false, null, test.streaming);
    }

    @Test
    public void testOldConfiguration() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsig";
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> newProperties = new HashMap<>();
        newProperties.put(SecurityConstants.CALLBACK_HANDLER,
            "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        newProperties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");

        String cryptoUrl = "org/apache/cxf/systest/jaxrs/security/alice.properties";
        newProperties.put(SecurityConstants.SIGNATURE_PROPERTIES, cryptoUrl);
        bean.setProperties(newProperties);

        if (test.streaming) {
            XmlSecOutInterceptor sigInterceptor = new XmlSecOutInterceptor();
            sigInterceptor.setSignRequest(true);
            bean.getOutInterceptors().add(sigInterceptor);
        } else {
            XmlSigOutInterceptor sigInterceptor = new XmlSigOutInterceptor();
            bean.getOutInterceptors().add(sigInterceptor);
        }
        bean.setServiceClass(BookStore.class);

        BookStore store = bean.create(BookStore.class);
        Book book = store.addBook(new Book("CXF", 126L));
        assertEquals(126L, book.getId());
    }

    @Test
    public void testPostBookWithEnvelopedSigAndProxy2() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsig";
        doTestSignatureProxy(address, false, "", test.streaming);
    }

    @Test
    public void testPostBookEnvelopingSigAndProxy() throws Exception {
        if (test.streaming || STAX_PORT.equals(test.port)) {
            // Enveloping not supported for streaming code
            return;
        }
        String address = "https://localhost:" + test.port + "/xmlsig";
        doTestSignatureProxy(address, true, "file:", test.streaming);
    }

    @Test
    public void testCertConstraints() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsigconstraints";

        // Successful test with "bob"
        Map<String, Object> newProperties = new HashMap<>();
        newProperties.put(SecurityConstants.CALLBACK_HANDLER,
            "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        newProperties.put(SecurityConstants.SIGNATURE_USERNAME, "bob");

        String cryptoUrl = "org/apache/cxf/systest/jaxrs/security/bob.properties";
        newProperties.put(SecurityConstants.SIGNATURE_PROPERTIES, cryptoUrl);
        doTestSignatureProxy(address, false, null, test.streaming, newProperties);

        // Constraint validation fails with "alice"
        newProperties.clear();
        newProperties.put(SecurityConstants.CALLBACK_HANDLER,
            "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        newProperties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");

        cryptoUrl = "org/apache/cxf/systest/jaxrs/security/alice.properties";
        newProperties.put(SecurityConstants.SIGNATURE_PROPERTIES, cryptoUrl);
        try {
            doTestSignatureProxy(address, false, null, test.streaming, newProperties);
            fail("Failure expected on a failing cert constraint");
        } catch (Exception ex) {
            // expected
        }
    }

    private void doTestSignatureProxy(String address, boolean enveloping,
                                      String cryptoUrlPrefix, boolean streaming) throws Exception {
        doTestSignatureProxy(address, enveloping, cryptoUrlPrefix,
                             streaming, new HashMap<String, Object>());
    }

    private void doTestSignatureProxy(String address, boolean enveloping,
                                      String cryptoUrlPrefix, boolean streaming,
                                      Map<String, Object> properties) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> newProperties = new HashMap<>(properties);
        if (newProperties.isEmpty()) {
            newProperties.put(SecurityConstants.CALLBACK_HANDLER,
                           "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
            newProperties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");

            String cryptoUrl = "org/apache/cxf/systest/jaxrs/security/alice.properties";
            if (cryptoUrlPrefix != null) {
                cryptoUrl = cryptoUrlPrefix + this.getClass().getResource("/" + cryptoUrl).toURI().getPath();
            }
            newProperties.put(SecurityConstants.SIGNATURE_PROPERTIES, cryptoUrl);
        }
        bean.setProperties(newProperties);

        if (streaming) {
            XmlSecOutInterceptor sigInterceptor = new XmlSecOutInterceptor();
            sigInterceptor.setSignRequest(true);
            bean.getOutInterceptors().add(sigInterceptor);
        } else {
            XmlSigOutInterceptor sigInterceptor = new XmlSigOutInterceptor();
            if (enveloping) {
                sigInterceptor.setStyle(XmlSigOutInterceptor.ENVELOPING_SIG);
            }
            bean.getOutInterceptors().add(sigInterceptor);
        }
        bean.setServiceClass(BookStore.class);

        BookStore store = bean.create(BookStore.class);
        Book book = store.addBook(new Book("CXF", 126L));
        assertEquals(126L, book.getId());
    }

    @Test
    public void testPostBookWithEnvelopedSig() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsig/bookstore/books";
        doTestSignature(address, false, false, true, test.streaming);
    }

    @Test
    public void testPostBookWithEnvelopedSigNoKeyInfo() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsignokeyinfo/bookstore/books";
        doTestSignature(address, false, false, false, test.streaming);
    }

    @Test
    public void testPostBookWithEnvelopingSig() throws Exception {
        if (test.streaming || STAX_PORT.equals(test.port)) {
            // Enveloping not supported for streaming code
            return;
        }
        String address = "https://localhost:" + test.port + "/xmlsig/bookstore/books";
        doTestSignature(address, true, false, true, test.streaming);
    }

    @Test
    public void testPostBookWithEnvelopingSigFromResponse() throws Exception {
        if (STAX_PORT.equals(test.port)) {
            // Enveloping not supported for streaming code
            return;
        }
        String address = "https://localhost:" + test.port + "/xmlsig/bookstore/books";
        doTestSignature(address, true, true, true, test.streaming);
    }

    private void doTestSignature(String address,
                                 boolean enveloping,
                                 boolean fromResponse,
                                 boolean useKeyInfo,
                                 boolean streaming) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");
        bean.setProperties(properties);
        if (streaming) {
            XmlSecOutInterceptor sigOutInterceptor = new XmlSecOutInterceptor();
            sigOutInterceptor.setSignRequest(true);
            sigOutInterceptor.setKeyInfoMustBeAvailable(useKeyInfo);
            bean.getOutInterceptors().add(sigOutInterceptor);

            XmlSecInInterceptor sigInInterceptor = new XmlSecInInterceptor();
            sigInInterceptor.setRequireSignature(true);
            if (!useKeyInfo) {
                sigInInterceptor.setSignatureVerificationAlias("alice");
            }
            bean.setProvider(sigInInterceptor);
        } else {
            XmlSigOutInterceptor sigOutInterceptor = new XmlSigOutInterceptor();
            if (enveloping) {
                sigOutInterceptor.setStyle(XmlSigOutInterceptor.ENVELOPING_SIG);
            }
            sigOutInterceptor.setKeyInfoMustBeAvailable(useKeyInfo);
            bean.getOutInterceptors().add(sigOutInterceptor);

            XmlSigInInterceptor sigInInterceptor = new XmlSigInInterceptor();
            sigInInterceptor.setKeyInfoMustBeAvailable(useKeyInfo);
            bean.getInInterceptors().add(sigInInterceptor);
        }

        WebClient wc = bean.createWebClient();
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        Book book;
        if (!fromResponse) {
            book = wc.type("application/xml").post(new Book("CXF", 126L), Book.class);
        } else {
            book = wc.type("application/xml").post(new Book("CXF", 126L)).readEntity(Book.class);
        }
        assertEquals(126L, book.getId());
    }

    @Test
    public void testSignatureNegativeServer() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsignegativeserver/bookstore/books";

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "bethal");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/bethal.properties");
        bean.setProperties(properties);
        if (test.streaming) {
            XmlSecOutInterceptor sigOutInterceptor = new XmlSecOutInterceptor();
            sigOutInterceptor.setSignRequest(true);
            bean.getOutInterceptors().add(sigOutInterceptor);

            XmlSecInInterceptor sigInInterceptor = new XmlSecInInterceptor();
            sigInInterceptor.setRequireSignature(true);
            bean.setProvider(sigInInterceptor);
        } else {
            XmlSigOutInterceptor sigOutInterceptor = new XmlSigOutInterceptor();
            bean.getOutInterceptors().add(sigOutInterceptor);

            XmlSigInInterceptor sigInInterceptor = new XmlSigInInterceptor();
            bean.getInInterceptors().add(sigInInterceptor);
        }

        WebClient wc = bean.createWebClient();
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        try {
            wc.type("application/xml").post(new Book("CXF", 126L), Book.class);
            fail("Failure expected on signature trust failure");
        } catch (WebApplicationException ex) {
            assertTrue(ex.getMessage().contains("400 Bad Request"));
        }
    }

    @Test
    public void testSignatureNegativeClient() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsignegativeclient/bookstore/books";

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "bethal");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/bethal.properties");
        bean.setProperties(properties);
        if (test.streaming) {
            XmlSecOutInterceptor sigOutInterceptor = new XmlSecOutInterceptor();
            sigOutInterceptor.setSignRequest(true);
            bean.getOutInterceptors().add(sigOutInterceptor);

            XmlSecInInterceptor sigInInterceptor = new XmlSecInInterceptor();
            sigInInterceptor.setRequireSignature(true);
            bean.setProvider(sigInInterceptor);
        } else {
            XmlSigOutInterceptor sigOutInterceptor = new XmlSigOutInterceptor();
            bean.getOutInterceptors().add(sigOutInterceptor);

            XmlSigInInterceptor sigInInterceptor = new XmlSigInInterceptor();
            bean.getInInterceptors().add(sigInInterceptor);
        }

        WebClient wc = bean.createWebClient();
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        try {
            wc.type("application/xml").post(new Book("CXF", 126L), Book.class);
            fail("Failure expected on signature trust failure");
        } catch (ProcessingException ex) {
            assertTrue(ex.getCause() instanceof BadRequestException);
        }
    }

    @Test
    public void testUnsignedServerResponse() throws Exception {
        if (STAX_PORT.equals(test.port)) {
            // We are only testing the client here
            return;
        }
        String address = "https://localhost:" + test.port + "/xmlnosigresponse/bookstore/books";

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");
        bean.setProperties(properties);
        if (test.streaming) {
            XmlSecOutInterceptor sigOutInterceptor = new XmlSecOutInterceptor();
            sigOutInterceptor.setSignRequest(true);
            bean.getOutInterceptors().add(sigOutInterceptor);

            XmlSecInInterceptor sigInInterceptor = new XmlSecInInterceptor();
            sigInInterceptor.setRequireSignature(true);
            bean.setProvider(sigInInterceptor);
        } else {
            XmlSigOutInterceptor sigOutInterceptor = new XmlSigOutInterceptor();
            bean.getOutInterceptors().add(sigOutInterceptor);

            XmlSigInInterceptor sigInInterceptor = new XmlSigInInterceptor();
            bean.getInInterceptors().add(sigInInterceptor);
        }

        WebClient wc = bean.createWebClient();
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        try {
            wc.type("application/xml").post(new Book("CXF", 126L), Book.class);
            fail("Failure expected on an unsigned response message");
        } catch (ProcessingException ex) {
            assertTrue(ex.getCause() instanceof BadRequestException);
        }
    }

    @Test
    public void testPostBookWithEnvelopedSigKeyName() throws Exception {
        // This test only applies to StAX - see CXF-7084
        if (!test.streaming || !STAX_PORT.equals(test.port)) {
            return;
        }
        String address = "https://localhost:" + test.port + "/xmlsigkeyname/bookstore/books";

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");
        bean.setProperties(properties);
        XmlSecOutInterceptor sigOutInterceptor = new XmlSecOutInterceptor();
        sigOutInterceptor.setSignRequest(true);
        sigOutInterceptor.setKeyInfoMustBeAvailable(true);

        SignatureProperties sigProps = new SignatureProperties();
        sigProps.setSignatureKeyName("alice-kn");
        sigProps.setSignatureKeyIdType("KeyName");
        sigOutInterceptor.setSignatureProperties(sigProps);

        bean.getOutInterceptors().add(sigOutInterceptor);

        XmlSecInInterceptor sigInInterceptor = new XmlSecInInterceptor();
        sigInInterceptor.setRequireSignature(true);
        bean.setProvider(sigInInterceptor);

        WebClient wc = bean.createWebClient();
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        Book book = wc.type("application/xml").post(new Book("CXF", 126L), Book.class);
        assertEquals(126L, book.getId());
    }

    @Test
    public void testPostEncryptedBook() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlenc/bookstore/books";
        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/bob.properties");
        doTestPostEncryptedBook(address, false, properties, test.streaming);
    }

    @Test
    public void testPostEncryptedBookGCM() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlenc/bookstore/books";
        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/bob.properties");

        EncryptionProperties encryptionProperties = new EncryptionProperties();
        String aes128GCM = "http://www.w3.org/2009/xmlenc11#aes128-gcm";
        encryptionProperties.setEncryptionSymmetricKeyAlgo(aes128GCM);
        encryptionProperties.setEncryptionKeyIdType(RSSecurityUtils.X509_CERT);

        doTestPostEncryptedBook(address, false, properties, encryptionProperties, false, test.streaming);
    }

    @Test
    public void testPostEncryptedBookSHA256() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlenc/bookstore/books";
        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/bob.properties");

        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionSymmetricKeyAlgo(XMLCipher.AES_128);
        encryptionProperties.setEncryptionKeyIdType(RSSecurityUtils.X509_CERT);
        encryptionProperties.setEncryptionDigestAlgo(XMLCipher.SHA256);

        doTestPostEncryptedBook(
            address, false, properties, encryptionProperties, false, test.streaming
        );
    }

    @Test
    public void testPostEncryptedBookIssuerSerial() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlenc/bookstore/books";
        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/bob.properties");

        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionSymmetricKeyAlgo(XMLCipher.AES_128);
        encryptionProperties.setEncryptionKeyIdType(RSSecurityUtils.X509_ISSUER_SERIAL);

        doTestPostEncryptedBook(
            address, false, properties, encryptionProperties, false, test.streaming
        );
    }

    @Test
    public void testPostEncryptedSignedBook() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsec-validate/bookstore/books";
        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/bob.properties");
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");
        doTestPostEncryptedBook(address, true, properties, test.streaming);

    }

    @Test
    public void testPostEncryptedSignedBookInvalid() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsec-validate/bookstore/books";
        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/bob.properties");
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");

        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionSymmetricKeyAlgo(
            "http://www.w3.org/2009/xmlenc11#aes128-gcm");
        encryptionProperties.setEncryptionKeyIdType(RSSecurityUtils.X509_CERT);

        try {
            doTestPostEncryptedBook(address, true, properties, encryptionProperties, true, test.streaming);
        } catch (BadRequestException ex) {
            assertEquals(400, ex.getResponse().getStatus());
        }

    }

    @Test
    public void testPostEncryptedSignedBookUseReqSigCert() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsec-useReqSigCert/bookstore/books";
        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/bob.properties");
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");
        doTestPostEncryptedBook(address, true, properties, test.streaming);
    }

    public void doTestPostEncryptedBook(String address, boolean sign, Map<String, Object> properties,
                                        boolean streaming)
        throws Exception {
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionSymmetricKeyAlgo(XMLCipher.AES_128);
        encryptionProperties.setEncryptionKeyIdType(RSSecurityUtils.X509_CERT);
        doTestPostEncryptedBook(
            address, sign, properties, encryptionProperties, false, test.streaming
        );
    }

    public void doTestPostEncryptedBook(
        String address, boolean sign, Map<String, Object> properties,
        EncryptionProperties encryptionProperties,
        boolean propagateException,
        boolean streaming
    ) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        bean.setProperties(properties);
        if (streaming) {
            XmlSecOutInterceptor encInterceptor = new XmlSecOutInterceptor();
            encInterceptor.setEncryptionKeyIdentifierType(encryptionProperties.getEncryptionKeyIdType());
            encInterceptor.setSymmetricEncAlgorithm(encryptionProperties.getEncryptionSymmetricKeyAlgo());
            encInterceptor.setEncryptionDigestAlgorithm(encryptionProperties.getEncryptionDigestAlgo());
            encInterceptor.setEncryptRequest(true);
            if (sign) {
                encInterceptor.setSignRequest(true);
            }
            bean.getOutInterceptors().add(encInterceptor);

            XmlSecInInterceptor encInInterceptor = new XmlSecInInterceptor();
            encInInterceptor.setRequireEncryption(true);
            bean.setProvider(encInInterceptor);
        } else {
            if (sign) {
                bean.getOutInterceptors().add(new XmlSigOutInterceptor());
            }
            XmlEncOutInterceptor encInterceptor = new XmlEncOutInterceptor();
            encInterceptor.setKeyIdentifierType(encryptionProperties.getEncryptionKeyIdType());
            encInterceptor.setSymmetricEncAlgorithm(encryptionProperties.getEncryptionSymmetricKeyAlgo());
            encInterceptor.setDigestAlgorithm(encryptionProperties.getEncryptionDigestAlgo());
            bean.getOutInterceptors().add(encInterceptor);

            bean.getInInterceptors().add(new XmlEncInInterceptor());
            if (sign) {
                bean.getInInterceptors().add(new XmlSigInInterceptor());
            }
        }

        WebClient wc = bean.createWebClient();
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        try {
            Book book = wc.type("application/xml").post(new Book("CXF", 126L), Book.class);
            assertEquals(126L, book.getId());
        } catch (WebApplicationException ex) {
            if (propagateException) {
                throw ex;
            }
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            assertTrue(ex.getCause() instanceof BadRequestException);
        }

    }


    @Test
    public void testPostBookWithNoSig() throws Exception {
        if (test.streaming) {
            // Only testing the endpoints, not the clients here
            return;
        }
        String address = "https://localhost:" + test.port + "/xmlsig";

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        bean.setServiceClass(BookStore.class);

        BookStore store = bean.create(BookStore.class);
        try {
            store.addBook(new Book("CXF", 126L));
            fail("Failure expected on no Signature");
        } catch (WebApplicationException ex) {
            // expected
        }
    }

    @Test
    public void testEncryptionNoSignature() throws Exception {
        if (test.streaming) {
            // Only testing the endpoints, not the clients here
            return;
        }
        String address = "https://localhost:" + test.port + "/xmlsec-validate";

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/bob.properties");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");
        bean.setProperties(properties);

        XmlEncOutInterceptor encInterceptor = new XmlEncOutInterceptor();
        encInterceptor.setKeyIdentifierType(RSSecurityUtils.X509_CERT);
        encInterceptor.setSymmetricEncAlgorithm(XMLCipher.AES_128);
        bean.getOutInterceptors().add(encInterceptor);
        bean.getInInterceptors().add(new XmlEncInInterceptor());
        bean.getInInterceptors().add(new XmlSigInInterceptor());

        bean.setServiceClass(BookStore.class);

        BookStore store = bean.create(BookStore.class);
        try {
            store.addBook(new Book("CXF", 126L));
            fail("Failure expected on no Signature");
        } catch (WebApplicationException ex) {
            // expected
        }
    }

    @Test
    public void testSignatureNoEncryption() throws Exception {
        if (test.streaming) {
            // Only testing the endpoints, not the clients here
            return;
        }
        String address = "https://localhost:" + test.port + "/xmlsec-validate";

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "bob");
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/bob.properties");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");
        bean.setProperties(properties);

        XmlSigOutInterceptor sigInterceptor = new XmlSigOutInterceptor();
        bean.getOutInterceptors().add(sigInterceptor);
        bean.getInInterceptors().add(new XmlEncInInterceptor());
        bean.getInInterceptors().add(new XmlSigInInterceptor());

        bean.setServiceClass(BookStore.class);

        BookStore store = bean.create(BookStore.class);
        try {
            store.addBook(new Book("CXF", 126L));
            fail("Failure expected on no Encryption");
        } catch (WebApplicationException ex) {
            // expected
        }
    }

    @Test
    public void testSignaturePassword() throws Exception {
        String address = "https://localhost:" + test.port + "/xmlsig/bookstore/books";

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSXmlSecTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        properties.put(SecurityConstants.SIGNATURE_PASSWORD, "password");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");
        bean.setProperties(properties);
        if (test.streaming) {
            XmlSecOutInterceptor sigOutInterceptor = new XmlSecOutInterceptor();
            sigOutInterceptor.setSignRequest(true);
            sigOutInterceptor.setKeyInfoMustBeAvailable(true);
            bean.getOutInterceptors().add(sigOutInterceptor);

            XmlSecInInterceptor sigInInterceptor = new XmlSecInInterceptor();
            sigInInterceptor.setRequireSignature(true);
            bean.setProvider(sigInInterceptor);
        } else {
            XmlSigOutInterceptor sigOutInterceptor = new XmlSigOutInterceptor();
            sigOutInterceptor.setKeyInfoMustBeAvailable(true);
            bean.getOutInterceptors().add(sigOutInterceptor);

            XmlSigInInterceptor sigInInterceptor = new XmlSigInInterceptor();
            sigInInterceptor.setKeyInfoMustBeAvailable(true);
            bean.getInInterceptors().add(sigInInterceptor);
        }

        WebClient wc = bean.createWebClient();
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        Book book = wc.type("application/xml").post(new Book("CXF", 126L), Book.class);
        assertEquals(126L, book.getId());
    }

    private static final class TestParam {
        final String port;
        final boolean streaming;

        TestParam(String p, boolean b) {
            port = p;
            streaming = b;
        }

        public String toString() {
            return port + ":" + (streaming ? "streaming" : "dom");
        }

    }

}
