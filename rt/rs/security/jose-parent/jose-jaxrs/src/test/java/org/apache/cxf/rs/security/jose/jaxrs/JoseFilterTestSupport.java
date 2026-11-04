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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.cxf.bus.managers.PhaseManagerImpl;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.spec.ClientRequestContextImpl;
import org.apache.cxf.jaxrs.client.spec.ClientResponseContextImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;

/**
 * Builds the real server and client contexts the JOSE filters see at runtime.
 */
final class JoseFilterTestSupport {
    interface Action {
        void run() throws Exception;
    }

    private JoseFilterTestSupport() {
    }

    static Message serverRequest(String httpMethod, byte[] body) {
        return serverRequest(httpMethod, body, Integer.toString(body.length));
    }

    static Message serverRequest(String httpMethod, byte[] body, String contentLength) {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.HTTP_REQUEST_METHOD, httpMethod);
        message.setContent(InputStream.class, new ByteArrayInputStream(body));
        message.put(Message.PROTOCOL_HEADERS, headers(contentLength));
        return message;
    }

    /**
     * A request with no entity stream at all, as the local transport sends a GET.
     */
    static Message serverRequestWithoutStream(String httpMethod) {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.HTTP_REQUEST_METHOD, httpMethod);
        message.put(Message.PROTOCOL_HEADERS, new MetadataMap<String, String>());
        return message;
    }

    static ClientRequestContext clientRequest(String httpMethod) {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.HTTP_REQUEST_METHOD, httpMethod);
        message.put(Message.PROTOCOL_HEADERS, new MetadataMap<String, String>());
        return new ClientRequestContextImpl(message, true);
    }

    /**
     * A 200 response whose entity is the raw response stream, as ClientResponseFilterInterceptor builds it.
     */
    static ClientResponseContext clientResponse(byte[] body) {
        return clientResponse(body, Integer.toString(body.length));
    }

    static ClientResponseContext clientResponse(byte[] body, String contentLength) {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.RESPONSE_CODE, 200);
        message.setContent(InputStream.class, new ByteArrayInputStream(body));
        message.put(Message.PROTOCOL_HEADERS, headers(contentLength));
        ResponseImpl response = (ResponseImpl)JAXRSUtils.toResponseBuilder(200)
            .entity(message.getContent(InputStream.class)).build();
        return new ClientResponseContextImpl(response, message);
    }

    /**
     * A 200 response with no entity, so that getEntityStream() returns null.
     */
    static ClientResponseContext clientResponseWithoutEntity() {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put(Message.RESPONSE_CODE, 200);
        message.put(Message.PROTOCOL_HEADERS, new MetadataMap<String, String>());
        ResponseImpl response = (ResponseImpl)JAXRSUtils.toResponseBuilder(200).build();
        return new ClientResponseContextImpl(response, message);
    }

    /**
     * Runs the action inside an interceptor chain so that the filter sees a current message,
     * rethrowing whatever the action threw.
     */
    static void runInPhase(Message message, Action action) throws Exception {
        PhaseInterceptorChain chain = new PhaseInterceptorChain(new PhaseManagerImpl().getInPhases());
        chain.add(new AbstractPhaseInterceptor<Message>(Phase.INVOKE) {
            @Override
            public void handleMessage(Message m) throws Fault {
                try {
                    action.run();
                } catch (Exception ex) {
                    throw new Fault(ex);
                }
            }
        });
        message.setInterceptorChain(chain);
        chain.doIntercept(message);
        Exception ex = message.getContent(Exception.class);
        if (ex instanceof Fault && ex.getCause() instanceof Exception) {
            throw (Exception)ex.getCause();
        } else if (ex != null) {
            throw ex;
        }
    }

    static void runInPhase(Action action) throws Exception {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        runInPhase(message, action);
    }

    private static MetadataMap<String, String> headers(String contentLength) {
        MetadataMap<String, String> headers = new MetadataMap<>();
        headers.putSingle(HttpHeaders.CONTENT_TYPE, "application/jose");
        headers.putSingle(HttpHeaders.CONTENT_LENGTH, contentLength);
        return headers;
    }
}
