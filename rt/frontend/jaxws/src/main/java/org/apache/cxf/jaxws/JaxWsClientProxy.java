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

package org.apache.cxf.jaxws;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.http.HTTPException;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.jaxws.support.ContextPropertiesMapping;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.service.model.BindingOperationInfo;

public class JaxWsClientProxy extends org.apache.cxf.frontend.ClientProxy implements
    InvocationHandler, BindingProvider {
 
    /*
     * modification are echoed back to the shared map
     */
    public static class EchoContext extends HashMap<String, Object> {
        final Map<String, Object> shared; 
        public EchoContext(Map<String, Object> sharedMap) {
            super(sharedMap);
            shared = sharedMap;
        }

        public Object put(String key, Object value) {
            shared.put(key, value);
            return super.put(key, value);
        }

        public void putAll(Map<? extends String, ? extends Object> t) {
            shared.putAll(t);
            super.putAll(t);
        }
        
        public Object remove(Object key) {
            shared.remove(key);
            return super.remove(key);
        }
        
        public void reload() {
            super.clear();
            super.putAll(shared);
        }
    }

    public static final String THREAD_LOCAL_REQUEST_CONTEXT = "thread.local.request.context";
    private static final Logger LOG = LogUtils.getL7dLogger(JaxWsClientProxy.class);

    protected Map<String, Object> currentRequestContext = new ConcurrentHashMap<String, Object>();
    protected ThreadLocal <EchoContext> requestContext =
        new ThreadLocal<EchoContext>();

    protected ThreadLocal <Map<String, Object>> responseContext =
            new ThreadLocal<Map<String, Object>>();

    private final Binding binding;
    private final EndpointReferenceBuilder builder;

    public JaxWsClientProxy(Client c, Binding b) {
        super(c);
        this.binding = b;
        setupEndpointAddressContext(getClient().getEndpoint());
        this.builder = new EndpointReferenceBuilder((JaxWsEndpointImpl)getClient().getEndpoint());
    }

    private void setupEndpointAddressContext(Endpoint endpoint) {
        // NOTE for jms transport the address would be null
        if (null != endpoint && null != endpoint.getEndpointInfo().getAddress()) {
            currentRequestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                                      endpoint.getEndpointInfo().getAddress());
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Endpoint endpoint = getClient().getEndpoint();
        String address = endpoint.getEndpointInfo().getAddress();
        MethodDispatcher dispatcher = (MethodDispatcher)endpoint.getService().get(
                                                                                  MethodDispatcher.class
                                                                                      .getName());
        Object[] params = args;
        if (null == params) {
            params = new Object[0];
        }        
        
        BindingOperationInfo oi = dispatcher.getBindingOperation(method, endpoint);
        if (oi == null) {
            // check for method on BindingProvider and Object
            if (method.getDeclaringClass().equals(BindingProvider.class)
                || method.getDeclaringClass().equals(BindingProviderImpl.class)
                || method.getDeclaringClass().equals(Object.class)) {
                try {
                    return method.invoke(this, params);
                } catch (InvocationTargetException e) {
                    throw e.fillInStackTrace().getCause();
                }
            }

            Message msg = new Message("NO_BINDING_OPERATION_INFO", LOG, method.getName());
            throw new WebServiceException(msg.toString());
        }

        Map<String, Object> reqContext = this.getRequestContextCopy();        
        Map<String, Object> respContext = this.getResponseContext();
        
        // Clear the response context's hold information
        // Not call the clear Context is to avoid the error 
        // that getResponseContext() could be called by Client code first
        respContext.clear();
        
        Map<String, Object> context = new HashMap<String, Object>();

        // need to do context mapping from jax-ws to cxf message
        ContextPropertiesMapping.mapRequestfromJaxws2Cxf(reqContext);

        context.put(Client.REQUEST_CONTEXT, reqContext);
        context.put(Client.RESPONSE_CONTEXT, respContext);
       
        reqContext.put(Method.class.getName(), method);
        reqContext.put(Client.REQUEST_METHOD, method);
        boolean isAsync = method.getName().endsWith("Async");

        Object result = null;
        try {
            if (isAsync) {
                result = invokeAsync(method, oi, params, context);
            } else {
                result = invokeSync(method, oi, params, context);
            }
        } catch (WebServiceException wex) {
            throw wex.fillInStackTrace();
        } catch (Exception ex) {
            for (Class<?> excls : method.getExceptionTypes()) {
                if (excls.isInstance(ex)) {
                    throw ex.fillInStackTrace();
                }
            }
            
            if (getBinding() instanceof HTTPBinding) {
                HTTPException exception = new HTTPException(HttpURLConnection.HTTP_INTERNAL_ERROR);
                exception.initCause(ex);
                throw exception;
            } else if (getBinding() instanceof SOAPBinding) {
                SOAPFault soapFault = createSoapFault(ex);
                if (soapFault == null) {
                    throw new WebServiceException(ex);
                }
                
                SOAPFaultException  exception = new SOAPFaultException(soapFault);
                exception.initCause(ex);
                throw exception;                
            } else {
                throw new WebServiceException(ex);
            }
        } finally {
            if (addressChanged(address)) {
                setupEndpointAddressContext(getClient().getEndpoint());
            }
        }
        
        // need to do context mapping from cxf message to jax-ws
        ContextPropertiesMapping.mapResponsefromCxf2Jaxws(respContext);
        Map<String, Scope> scopes = CastUtils.cast((Map<?, ?>)respContext.get(WrappedMessageContext.SCOPES));
        if (scopes != null) {
            for (Map.Entry<String, Scope> scope : scopes.entrySet()) {
                if (scope.getValue() == Scope.HANDLER) {
                    respContext.remove(scope.getKey());
                }
            }
        }
        return result;

    }
    
    private SOAPFault createSoapFault(Exception ex) throws SOAPException {
        SOAPFault soapFault;
        try {
            soapFault = ((SOAPBinding)getBinding()).getSOAPFactory().createFault();
        } catch (Throwable t) {
            //probably an old version of saaj or something that is not allowing createFault 
            //method to work.  Try the saaj 1.2 method of doing this.
            try {
                soapFault = ((SOAPBinding)getBinding()).getMessageFactory().createMessage()
                    .getSOAPBody().addFault();
            } catch (Throwable t2) {
                //still didn't work, we'll just throw what we have
                return null;
            }                        
        }
        
        if (ex instanceof SoapFault) {
            soapFault.setFaultString(((SoapFault)ex).getReason());
            soapFault.setFaultCode(((SoapFault)ex).getFaultCode());
            soapFault.setFaultActor(((SoapFault)ex).getRole());

            Node nd = soapFault.getOwnerDocument().importNode(((SoapFault)ex).getOrCreateDetail(),
                                                              true);
            nd = nd.getFirstChild();
            soapFault.addDetail();
            while (nd != null) {
                Node next = nd.getNextSibling();
                soapFault.getDetail().appendChild(nd);
                nd = next;
            }

        } else {
            try {
                soapFault.setFaultCode(new QName("http://cxf.apache.org/faultcode", "HandlerFault"));
            } catch (SOAPException ex2) {
                //ignore
            }
            String msg = ex.getMessage();
            if (msg != null) {
                soapFault.setFaultString(msg);
            }
        }      
        return soapFault;
    }

    private boolean addressChanged(String address) {
        return !(address == null
                 || getClient().getEndpoint().getEndpointInfo() == null
                 || address.equals(getClient().getEndpoint().getEndpointInfo().getAddress()));
    }

    private Object invokeAsync(Method method, BindingOperationInfo oi, Object[] params,
                               Map<String, Object> context) {

        FutureTask<Object> f = new FutureTask<Object>(new JAXWSAsyncCallable(this, method, oi, params,
                                                                             context));

        Endpoint endpoint = getClient().getEndpoint();
        endpoint.getExecutor().execute(f);
        Response<?> r = new AsyncResponse<Object>(f, Object.class);
        if (params.length > 0 && params[params.length - 1] instanceof AsyncHandler) {
            // callback style
            AsyncCallbackFuture callback = 
                new AsyncCallbackFuture(r, (AsyncHandler)params[params.length - 1]);
            endpoint.getExecutor().execute(callback);
            return callback;
        } else {
            return r;
        }
    }

    public boolean isThreadLocalRequestContext() {
        if (currentRequestContext.containsKey(THREAD_LOCAL_REQUEST_CONTEXT)) {
            Object o = currentRequestContext.get(THREAD_LOCAL_REQUEST_CONTEXT);
            boolean local = false;
            if (o instanceof Boolean) {
                local = ((Boolean)o).booleanValue();
            } else {
                local = Boolean.parseBoolean(o.toString());
            }
            return local;
        }
        return false;
    }
    public void setThreadLocalRequestContext(boolean b) {
        currentRequestContext.put(THREAD_LOCAL_REQUEST_CONTEXT, b);
    }

    
    private Map<String, Object> getRequestContextCopy() {
        Map<String, Object> realMap = new HashMap<String, Object>();
        WrappedMessageContext ctx = new WrappedMessageContext(realMap,
                                                              Scope.APPLICATION);
        // thread local contexts reflect currentRequestContext as of 
        // last call to getRequestContext()
        if (isThreadLocalRequestContext()
            && null != requestContext.get()) {
            ctx.putAll(requestContext.get());
        } else {
            ctx.putAll(currentRequestContext);
        }
        return realMap;
    }

    public Map<String, Object> getRequestContext() {
        if (isThreadLocalRequestContext()) {
            if (null == requestContext.get()) {
                requestContext.set(new EchoContext(currentRequestContext));
            }
            return requestContext.get();
        }
        return currentRequestContext;
    }

    public Map<String, Object> getResponseContext() {
        if (null == responseContext.get()) {
            responseContext.set(new HashMap<String, Object>());
        }        
        return responseContext.get();
    }

    public Binding getBinding() {
        return binding;
    }

    public EndpointReference getEndpointReference() {
        return builder.getEndpointReference();
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
        return builder.getEndpointReference(clazz);
    }    
}
