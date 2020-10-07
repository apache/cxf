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

package org.apache.cxf.transport.http.netty.server.session;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.http.netty.server.servlet.NettyHttpSession;


public class DefaultHttpSessionStore implements HttpSessionStore {

    private static final Logger LOG = LogUtils.getL7dLogger(DefaultHttpSessionStore.class);

    private static final Map<String, NettyHttpSession> SESSIONS
        = new ConcurrentHashMap<>();

    @Override
    public NettyHttpSession createSession() {
        String sessionId = this.generateNewSessionId();
        LOG.log(Level.FINE, "Creating new session with id {}", sessionId);

        NettyHttpSession session = new NettyHttpSession(sessionId);
        SESSIONS.put(sessionId, session);
        return session;
    }

    @Override
    public void destroySession(String sessionId) {
        LOG.log(Level.FINE, "Destroying session with id {}", sessionId);
        SESSIONS.remove(sessionId);
    }

    @Override
    public NettyHttpSession findSession(String sessionId) {
        return SESSIONS.get(sessionId);
    }

    protected String generateNewSessionId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void destroyInactiveSessions() {
        for (Map.Entry<String, NettyHttpSession> entry : SESSIONS.entrySet()) {
            NettyHttpSession session = entry.getValue();
            if (session.getMaxInactiveInterval() < 0) {
                continue;
            }

            long currentMillis = System.currentTimeMillis();

            if (currentMillis - session.getLastAccessedTime() > session
                    .getMaxInactiveInterval() * 1000) {

                destroySession(entry.getKey());
            }
        }
    }
}
