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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.saml.SamlEnvelopedOutInterceptor;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXRSSamlAuthorizationTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerSaml.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(SecureBookServerSaml.class, true));
    }

    @Test
    public void testPostBookUserRole() throws Exception {
        String address = "https://localhost:" + PORT + "/saml-roles/bookstore/books";
        WebClient wc = createWebClient(address, null);
        wc.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        try {
            wc.post(new Book("CXF", 125L), Book.class);
            fail("403 is expected");
        } catch (WebApplicationException ex) {
            assertEquals(403, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testPostBookAdminRole() throws Exception {
        String address = "https://localhost:" + PORT + "/saml-roles/bookstore/books";
        WebClient wc = createWebClient(address,
                                       Collections.<String, Object>singletonMap("saml.roles",
                                       Collections.singletonList("admin")));
        wc.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        Book book = wc.post(new Book("CXF", 125L), Book.class);
        assertEquals(125L, book.getId());
    }

    @Test
    public void testPostBookAdminRoleWithWrongSubjectNameFormat() throws Exception {
        String address = "https://localhost:" + PORT + "/saml-roles2/bookstore/books";
        WebClient wc = createWebClient(address,
                                       Collections.<String, Object>singletonMap("saml.roles",
                                        Collections.singletonList("admin")));
        wc.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        try {
            wc.post(new Book("CXF", 125L), Book.class);
            fail("403 is expected");
        } catch (WebApplicationException ex) {
            assertEquals(403, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testPostBookAdminRoleWithGoodSubjectName() throws Exception {
        String address = "https://localhost:" + PORT + "/saml-roles2/bookstore/books";

        Map<String, Object> props = new HashMap<>();
        props.put("saml.roles", Collections.singletonList("admin"));
        props.put("saml.subject.name", "bob@mycompany.com");
        WebClient wc = createWebClient(address, props);
        wc.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        Book book = wc.post(new Book("CXF", 125L), Book.class);
        assertEquals(125L, book.getId());
    }

    @Test
    public void testPostBookAdminWithWeakClaims() throws Exception {
        String address = "https://localhost:" + PORT + "/saml-claims/bookstore/books";

        Map<String, Object> props = new HashMap<>();
        WebClient wc = createWebClient(address, props);
        wc.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        try {
            wc.post(new Book("CXF", 125L), Book.class);
            fail("403 is expected");
        } catch (WebApplicationException ex) {
            assertEquals(403, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testPostBookAdminWithWeakClaims2() throws Exception {
        String address = "https://localhost:" + PORT + "/saml-claims/bookstore/books";

        Map<String, Object> props = new HashMap<>();
        props.put("saml.roles", Collections.singletonList("admin"));
        props.put("saml.auth", Collections.singletonList("password"));
        WebClient wc = createWebClient(address, props);
        wc.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        try {
            wc.post(new Book("CXF", 125L), Book.class);
            fail("403 is expected");
        } catch (WebApplicationException ex) {
            assertEquals(403, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testPostBookAdminWithClaims() throws Exception {
        String address = "https://localhost:" + PORT + "/saml-claims/bookstore/books";

        Map<String, Object> props = new HashMap<>();
        props.put("saml.roles", Collections.singletonList("admin"));
        props.put("saml.auth", Collections.singletonList("smartcard"));
        WebClient wc = createWebClient(address, props);
        wc.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        Book book = wc.post(new Book("CXF", 125L), Book.class);
        assertEquals(125L, book.getId());
    }

    private WebClient createWebClient(String address, Map<String, Object> extraProperties) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSSamlAuthorizationTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.SAML_CALLBACK_HANDLER,
                       "org.apache.cxf.systest.jaxrs.security.saml.SamlCallbackHandler");
        if (extraProperties != null) {
            properties.putAll(extraProperties);
        }
        bean.setProperties(properties);

        bean.getOutInterceptors().add(new SamlEnvelopedOutInterceptor());

        return bean.createWebClient();
    }
}
