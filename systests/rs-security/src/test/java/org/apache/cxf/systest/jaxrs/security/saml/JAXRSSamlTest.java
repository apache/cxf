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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.ClientWebApplicationException;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.apache.cxf.jaxrs.provider.FormEncodingProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.saml.SamlEnvelopedOutInterceptor;
import org.apache.cxf.rs.security.saml.SamlFormOutInterceptor;
import org.apache.cxf.rs.security.saml.SamlHeaderOutInterceptor;
import org.apache.cxf.rs.security.xml.XmlSigOutInterceptor;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSSamlTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerSaml.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerSaml.class, true));
    }
    
    @Test
    public void testGetBookSAMLTokenAsHeader() throws Exception {
        String address = "https://localhost:" + PORT + "/samlheader/bookstore/books/123";
        
        WebClient wc = createWebClient(address, new SamlHeaderOutInterceptor(), null, true);
        
        try {
            Book book = wc.get(Book.class);
            assertEquals(123L, book.getId());
        } catch (ServerWebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ClientWebApplicationException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }
    
    @Test
    public void testGetBookSAMLTokenInForm() throws Exception {
        String address = "https://localhost:" + PORT + "/samlform/bookstore/books";
        FormEncodingProvider formProvider = new FormEncodingProvider();
        formProvider.setExpectedEncoded(true);
        WebClient wc = createWebClient(address, new SamlFormOutInterceptor(),
                                       formProvider, true);
        
        wc.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML);
        try {
            Book book = wc.post(new Form().set("name", "CXF").set("id", 125),
                                Book.class);                
            assertEquals(125L, book.getId());
        } catch (ServerWebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ClientWebApplicationException ex) {
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
    public void testEnvelopedUnsignedSAMLToken() throws Exception {
        doTestEnvelopedSAMLToken(false);
    }
    
    public void doTestEnvelopedSAMLToken(boolean signed) throws Exception {
        String address = "https://localhost:" + PORT + "/samlxml/bookstore/books";
        WebClient wc = createWebClient(address, new SamlEnvelopedOutInterceptor(!signed),
                                       null, signed);
        XmlSigOutInterceptor xmlSig = new XmlSigOutInterceptor();
        if (signed) {
            xmlSig.setStyle(XmlSigOutInterceptor.DETACHED_SIG);
        }
                
        WebClient.getConfig(wc).getOutInterceptors().add(xmlSig);
        wc.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        try {
            Book book = wc.post(new Book("CXF", 125L), Book.class);                
            assertEquals(125L, book.getId());
        } catch (ServerWebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ClientWebApplicationException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }
    
    private WebClient createWebClient(String address, 
                                      Interceptor<Message> outInterceptor,
                                      Object provider,
                                      boolean selfSign) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSSamlTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("ws-security.callback-handler", 
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        properties.put("ws-security.saml-callback-handler", 
                       "org.apache.cxf.systest.jaxrs.security.saml.SamlCallbackHandler");
        properties.put("ws-security.signature.username", "alice");
        properties.put("ws-security.signature.properties", 
                       "org/apache/cxf/systest/jaxrs/security/alice.properties");
        if (selfSign) {
            properties.put("ws-security.self-sign-saml-assertion", "true");
        }
        bean.setProperties(properties);
        
        bean.getOutInterceptors().add(outInterceptor);
        if (provider != null) {
            bean.setProvider(provider);
        }
        return bean.createWebClient();
    }
}
