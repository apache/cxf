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

package org.apache.cxf.transport.http.netty.server.interceptor;

import java.util.Collection;

import org.apache.cxf.transport.http.netty.server.servlet.HttpSessionThreadLocal;
import org.apache.cxf.transport.http.netty.server.servlet.NettyHttpSession;
import org.apache.cxf.transport.http.netty.server.session.HttpSessionStore;
import org.apache.cxf.transport.http.netty.server.util.Utils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;

public class HttpSessionInterceptor implements NettyInterceptor {
    private boolean sessionRequestedByCookie;

    public HttpSessionInterceptor(HttpSessionStore sessionStore) {
        HttpSessionThreadLocal.setSessionStore(sessionStore);
    }

    @Override
    public void onRequestReceived(ChannelHandlerContext ctx, HttpRequest request) {

        HttpSessionThreadLocal.unset();

        Collection<Cookie> cookies = Utils.getCookies(
                NettyHttpSession.SESSION_ID_KEY, request);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String jsessionId = cookie.value();
                NettyHttpSession s = HttpSessionThreadLocal.getSessionStore()
                        .findSession(jsessionId);
                if (s != null) {
                    HttpSessionThreadLocal.set(s);
                    this.sessionRequestedByCookie = true;
                    break;
                }
            }
        }
    }

    @Override
    public void onRequestSuccessed(ChannelHandlerContext ctx,
                                   HttpResponse response) {

        NettyHttpSession s = HttpSessionThreadLocal.get();
        if (s != null && !this.sessionRequestedByCookie) {
            // setup the Cookie for session
            response.headers().set(HttpHeaderNames.SET_COOKIE,
                                  ClientCookieEncoder.STRICT.encode(NettyHttpSession.SESSION_ID_KEY, s.getId()));
        }

    }

    @Override
    public void onRequestFailed(ChannelHandlerContext ctx, Throwable e) {
        this.sessionRequestedByCookie = false;
        HttpSessionThreadLocal.unset();
    }
}
