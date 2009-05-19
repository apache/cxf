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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.logging.Logger;

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
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.service.model.BindingOperationInfo;

public class JaxWsClientProxy extends org.apache.cxf.frontend.ClientProxy implements
    InvocationHandler, BindingProvider {

    public static final String THREAD_LOCAL_REQUEST_CONTEXT = "thread.local.request.context";
    
    private static final Logger LOG = LogUtils.getL7dLogger(JaxWsClientProxy.class);

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
            getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
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

        client.getRequestContext().put(Method.class.getName(), method);
        boolean isAsync = isAsync(method);

        Object result = null;
        try {
            if (isAsync) {
                result = invokeAsync(method, oi, params);
            } else {
                result = invokeSync(method, oi, params);
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
                SOAPFault soapFault = createSoapFault((SOAPBinding)getBinding(), ex);
                if (soapFault == null) {
                    throw new WebServiceException(ex);
                }
                SOAPFaultException  exception = new SOAPFaultException(soapFault);
                if (ex instanceof Fault && ex.getCause() != null) {
                    exception.initCause(ex.getCause());
                } else {
                    exception.initCause(ex);
                }
                throw exception;                
            } else {
                throw new WebServiceException(ex);
            }
        } finally {
            if (addressChanged(address)) {
                setupEndpointAddressContext(getClient().getEndpoint());
            }
        }
        
        Map<String, Object> respContext = client.getResponseContext();
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
    boolean isAsync(Method m) {
        return m.getName().endsWith("Async")
            && (Future.class.equals(m.getReturnType()) 
                || Response.class.equals(m.getReturnType()));
    }
    
    static SOAPFault createSoapFault(SOAPBinding binding, Exception ex) throws SOAPException {
        SOAPFault soapFault;
        try {
            soapFault = binding.getSOAPFactory().createFault();
        } catch (Throwable t) {
            //probably an old version of saaj or something that is not allowing createFault 
            //method to work.  Try the saaj 1.2 method of doing this.
            try {
                soapFault = binding.getMessageFactory().createMessage()
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

    @SuppressWarnings("unchecked")
    private Object invokeAsync(Method method, BindingOperationInfo oi, Object[] params) throws Exception {

        client.setExecutor(getClient().getEndpoint().getExecutor());
        
        AsyncHandler<Object> handler;
        if (params.length > 0 && params[params.length - 1] instanceof AsyncHandler) {
            handler = (AsyncHandler)params[params.length - 1];
        } else {
            handler = null;
        }
        ClientCallback callback = new JaxwsClientCallback(handler);
             
        Response<Object> ret = new JaxwsResponseCallback(callback);
        client.invoke(callback, oi, params);
        return ret;
    }

    public Map<String, Object> getRequestContext() {
        return new WrappedMessageContext(this.getClient().getRequestContext(),
                                         null,
                                         Scope.APPLICATION);
    }
    public Map<String, Object> getResponseContext() {
        return new WrappedMessageContext(this.getClient().getResponseContext(),
                                                          null,
                                                          Scope.APPLICATION);
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
