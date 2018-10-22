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

package org.apache.cxf.transport.http.netty.server.servlet;

import org.apache.cxf.transport.http.netty.server.session.HttpSessionStore;

public final class HttpSessionThreadLocal {
    public static final ThreadLocal<NettyHttpSession> SESSION_THREAD_LOCAL = new ThreadLocal<>();

    private static HttpSessionStore sessionStore;

    private HttpSessionThreadLocal() {
        // Utils class
    }

    public static HttpSessionStore getSessionStore() {
        return sessionStore;
    }

    public static void setSessionStore(HttpSessionStore store) {
        sessionStore = store;
    }

    public static void set(NettyHttpSession session) {
        SESSION_THREAD_LOCAL.set(session);
    }

    public static void unset() {
        SESSION_THREAD_LOCAL.remove();
    }

    public static NettyHttpSession get() {
        NettyHttpSession session = SESSION_THREAD_LOCAL.get();
        if (session != null) {
            session.touch();
        }
        return session;
    }

    public static NettyHttpSession getOrCreate() {
        if (HttpSessionThreadLocal.get() == null) {
            //HttpSession newSession = sessionStore.createSession();
            // TODO need to set the sessionTimeout
            //newSession.setMaxInactiveInterval();
            SESSION_THREAD_LOCAL.set(sessionStore.createSession());
        }
        return get();
    }

}
