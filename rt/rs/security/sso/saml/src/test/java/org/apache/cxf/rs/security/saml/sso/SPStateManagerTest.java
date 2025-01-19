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

package org.apache.cxf.rs.security.saml.sso;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

import org.apache.cxf.rs.security.saml.sso.state.EHCacheSPStateManager;
import org.apache.cxf.rs.security.saml.sso.state.MemorySPStateManager;
import org.apache.cxf.rs.security.saml.sso.state.RequestState;
import org.apache.cxf.rs.security.saml.sso.state.ResponseState;
import org.apache.cxf.rs.security.saml.sso.state.SPStateManager;
import org.apache.cxf.rs.security.saml.sso.state.jcache.JCacheSPStateManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Some unit tests for the SPStateManager implementations
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class SPStateManagerTest {

    private final Class<? extends SPStateManager> stateManagerClass;
    private SPStateManager stateManager;

    public SPStateManagerTest(Class<? extends SPStateManager> stateManagerClass) {
        this.stateManagerClass = stateManagerClass;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Class<? extends SPStateManager>> data() {
        return Arrays.asList(MemorySPStateManager.class, EHCacheSPStateManager.class, JCacheSPStateManager.class);
    }

    @Before
    public void setUp() throws IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException {
        stateManager = stateManagerClass.getDeclaredConstructor().newInstance();
    }

    @After
    public void close() throws IOException {
        stateManager.close();
    }

    @Test
    public void testRequestState() throws Exception {
        RequestState requestState = new RequestState("https://www.apache.org/target",
                "https://idp.apache.org",
                "some-request_id",
                "some-issuer-id",
                "/",
                null,
                Instant.now().toEpochMilli(),
                SSOConstants.DEFAULT_STATE_TIME);

        stateManager.setRequestState("some-relay-state", requestState);
        RequestState retrievedState = stateManager.removeRequestState("some-relay-state");
        assertEquals(retrievedState, requestState);

        // Check we can't remove again
        retrievedState = stateManager.removeRequestState("some-relay-state");
        assertNull(retrievedState);

        retrievedState = stateManager.removeRequestState("some-other-relay-state");
        assertNull(retrievedState);
    }

    @Test
    public void testResponseState() throws Exception {
        ResponseState responseState = new ResponseState("some-assertion",
                null,
                "/",
                null,
                Instant.now().toEpochMilli(),
                SSOConstants.DEFAULT_STATE_TIME);

        stateManager.setResponseState("some-context-key", responseState);
        ResponseState retrievedState = stateManager.getResponseState("some-context-key");
        assertEquals(retrievedState, responseState);

        retrievedState = stateManager.removeResponseState("some-context-key");
        assertEquals(retrievedState, responseState);

        // Check we can't remove again
        retrievedState = stateManager.removeResponseState("some-context-state");
        assertNull(retrievedState);

        retrievedState = stateManager.removeResponseState("some-other-context-state");
        assertNull(retrievedState);
    }

    @Test
    public void testCloseTwice() throws Exception {
        RequestState requestState = new RequestState("https://www.apache.org/target",
                "https://idp.apache.org",
                "some-request_id",
                "some-issuer-id",
                "/",
                null,
                Instant.now().toEpochMilli(),
                SSOConstants.DEFAULT_STATE_TIME);

        stateManager.setRequestState("some-relay-state", requestState);

        stateManager.close();
    }

}