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
import java.security.Security;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jaxrs.JweJsonClientResponseFilter;
import org.apache.cxf.rs.security.jose.jaxrs.JweJsonWriterInterceptor;
import org.apache.cxf.systest.jaxrs.security.jose.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSJweJsonTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJweJson.PORT;
    private static final Boolean SKIP_AES_GCM_TESTS = isJava6();
    
    private static boolean isJava6() {
        String version = System.getProperty("java.version");
        return 1.6D == Double.parseDouble(version.substring(0, 3));    
    }
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerJweJson.class, true));
        registerBouncyCastle();
    }
    
    private static void registerBouncyCastle() throws Exception {
        Security.addProvider(new BouncyCastleProvider());    
    }
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);    
    }
    
    @Test
    public void testJweJsonPlainTextHmac() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        String address = "https://localhost:" + PORT + "/jwejsonhmac";
        BookStore bs = createBookStore(address, 
                                       "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties",
                                       null);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    
    private BookStore createBookStore(String address, Object properties,
                                      List<?> extraProviders) throws Exception {
        return createBookStore(address, 
                               Collections.singletonMap(JoseConstants.RSSEC_ENCRYPTION_PROPS, properties),
                               extraProviders);
    }
    private BookStore createBookStore(String address, 
                                      Map<String, Object> mapProperties,
                                      List<?> extraProviders) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJsonTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JweJsonWriterInterceptor writer = new JweJsonWriterInterceptor();
        providers.add(writer);
        providers.add(new JweJsonClientResponseFilter());
        if (extraProviders != null) {
            providers.addAll(extraProviders);
        }
        bean.setProviders(providers);
        bean.getProperties(true).putAll(mapProperties);
        return bean.create(BookStore.class);
    }
    
}
