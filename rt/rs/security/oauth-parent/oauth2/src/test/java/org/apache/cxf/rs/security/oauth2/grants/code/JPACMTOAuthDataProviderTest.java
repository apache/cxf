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
package org.apache.cxf.rs.security.oauth2.grants.code;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.JPAOAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.JPAOAuthDataProviderTest;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;



/**
 * Runs the same tests as JPAOAuthDataProviderTest but within a Spring Managed Transaction.
 *
 * Spring spawns a transaction before each call to <code><oauthProvider</code>.
 *
 * Note : this test needs <code>@DirtiesContext</code>, otherwise
 * spring tests cache and reuse emf across test classes
 * while non spring unit tests are closing emf (hence connection exception: closed).
 *
 * @author agonzalez
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("JPACMTCodeDataProvider.xml")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("hibernate")
public class JPACMTOAuthDataProviderTest extends JPAOAuthDataProviderTest {

    @Autowired
    private JPACMTCodeDataProvider oauthProvider;

    @Override
    protected JPAOAuthDataProvider getProvider() {
        return this.oauthProvider;
    }

    @Before
    @Override
    public void setUp() {
        initializeProvider(oauthProvider);
    }

    @After
    @Override
    public void tearDown() {
        tearDownClients();
    }
    
    @Test
    public void testRefreshAccessTokenConcurrently() throws Exception {
        getProvider().setRecycleRefreshTokens(false);

        Client c = addClient("101", "bob");

        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Arrays.asList("a", "refreshToken"));
        atr.setSubject(null);
        final ServerAccessToken at = getProvider().createAccessToken(atr);

        Runnable task = new Runnable() {

            @Override
            public void run() {
                getProvider().refreshAccessToken(c, at.getRefreshToken(), Collections.emptyList());
            }
        };

        Thread th1 = new Thread(task);
        Thread th2 = new Thread(task);
        Thread th3 = new Thread(task);

        th1.start();
        th2.start();
        th3.start();

        th1.join();
        th2.join();
        th3.join();

        assertNotNull(getProvider().getAccessToken(at.getTokenKey()));
        List<RefreshToken> rtl = getProvider().getRefreshTokens(c, null);
        assertNotNull(rtl);
        assertEquals(1, rtl.size());
        List<String> atl = rtl.get(0).getAccessTokens();
        assertNotNull(atl);

        assertEquals(4, atl.size());
    }
}
