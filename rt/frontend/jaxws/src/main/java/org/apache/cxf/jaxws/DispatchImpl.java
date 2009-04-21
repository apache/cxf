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

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.http.HTTPException;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.UpfrontConduitSelector;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.jaxws.handler.logical.DispatchLogicalHandlerInterceptor;
import org.apache.cxf.jaxws.handler.soap.DispatchSOAPHandlerInterceptor;
import org.apache.cxf.jaxws.interceptors.DispatchInDatabindingInterceptor;
import org.apache.cxf.jaxws.interceptors.DispatchOutDatabindingInterceptor;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.workqueue.WorkQueueManager;

public class DispatchImpl<T> extends BindingProviderImpl implements Dispatch<T>, MessageObserver {
    private static final Logger LOG = LogUtils.getL7dLogger(DispatchImpl.class);
    private static final String FINISHED = "exchange.finished";

    private Bus bus;
    private InterceptorProvider iProvider;
    private final Class<T> cl;
    private Executor executor;
    private JAXBContext context;
    private final Service.Mode mode;

    private ConduitSelector conduitSelector;
    private final DispatchOutDatabindingInterceptor outInterceptor;
    private final DispatchInDatabindingInterceptor inInterceptor;
    
    DispatchImpl(Bus b, Client client, Service.Mode m,
                 JAXBContext ctx, Class<T> clazz, Executor e) {
        super((JaxWsEndpointImpl)client.getEndpoint());
        bus = b;
        this.iProvider = client;
        executor = e;
        context = ctx;
        cl = clazz;
        mode = m;
        getConduitSelector().setEndpoint(client.getEndpoint());
        setupEndpointAddressContext(client.getEndpoint());
        outInterceptor = new DispatchOutDatabindingInterceptor(mode);
        inInterceptor = new DispatchInDatabindingInterceptor(cl, mode);
    }
    
    DispatchImpl(Bus b, Client cl, Service.Mode m, Class<T> clazz, Executor e) {
        this(b, cl, m, null, clazz, e);
    }

    private void setupEndpointAddressContext(Endpoint endpoint) {
        //NOTE for jms transport the address would be null
        if (null != endpoint
            && null != endpoint.getEndpointInfo().getAddress()) {
            Map<String, Object> requestContext = this.getRequestContext();
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                           endpoint.getEndpointInfo().getAddress());
        }    
    }
    public T invoke(T obj) {
        return invoke(obj, false);
    }

    public T invoke(T obj, boolean isOneWay) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Dispatch: invoke called");
        }

        Bus origBus = BusFactory.getThreadDefaultBus(false);
        BusFactory.setThreadDefaultBus(bus);
        try { 
            Endpoint endpoint = getEndpoint();
            Message message = endpoint.getBinding().createMessage();
    
            if (context != null) {
                message.setContent(JAXBContext.class, context);
            }
            
            
            Map<String, Object> reqContext = new HashMap<String, Object>();
            WrappedMessageContext ctx = new WrappedMessageContext(reqContext,
                                                                  null,
                                                                  Scope.APPLICATION);
            ctx.putAll(this.getRequestContext());
            Map<String, Object> respContext = this.getResponseContext();
            // clear the response context's hold information
            // Not call the clear Context is to avoid the error 
            // that getResponseContext() would be called by Client code first
            respContext.clear();
            
            message.putAll(reqContext);
            //need to do context mapping from jax-ws to cxf message
            
            Exchange exchange = new ExchangeImpl();
            exchange.setOneWay(isOneWay);
    
            exchange.setOutMessage(message);
            setExchangeProperties(exchange, endpoint);
    
            message.setContent(Object.class, obj);
            
            if (obj instanceof SOAPMessage) {
                message.setContent(SOAPMessage.class, obj);
            } else if (obj instanceof Source) {
                message.setContent(Source.class, obj);
            } else if (obj instanceof DataSource) {
                message.setContent(DataSource.class, obj);
            }
      
            message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
    
            PhaseInterceptorChain chain = getDispatchOutChain(endpoint);
            message.setInterceptorChain(chain);
    
            // setup conduit selector
            prepareConduitSelector(message);
            
            // execute chain
            chain.doIntercept(message);
            
            Exception exp = message.getContent(Exception.class);
            if (exp == null && exchange.getInMessage() != null) {
                exp = exchange.getInMessage().getContent(Exception.class);
            }

            if (exp != null) {
                getConduitSelector().complete(exchange);
                if (getBinding() instanceof SOAPBinding && exp instanceof Fault) {
                    try {
                        SOAPFault soapFault = SOAPFactory.newInstance().createFault();
                        Fault fault = (Fault)exp;
                        soapFault.setFaultCode(fault.getFaultCode());
                        soapFault.setFaultString(fault.getMessage());
                        if (fault.getDetail() != null) {
                            Detail det = soapFault.addDetail();
                            Element fd = fault.getDetail();
                            Node child = fd.getFirstChild();
                            while (child != null) {
                                Node next = child.getNextSibling();
                                det.appendChild(det.getOwnerDocument().importNode(child, true));
                                child = next;
                            }
                        }
                        SOAPFaultException ex = new SOAPFaultException(soapFault);
                        ex.initCause(exp);
                        throw ex;
                    } catch (SOAPException e) {
                        throw new WebServiceException(e);
                    }
                } else if (getBinding() instanceof HTTPBinding) {
                    HTTPException exception = new HTTPException(HttpURLConnection.HTTP_INTERNAL_ERROR);
                    exception.initCause(exp);
                    throw exception;
                } else {
                    throw new WebServiceException(exp);
                }
            }
    
            // correlate response        
            if (getConduitSelector().selectConduit(message).getBackChannel() != null) {
                // process partial response and wait for decoupled response
            } else {
                // process response: send was synchronous so when we get here we can assume that the 
                // Exchange's inbound message is set and had been passed through the inbound
                // interceptor chain.
            }
    
            if (!isOneWay) {
                synchronized (exchange) {
                    Message inMsg = waitResponse(exchange);
                    respContext.putAll(inMsg);
                    getConduitSelector().complete(exchange);
                    return cl.cast(inMsg.getContent(Object.class));
                }
            }
            return null;
        } finally {
            BusFactory.setThreadDefaultBus(origBus);
        }        
    }

    private Message waitResponse(Exchange exchange) {
        while (!Boolean.TRUE.equals(exchange.get(FINISHED))) {
            try {
                exchange.wait();
            } catch (InterruptedException e) {
                //TODO - timeout
            }
        }
        Message inMsg = exchange.getInMessage();
        if (inMsg == null) {
            try {
                exchange.wait();
            } catch (InterruptedException e) {
                //TODO - timeout
            }
            inMsg = exchange.getInMessage();
        }
        if (inMsg.getContent(Exception.class) != null) {
            //TODO - exceptions 
            throw new RuntimeException(inMsg.getContent(Exception.class));
        }
        return inMsg;
    }

    private PhaseInterceptorChain getDispatchOutChain(Endpoint endpoint) {
        PhaseManager pm = bus.getExtension(PhaseManager.class);
        PhaseInterceptorChain chain = new PhaseInterceptorChain(pm.getOutPhases());

        List<Interceptor> il = bus.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by bus: " + il);
        }
        chain.add(il);
        List<Interceptor> i2 = iProvider.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by client: " + i2);
        }
        chain.add(i2);
        
        if (endpoint instanceof JaxWsEndpointImpl) {
            Binding jaxwsBinding = ((JaxWsEndpointImpl)endpoint).getJaxwsBinding();
            if (endpoint.getBinding() instanceof SoapBinding) {
                chain.add(new DispatchSOAPHandlerInterceptor(jaxwsBinding));
            } else {
                // TODO: what for non soap bindings?
            }       
            chain.add(new DispatchLogicalHandlerInterceptor(jaxwsBinding));
        }

        if (getBinding() instanceof SOAPBinding) {
            chain.add(new SoapPreProtocolOutInterceptor());
        }

        chain.add(new MessageSenderInterceptor());

        chain.add(outInterceptor);
        return chain;
    }

    public void onMessage(Message message) {
        Endpoint endpoint = getEndpoint();
        message = endpoint.getBinding().createMessage(message);

        message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);

        PhaseManager pm = bus.getExtension(PhaseManager.class);
        PhaseInterceptorChain chain = new PhaseInterceptorChain(pm.getInPhases());
        message.setInterceptorChain(chain);

        List<Interceptor> il = bus.getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by bus: " + il);
        }
        chain.add(il);
        List<Interceptor> i2 = iProvider.getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by client: " + i2);
        }
        chain.add(i2);

        if (endpoint instanceof JaxWsEndpointImpl) {
            Binding jaxwsBinding = ((JaxWsEndpointImpl)endpoint).getJaxwsBinding();
            if (endpoint.getBinding() instanceof SoapBinding) {
                chain.add(new DispatchSOAPHandlerInterceptor(jaxwsBinding));
            }      
            DispatchLogicalHandlerInterceptor slhi 
                = new DispatchLogicalHandlerInterceptor(jaxwsBinding, Phase.USER_LOGICAL);            
            chain.add(slhi);
        }

        chain.add(inInterceptor);

        // execute chain
        Bus origBus = BusFactory.getThreadDefaultBus(false);
        BusFactory.setThreadDefaultBus(bus);
        try {
            chain.doIntercept(message);
        } finally {
            synchronized (message.getExchange()) {
                message.getExchange().put(FINISHED, Boolean.TRUE);
                message.getExchange().setInMessage(message);
                message.getExchange().notifyAll();
            }
            BusFactory.setThreadDefaultBus(origBus);
        }
    }

    private Executor getExecutor() {
        if (executor == null) {
            executor = bus.getExtension(WorkQueueManager.class).getAutomaticWorkQueue();
        }
        if (executor == null) {
            System.err.println("Can't not get executor");
        }
        return executor;
    }
    
    private Endpoint getEndpoint() {
        return getConduitSelector().getEndpoint();
    }

    public Future<?> invokeAsync(T obj, AsyncHandler<T> asyncHandler) {
        FutureTask<T> f = new FutureTask<T>(new DispatchAsyncCallable<T>(this, obj, asyncHandler));
        getExecutor().execute(f);
        
        return f;
    }

    public Response<T> invokeAsync(T obj) {
        FutureTask<T> f = new FutureTask<T>(new DispatchAsyncCallable<T>(this, obj, null));

        getExecutor().execute(f);
        return new AsyncResponse<T>(f, cl);
    }

    public void invokeOneWay(T obj) {
        invoke(obj, true);
    }
        
    public synchronized ConduitSelector getConduitSelector() {
        if (null == conduitSelector) {
            conduitSelector = new UpfrontConduitSelector();
        }
        return conduitSelector;
    }

    public void setConduitSelector(ConduitSelector selector) {
        conduitSelector = selector;
    }
    
    protected void prepareConduitSelector(Message message) {
        message.getExchange().put(ConduitSelector.class, getConduitSelector());
    }
    
    protected void setExchangeProperties(Exchange exchange, Endpoint endpoint) {
        exchange.put(Service.Mode.class, mode);
        exchange.put(Class.class, cl);
        exchange.put(org.apache.cxf.service.Service.class, endpoint.getService());
        exchange.put(Endpoint.class, endpoint);
        
        exchange.put(MessageObserver.class, this);
        exchange.put(Bus.class, bus);

        if (endpoint != null) {

            EndpointInfo endpointInfo = endpoint.getEndpointInfo();

            QName serviceQName = endpointInfo.getService().getName();
            exchange.put(Message.WSDL_SERVICE, serviceQName);

            QName interfaceQName = endpointInfo.getService().getInterface().getName();
            exchange.put(Message.WSDL_INTERFACE, interfaceQName);

            QName portQName = endpointInfo.getName();
            exchange.put(Message.WSDL_PORT, portQName);
            URI wsdlDescription = endpointInfo.getProperty("URI", URI.class);
            if (wsdlDescription == null) {
                String address = endpointInfo.getAddress();
                try {
                    wsdlDescription = new URI(address + "?wsdl");
                } catch (URISyntaxException e) {
                    // do nothing
                }
                endpointInfo.setProperty("URI", wsdlDescription);
            }
            exchange.put(Message.WSDL_DESCRIPTION, wsdlDescription);
        }      
    }
}
