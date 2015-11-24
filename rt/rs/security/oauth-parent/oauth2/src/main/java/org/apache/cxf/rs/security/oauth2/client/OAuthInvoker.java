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

import javax.ws.rs.NotAuthorizedException;

import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;

public class OAuthInvoker extends JAXRSInvoker {
    private WebClient accessTokenServiceClient;
    private Consumer consumer;
    @Override
    protected Object performInvocation(Exchange exchange, final Object serviceObject, Method m,
                                       Object[] paramArray) throws Exception {
        try {
            return super.performInvocation(exchange, serviceObject, m, paramArray);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof NotAuthorizedException) {
                Message inMessage = exchange.getInMessage();
                ClientTokenContext tokenContext = inMessage.getContent(ClientTokenContext.class);
                ClientAccessToken accessToken = tokenContext.getToken();
                String refreshToken  = accessToken.getRefreshToken();
                if (refreshToken != null) {
                    accessToken = OAuthClientUtils.refreshAccessToken(accessTokenServiceClient, 
                                                        consumer, 
                                                        accessToken);
                    ClientTokenContextManager contextManager = 
                        exchange.getInMessage().getContent(ClientTokenContextManager.class);
                    MessageContext mc = new MessageContextImpl(inMessage);
                    ((ClientTokenContextImpl)tokenContext).setToken(accessToken);           
                    contextManager.setClientTokenContext(mc, tokenContext);
                    
                    //retry
                    return super.performInvocation(exchange, serviceObject, m, paramArray);
                }
            }
            throw ex;
        }
    }
    
    public void setAccessTokenServiceClient(WebClient accessTokenServiceClient) {
        this.accessTokenServiceClient = accessTokenServiceClient;
    }

    
    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }
}
