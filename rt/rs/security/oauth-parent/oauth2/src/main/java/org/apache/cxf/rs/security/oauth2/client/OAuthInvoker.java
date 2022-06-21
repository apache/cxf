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
package org.apache.cxf.rs.security.oauth2.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.ws.rs.NotAuthorizedException;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;

public class OAuthInvoker extends JAXRSInvoker {
    private static final String OAUTH2_CALL_RETRIED = "oauth2.call.retried";
    private WebClient accessTokenServiceClient;
    private ClientTokenContextManager clientTokenContextManager;
    private Consumer consumer;
    @Override
    protected Object performInvocation(Exchange exchange, final Object serviceObject, Method m,
                                       Object[] paramArray) throws Exception {
        Message inMessage = exchange.getInMessage();
        ClientTokenContext tokenContext = inMessage.getContent(ClientTokenContext.class);
        try {
            if (tokenContext != null) {
                StaticClientTokenContext.setClientTokenContext(tokenContext);
            }

            return super.performInvocation(exchange, serviceObject, m, paramArray);
        } catch (InvocationTargetException ex) {
            if (tokenContext != null
                && ex.getCause() instanceof NotAuthorizedException
                && !inMessage.containsKey(OAUTH2_CALL_RETRIED)) {
                ClientAccessToken accessToken = tokenContext.getToken();
                String refreshToken = accessToken.getRefreshToken();
                if (refreshToken != null) {
                    accessToken = OAuthClientUtils.refreshAccessToken(accessTokenServiceClient,
                                                        consumer,
                                                        accessToken);
                    validateRefreshedToken(tokenContext, accessToken);
                    MessageContext mc = new MessageContextImpl(inMessage);
                    ((ClientTokenContextImpl)tokenContext).setToken(accessToken);
                    clientTokenContextManager.setClientTokenContext(mc, tokenContext);

                    //retry
                    inMessage.put(OAUTH2_CALL_RETRIED, true);
                    return super.performInvocation(exchange, serviceObject, m, paramArray);
                }
            }
            throw ex;
        } finally {
            if (tokenContext != null) {
                StaticClientTokenContext.removeClientTokenContext();
            }
        }
    }

    protected void validateRefreshedToken(ClientTokenContext tokenContext, ClientAccessToken refreshedToken) {
        // complete
    }

    public void setAccessTokenServiceClient(WebClient accessTokenServiceClient) {
        this.accessTokenServiceClient = accessTokenServiceClient;
    }


    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }
    public Consumer getConsumer() {
        return consumer;
    }

    public void setClientTokenContextManager(ClientTokenContextManager clientTokenContextManager) {
        this.clientTokenContextManager = clientTokenContextManager;
    }
}
