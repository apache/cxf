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
package org.apache.cxf.microprofile.client.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.ClientProxyImpl;
import org.apache.cxf.jaxrs.client.ClientState;
import org.apache.cxf.jaxrs.client.JaxrsClientCallback;
import org.apache.cxf.jaxrs.client.LocalClientState;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.microprofile.client.MicroProfileClientProviderFactory;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class MicroProfileClientProxyImpl extends ClientProxyImpl {
    private static final InvocationCallback<Object> MARKER_CALLBACK = new InvocationCallback<Object>() {

        @Override
        public void completed(Object paramRESPONSE) {
            // no-op
        }

        @Override
        public void failed(Throwable paramThrowable) {
            // no-op
        }
    };
    
    private final MPAsyncInvocationInterceptorImpl aiiImpl = new MPAsyncInvocationInterceptorImpl();
    
    public MicroProfileClientProxyImpl(URI baseURI, ClassLoader loader, ClassResourceInfo cri,
                                       boolean isRoot, boolean inheritHeaders, ExecutorService executorService,
                                       Object... varValues) {
        super(new LocalClientState(baseURI), loader, cri, isRoot, inheritHeaders, varValues);
        cfg.getRequestContext().put(EXECUTOR_SERVICE_PROPERTY, executorService);
    }

    public MicroProfileClientProxyImpl(ClientState initialState, ClassLoader loader, ClassResourceInfo cri,
                                       boolean isRoot, boolean inheritHeaders, ExecutorService executorService,
                                       Object... varValues) {
        super(initialState, loader, cri, isRoot, inheritHeaders, varValues);
        cfg.getRequestContext().put(EXECUTOR_SERVICE_PROPERTY, executorService);
    }

    

    @SuppressWarnings("unchecked")
    @Override
    protected InvocationCallback<Object> checkAsyncCallback(OperationResourceInfo ori,
                                                            Map<String, Object> reqContext,
                                                            Message outMessage) {
        if (Future.class.equals(ori.getMethodToInvoke().getReturnType())) {
            return MARKER_CALLBACK;
        }
        return outMessage.getContent(InvocationCallback.class);
    }

    
    @Override
    protected Object doInvokeAsync(OperationResourceInfo ori, Message outMessage,
                                   InvocationCallback<Object> asyncCallback) {
        outMessage.getInterceptorChain().add(aiiImpl);
        cfg.getInInterceptors().add(new MPAsyncInvocationInterceptorPostAsyncImpl(aiiImpl.getInterceptors()));
        
        super.doInvokeAsync(ori, outMessage, asyncCallback);
        
        Future<?> future = null;
        if (asyncCallback == MARKER_CALLBACK) {
            JaxrsClientCallback<?> cb = outMessage.getExchange().get(JaxrsClientCallback.class);
            future = cb.createFuture();
        }
        return future;
    }

    @Override
    protected void checkResponse(Method m, Response r, Message inMessage) throws Throwable {
        MicroProfileClientProviderFactory factory = MicroProfileClientProviderFactory.getInstance(inMessage);
        List<ResponseExceptionMapper<?>> mappers = factory.createResponseExceptionMapper(inMessage,
                Throwable.class);
        for (ResponseExceptionMapper<?> mapper : mappers) {
            if (mapper.handles(r.getStatus(), r.getHeaders())) {
                Throwable t = mapper.toThrowable(r);
                if (t instanceof RuntimeException) {
                    throw t;
                } else if (t != null && m.getExceptionTypes() != null) {
                    // its a checked exception, make sure its declared
                    for (Class<?> c : m.getExceptionTypes()) {
                        if (t.getClass().isAssignableFrom(c)) {
                            throw t;
                        }
                    }
                    // TODO Log the unhandled declarable
                }
            }
        }
    }

    @Override
    protected Class<?> getReturnType(Method method, Message outMessage) {
        if (!outMessage.getExchange().isSynchronous()) {
            InvocationCallback<?> callback = outMessage.getContent(InvocationCallback.class);
            if (callback != null) {
                Type t = getCallbackType(callback);
                if (t instanceof Class) {
                    return (Class<?>) t;
                }
            }
        }
        Class<?> returnType = super.getReturnType(method, outMessage);
        if (Future.class.isAssignableFrom(returnType)) {
            Type t = method.getGenericReturnType();
            returnType = InjectionUtils.getActualType(t);
            System.out.println("returnType (from future) = " + returnType);
        }
        System.out.println("returnType = " + returnType);
        return returnType;
    }
}
