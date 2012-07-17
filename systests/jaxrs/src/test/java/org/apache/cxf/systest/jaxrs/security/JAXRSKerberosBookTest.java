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

package org.apache.cxf.systest.jaxrs.security;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.security.KerberosAuthOutInterceptor;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.auth.HttpAuthHeader;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


public class JAXRSKerberosBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookKerberosServer.PORT;

    private static final String KERBEROS_CONFIG_FILE =
        "org/apache/cxf/systest/jaxrs/security/kerberosClient.xml";
    
    @BeforeClass
    public static void startServers() throws Exception {
        String jaasConfig = JAXRSKerberosBookTest.class
            .getResource("/org/apache/cxf/systest/jaxrs/security/kerberos.cfg").toURI().getPath();
        System.setProperty("java.security.auth.login.config", jaasConfig);

        assertTrue("server did not launch correctly",
                   launchServer(BookKerberosServer.class, true));
    }
    
    @Test
    @Ignore
    public void testGetBookWithConfigInHttpConduit() throws Exception {
        doTestGetBook123Proxy(KERBEROS_CONFIG_FILE);
    }
    
    private void doTestGetBook123Proxy(String configFile) throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class, 
                configFile);
        WebClient.getConfig(bs).getOutInterceptors().add(new LoggingOutInterceptor());
        // just to verify the interface call goes through CGLIB proxy too
        assertEquals("http://localhost:" + PORT, WebClient.client(bs).getBaseURI().toString());
        Book b = bs.getBook("123");
        assertEquals(b.getId(), 123);
        b = bs.getBook("123");
        assertEquals(b.getId(), 123);
    }
    
    @Test
    @Ignore
    public void testGetBookWithInterceptor() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/123");
        
        KerberosAuthOutInterceptor kbInterceptor = new KerberosAuthOutInterceptor();
        
        AuthorizationPolicy policy = new AuthorizationPolicy();
        policy.setAuthorizationType(HttpAuthHeader.AUTH_TYPE_NEGOTIATE);
        policy.setAuthorization("KerberosClient");
        policy.setUserName("alice");
        policy.setPassword("alice");
        
        kbInterceptor.setPolicy(policy);
        
        WebClient.getConfig(wc).getOutInterceptors().add(new LoggingOutInterceptor());
        WebClient.getConfig(wc).getOutInterceptors().add(kbInterceptor);
        
        Book b = wc.get(Book.class);
        assertEquals(b.getId(), 123);
    }
}
