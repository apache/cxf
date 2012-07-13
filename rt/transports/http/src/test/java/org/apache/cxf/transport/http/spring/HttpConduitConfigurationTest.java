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

package org.apache.cxf.transport.http.spring;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.jsse.TLSParameterJaxBUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.configuration.security.KeyManagersType;
import org.apache.cxf.configuration.security.KeyStoreType;
import org.apache.cxf.configuration.security.TrustManagersType;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPTransportFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class HttpConduitConfigurationTest extends Assert {
    private static EndpointInfo ei;
    private Bus bus;

    @BeforeClass
    public static void setUpOnce() {
        ei = new EndpointInfo();
        ei.setName(new QName("http://apache.org/hello_world", "HelloWorld"));
        ei.setAddress("https://localhost:8443/nopath");
    }
    
    @After
    public void tearDown() {
        bus.shutdown(true);
        BusFactory.setDefaultBus(null);
    }
    
    @Test
    public void testConduitBean() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        bus = factory.createBus("org/apache/cxf/transport/http/spring/conduit-bean.xml");
        HTTPTransportFactory atf = new HTTPTransportFactory(bus);
        HTTPConduit conduit = (HTTPConduit)atf.getConduit(ei);
        
        verifyConduit(conduit);
    }

    @Test
    public void testConduitBeanWithTLSReferences() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        bus = factory.createBus("org/apache/cxf/transport/http/spring/conduit-tlsrefs-bean.xml");
        HTTPTransportFactory atf = new HTTPTransportFactory(bus);
        HTTPConduit conduit = (HTTPConduit)atf.getConduit(ei);

        verifyConduit(conduit);
    }

    private void verifyConduit(HTTPConduit conduit) {
        AuthorizationPolicy authp = conduit.getAuthorization();
        assertNotNull(authp);
        assertEquals("Betty", authp.getUserName());
        assertEquals("password", authp.getPassword());
        TLSClientParameters tlscps = conduit.getTlsClientParameters();
        assertNotNull(tlscps);
        assertTrue(tlscps.isDisableCNCheck());
        assertEquals(3600000, tlscps.getSslCacheTimeout());
        
        KeyManager[] kms = tlscps.getKeyManagers();
        assertTrue(kms != null && kms.length == 1);
        assertTrue(kms[0] instanceof X509KeyManager);
        
        TrustManager[] tms = tlscps.getTrustManagers(); 
        assertTrue(tms != null && tms.length == 1);
        assertTrue(tms[0] instanceof X509TrustManager);
        
        FiltersType csfs = tlscps.getCipherSuitesFilter();
        assertNotNull(csfs);
        assertEquals(5, csfs.getInclude().size());
        assertEquals(1, csfs.getExclude().size());
    }

    
    public static final class ManagersFactory {
    
        public static KeyManager[] getKeyManagers() {
            KeyManagersType kmt = new KeyManagersType();
            KeyStoreType kst = new KeyStoreType();
            kst.setResource("org/apache/cxf/transport/https/resources/Bethal.jks");
            kst.setPassword("password");
            kst.setType("JKS");
        
            kmt.setKeyStore(kst);
            kmt.setKeyPassword("password");
            try {
                return TLSParameterJaxBUtils.getKeyManagers(kmt);
            } catch (Exception e) {
                throw new RuntimeException("failed to retrieve key managers", e);
            }
        }
    
        public static TrustManager[] getTrustManagers() {
            TrustManagersType tmt = new TrustManagersType();
            KeyStoreType kst = new KeyStoreType();
            kst.setResource("org/apache/cxf/transport/https/resources/Gordy.jks");
            kst.setPassword("password");
            kst.setType("JKS");
        
            tmt.setKeyStore(kst);
            try {
                return TLSParameterJaxBUtils.getTrustManagers(tmt);
            } catch (Exception e) {
                throw new RuntimeException("failed to retrieve trust managers", e);
            }
        }
    }

}
