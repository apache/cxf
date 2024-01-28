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

package org.apache.cxf.systest.jaxrs.security.jose.jwejws;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jaxrs.JweJsonClientResponseFilter;
import org.apache.cxf.rs.security.jose.jaxrs.JweJsonWriterInterceptor;
import org.apache.cxf.systest.jaxrs.security.jose.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;


import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSJweJsonTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJweJson.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerJweJson.class, true));
    }


    @Test
    public void testJweJsonSingleRecipientKeyWrapAndAesCbcHmac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwejsonkeywrap";
        BookStore bs = createBookStore(address,
                                       "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties");
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJweJsonSingleRecipientAesGcmDirect() throws Exception {
        String address = "https://localhost:" + PORT + "/jwejsondirect";
        BookStore bs = createBookStore(address,
                                       "org/apache/cxf/systest/jaxrs/security/jwe.direct.properties");
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    private BookStore createBookStore(String address, String propLoc) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJsonTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<>();
        JweJsonWriterInterceptor writer = new JweJsonWriterInterceptor();
        providers.add(writer);
        providers.add(new JweJsonClientResponseFilter());
        bean.setProviders(providers);
        bean.getProperties(true).put(JoseConstants.RSSEC_ENCRYPTION_PROPS,
                                     propLoc);
        return bean.create(BookStore.class);
    }

    @Test
    public void testJweJsontTwoRecipientsKeyWrapAndAesGcm() throws Exception {
        String address = "https://localhost:" + PORT + "/jwejsonTwoRecipients";
        BookStore bs = createBookStoreTwoRecipients(address);
        String text = bs.echoTextJweJsonIn("book");
        assertEquals("bookbook", text);
    }

    private BookStore createBookStoreTwoRecipients(String address) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJsonTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        bean.setProvider(new JweJsonWriterInterceptor());

        List<String> properties = new ArrayList<>();
        properties.add("org/apache/cxf/systest/jaxrs/security/jwejson1.properties");
        properties.add("org/apache/cxf/systest/jaxrs/security/jwejson2.properties");
        bean.getProperties(true).put(JoseConstants.RSSEC_ENCRYPTION_PROPS,
                                 properties);
        return bean.create(BookStore.class);
    }

}
