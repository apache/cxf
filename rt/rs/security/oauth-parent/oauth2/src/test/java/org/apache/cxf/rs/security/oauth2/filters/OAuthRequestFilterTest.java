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
package org.apache.cxf.rs.security.oauth2.filters;

import java.lang.reflect.Field;
import java.util.Collections;

import jakarta.ws.rs.NotAuthorizedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class OAuthRequestFilterTest {

    @After
    public void clearCurrentMessage() throws Exception {
        setThreadLocalMessage(null);
    }

    @Test
    public void testValidateAudiencesMatchesSubPathInNonExactMode() throws Exception {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setAudienceIsEndpointAddress(true);
        filter.setCompleteAudienceMatch(false);

        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "/api/read/item");
        setThreadLocalMessage(message);

        String result = filter.validateAudiences(Collections.singletonList("/api/read"));
        assertEquals("/api/read", result);
    }

    @Test
    public void testValidateAudiencesRejectsSiblingPrefixInNonExactMode() throws Exception {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setAudienceIsEndpointAddress(true);
        filter.setCompleteAudienceMatch(false);

        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "/api/readadmin");
        setThreadLocalMessage(message);

        assertThrows(NotAuthorizedException.class,
            () -> filter.validateAudiences(Collections.singletonList("/api/read")));
    }

    @Test
    public void testValidateAudiencesRequiresExactMatchWhenConfigured() throws Exception {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setAudienceIsEndpointAddress(true);
        filter.setCompleteAudienceMatch(true);

        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "/api/read/item");
        setThreadLocalMessage(message);

        assertThrows(NotAuthorizedException.class,
            () -> filter.validateAudiences(Collections.singletonList("/api/read")));
    }

    @Test
    public void testValidateAudiencesSkipsEndpointCheckByDefault() {
        OAuthRequestFilter filter = new OAuthRequestFilter();

        String result = filter.validateAudiences(Collections.singletonList("/api/read"));
        assertNull(result);
    }

    @Test
    public void testValidateAudiencesMatchesQueryBoundaryInNonExactMode() throws Exception {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setAudienceIsEndpointAddress(true);
        filter.setCompleteAudienceMatch(false);

        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "/api/read?include=details");
        setThreadLocalMessage(message);

        String result = filter.validateAudiences(Collections.singletonList("/api/read"));
        assertEquals("/api/read", result);
    }

    @Test
    public void testValidateAudiencesRequiresEndpointCheckEnabledForAudienceMatch() throws Exception {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setCompleteAudienceMatch(false);

        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "/api/read/item");
        setThreadLocalMessage(message);

        // Default is disabled, so endpoint-address audience matching is skipped.
        assertNull(filter.validateAudiences(Collections.singletonList("/api/read")));

        filter.setAudienceIsEndpointAddress(true);
        assertEquals("/api/read", filter.validateAudiences(Collections.singletonList("/api/read")));
    }

    private static void setThreadLocalMessage(Message message) throws Exception {
        Field f = PhaseInterceptorChain.class.getDeclaredField("CURRENT_MESSAGE");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Message> tl = (ThreadLocal<Message>) f.get(null);
        if (message == null) {
            tl.remove();
        } else {
            tl.set(message);
        }
    }
}
