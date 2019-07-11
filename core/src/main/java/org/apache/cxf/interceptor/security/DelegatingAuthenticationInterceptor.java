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
package org.apache.cxf.interceptor.security;

import java.util.Collections;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class DelegatingAuthenticationInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private Map<String, Interceptor<Message>> authenticationHandlers = Collections.emptyMap();

    public DelegatingAuthenticationInterceptor() {
        super(Phase.UNMARSHAL);
    }

    public DelegatingAuthenticationInterceptor(String phase) {
        super(phase);
    }

    public void handleMessage(Message message) {

        String scheme = getAuthenticationScheme(message);
        Interceptor<Message> handler = authenticationHandlers.get(scheme);
        if (handler == null) {
            throw new AuthenticationException();
        }
        handler.handleMessage(message);
    }

    public void setSchemeHandlers(Map<String, Interceptor<Message>> handlers) {
        this.authenticationHandlers = handlers;
    }

    protected String getAuthenticationScheme(Message message) {
        Map<String, String> headers = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        if (headers == null || !headers.containsKey(AUTHORIZATION_HEADER)) {
            throw new AuthenticationException();
        }
        return headers.get(AUTHORIZATION_HEADER).split(" ")[0].trim();
    }

}
