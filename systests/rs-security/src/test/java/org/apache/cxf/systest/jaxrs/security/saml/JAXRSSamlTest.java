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

package org.apache.cxf.systest.jaxrs.security.saml;

import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.provider.FormEncodingProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.rs.security.saml.SamlEnvelopedOutInterceptor;
import org.apache.cxf.rs.security.saml.SamlFormOutInterceptor;
import org.apache.cxf.rs.security.saml.SamlHeaderOutInterceptor;
import org.apache.cxf.rs.security.xml.XmlSigOutInterceptor;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXRSSamlTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerSaml.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerSaml.class, true));
    }

    @Test
    public void testSAMLTokenHeaderUsingAuthorizationPolicy() throws Exception {
        String address = "https://localhost:" + PORT + "/samlheader/bookstore/books/123";

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSSamlTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        // Create SAML Token
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(new SamlCallbackHandler(), samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);
        Document doc = DOMUtils.createDocument();
        Element token = assertion.toDOM(doc);

        WebClient wc =  bean.createWebClient();

        HTTPConduit http = (HTTPConduit) WebClient.getConfig(wc).getConduit();
        AuthorizationPolicy authorizationPolicy = new AuthorizationPolicy();
        String encodedToken = encodeToken(DOM2Writer.nodeToString(token));
        authorizationPolicy.setAuthorization(encodedToken);
        authorizationPolicy.setAuthorizationType("SAML");
        http.setAuthorization(authorizationPolicy);

        try {
            Book book = wc.get(Book.class);
            assertEquals(123L, book.getId());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }

    }

    @Test
    public void testGetBookSAMLTokenAsHeader() throws Exception {
        String address = "https://localhost:" + PORT + "/samlheader/bookstore/books/123";

        WebClient wc = createWebClient(address, new SamlHeaderOutInterceptor(), null);

        try {
            Book book = wc.get(Book.class);
            assertEquals(123L, book.getId());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }

    }

    @Test
    public void testInvalidSAMLTokenAsHeader() throws Exception {
        String address = "https://localhost:" + PORT + "/samlheader/bookstore/books/123";

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSSamlTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        WebClient wc = bean.createWebClient();
        wc.header("Authorization", "SAML invalid_grant");
        Response r = wc.get();
        assertEquals(401, r.getStatus());
    }

    @Test
    public void testGetBookSAMLTokenInForm() throws Exception {
        String address = "https://localhost:" + PORT + "/samlform/bookstore/books";
        FormEncodingProvider<Form> formProvider = new FormEncodingProvider<>();
        formProvider.setExpectedEncoded(true);
        WebClient wc = createWebClient(address, new SamlFormOutInterceptor(), formProvider);

        wc.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML);
        try {
            Book book = wc.post(new Form(new MetadataMap<String, String>()).param("name", "CXF").param("id", "125"),
                                Book.class);
            assertEquals(125L, book.getId());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }

    }

    @Test
    public void testEnvelopedSelfSignedSAMLToken() throws Exception {
        doTestEnvelopedSAMLToken(true);
    }

    @Test
    public void testBearerSignedDifferentAlgorithms() throws Exception {
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setSignatureAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        callbackHandler.setDigestAlgorithm(WSS4JConstants.SHA256);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);
        callbackHandler.setSignAssertion(true);
        doTestEnvelopedSAMLToken(true, callbackHandler);
    }

    @Test
    public void testEnvelopedUnsignedSAMLToken() throws Exception {
        doTestEnvelopedSAMLToken(false);
    }

    @Test
    public void testGetBookPreviousSAMLTokenAsHeader() throws Exception {
        String address = "https://localhost:" + PORT + "/samlheader/bookstore/books/123";

        WebClient wc =
            createWebClientForExistingToken(address, new SamlHeaderOutInterceptor(), null);

        try {
            Book book = wc.get(Book.class);
            assertEquals(123L, book.getId());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }

    }

    @Test
    public void testGetBookPreviousSAMLTokenInForm() throws Exception {
        String address = "https://localhost:" + PORT + "/samlform/bookstore/books";
        FormEncodingProvider<Form> formProvider = new FormEncodingProvider<>();
        formProvider.setExpectedEncoded(true);
        WebClient wc = createWebClientForExistingToken(address, new SamlFormOutInterceptor(),
                                       formProvider);

        wc.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML);
        try {
            Book book = wc.post(new Form(new MetadataMap<String, String>()).param("name", "CXF").param("id", "125"),
                                Book.class);
            assertEquals(125L, book.getId());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }

    }

    public void doTestEnvelopedSAMLToken(boolean signed) throws Exception {
        doTestEnvelopedSAMLToken(signed, new SamlCallbackHandler());
    }

    public void doTestEnvelopedSAMLToken(boolean signed, CallbackHandler samlCallbackHandler) throws Exception {
        String address = "https://localhost:" + PORT + "/samlxml/bookstore/books";
        WebClient wc = createWebClient(address, new SamlEnvelopedOutInterceptor(!signed), null, samlCallbackHandler);
        XmlSigOutInterceptor xmlSig = new XmlSigOutInterceptor();
        if (signed) {
            xmlSig.setStyle(XmlSigOutInterceptor.DETACHED_SIG);
        }

        WebClient.getConfig(wc).getOutInterceptors().add(xmlSig);
        wc.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        try {
            Book book = wc.post(new Book("CXF", 125L), Book.class);
            assertEquals(125L, book.getId());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }

    }

    private WebClient createWebClient(String address,
                                      Interceptor<Message> outInterceptor,
                                      Object provider) {
        return createWebClient(address, outInterceptor, provider, new SamlCallbackHandler());
    }

    private WebClient createWebClient(String address,
                                      Interceptor<Message> outInterceptor,
                                      Object provider,
                                      CallbackHandler samlCallbackHandler) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSSamlTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put(SecurityConstants.SAML_CALLBACK_HANDLER, samlCallbackHandler);
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES,
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");
        bean.setProperties(properties);

        bean.getOutInterceptors().add(outInterceptor);
        if (provider != null) {
            bean.setProvider(provider);
        }
        return bean.createWebClient();
    }

    private WebClient createWebClientForExistingToken(String address,
                                      Interceptor<Message> outInterceptor,
                                      Object provider) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSSamlTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        bean.getOutInterceptors().add(outInterceptor);
        bean.getOutInterceptors().add(new SamlRetrievalInterceptor());
        if (provider != null) {
            bean.setProvider(provider);
        }
        return bean.createWebClient();
    }

    private String encodeToken(String assertion) throws Base64Exception {
        byte[] tokenBytes = assertion.getBytes(StandardCharsets.UTF_8);

        tokenBytes = new DeflateEncoderDecoder().deflateToken(tokenBytes);
        StringWriter writer = new StringWriter();
        Base64Utility.encode(tokenBytes, 0, tokenBytes.length, writer);
        return writer.toString();
    }
}
