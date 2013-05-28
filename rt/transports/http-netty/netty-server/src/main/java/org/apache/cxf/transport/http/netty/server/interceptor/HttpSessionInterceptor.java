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
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class HttpSessionInterceptor implements NettyInterceptor {
    private boolean sessionRequestedByCookie;

    public HttpSessionInterceptor(HttpSessionStore sessionStore) {
        HttpSessionThreadLocal.setSessionStore(sessionStore);
    }

    @Override
    public void onRequestReceived(ChannelHandlerContext ctx, MessageEvent e) {

        HttpSessionThreadLocal.unset();

        HttpRequest request = (HttpRequest) e.getMessage();
        Collection<Cookie> cookies = Utils.getCookies(
                NettyHttpSession.SESSION_ID_KEY, request);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String jsessionId = cookie.getValue();
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
    public void onRequestSuccessed(ChannelHandlerContext ctx, MessageEvent e,
                                   HttpResponse response) {

        NettyHttpSession s = HttpSessionThreadLocal.get();
        if (s != null && !this.sessionRequestedByCookie) {
            CookieEncoder cookieEncoder = new CookieEncoder(true);
            cookieEncoder.addCookie(NettyHttpSession.SESSION_ID_KEY, s.getId());
            HttpHeaders.addHeader(response, Names.SET_COOKIE, cookieEncoder.encode());
        }

    }

    @Override
    public void onRequestFailed(ChannelHandlerContext ctx, ExceptionEvent e) {
        this.sessionRequestedByCookie = false;
        HttpSessionThreadLocal.unset();
    }
}
