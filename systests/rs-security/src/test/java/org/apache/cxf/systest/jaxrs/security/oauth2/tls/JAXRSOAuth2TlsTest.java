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

package org.apache.cxf.systest.jaxrs.security.oauth2.tls;

import java.net.URL;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSOAuth2TlsTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerOAuth2Tls.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerOAuth2Tls.class, true));
    }


    @Test
    public void testTwoWayTLSClientIdIsSubjectDn() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2/token";
        WebClient wc = createWebClient(address);

        ClientAccessToken at = OAuthClientUtils.getAccessToken(wc, new CustomGrant());
        assertNotNull(at.getTokenKey());
    }
    
    @Test
    public void testTwoWayTLSClientIdBound() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2/token";
        WebClient wc = createWebClient(address);

        ClientAccessToken at = OAuthClientUtils.getAccessToken(wc,
                                        new Consumer("bound"),
                                        new CustomGrant());
        assertNotNull(at.getTokenKey());
    }
    
    @Test
    public void testTwoWayTLSClientUnbound() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2/token";
        WebClient wc = createWebClient(address);
        try {
            OAuthClientUtils.getAccessToken(wc,
                                            new Consumer("unbound"),
                                            new CustomGrant());
            fail("exception_expected");
        } catch (OAuthServiceException ex) {
            assertEquals("invalid_client", ex.getError().getError());
        }
        
    }
    

    private WebClient createWebClient(String address) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSOAuth2TlsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        WebClient wc = bean.createWebClient();
        wc.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON);
        return wc;
    }


    private static class CustomGrant implements AccessTokenGrant {

        private static final long serialVersionUID = -4007538779198315873L;

        @Override
        public String getType() {
            return "custom_grant";
        }

        @Override
        public MultivaluedMap<String, String> toMap() {
            MultivaluedMap<String, String> map = new MetadataMap<String, String>();
            map.putSingle(OAuthConstants.GRANT_TYPE, "custom_grant");
            return map;
        }

    }

}
