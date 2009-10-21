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

package org.apache.cxf.endpoint;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import com.ibm.wsdl.extensions.soap.SOAPBindingImpl;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.common.i18n.UncheckedException;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.interceptor.ClientOutFaultObserver;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.wsdl11.WSDLServiceFactory;

public class ClientImpl
    extends AbstractBasicInterceptorProvider
    implements Client, Retryable, MessageObserver {

    public static final String THREAD_LOCAL_REQUEST_CONTEXT = "thread.local.request.context";


    public static final String FINISHED = "exchange.finished";

    private static final Logger LOG = LogUtils.getL7dLogger(ClientImpl.class);

    protected Bus bus;
    protected ConduitSelector conduitSelector;
    protected ClientOutFaultObserver outFaultObserver;
    protected int synchronousTimeout = 60000; // default 60 second timeout

    protected PhaseChainCache outboundChainCache = new PhaseChainCache();
    protected PhaseChainCache inboundChainCache = new PhaseChainCache();

    protected Map<String, Object> currentRequestContext = new ConcurrentHashMap<String, Object>();
    protected Map<Thread, EchoContext> requestContext 
        = Collections.synchronizedMap(new WeakHashMap<Thread, EchoContext>());

    protected Map<Thread, Map<String, Object>> responseContext 
        = Collections.synchronizedMap(new WeakHashMap<Thread, Map<String, Object>>());

    protected Executor executor;


    public ClientImpl(Bus b, Endpoint e) {
        this(b, e, (ConduitSelector)null);
    }

    public ClientImpl(Bus b, Endpoint e, Conduit c) {
       this(b, e, new PreexistingConduitSelector(c));
    }

    public ClientImpl(Bus b, Endpoint e, ConduitSelector sc) {
        bus = b;
        outFaultObserver = new ClientOutFaultObserver(bus);
        getConduitSelector(sc).setEndpoint(e);
        notifyLifecycleManager();
    }

    public ClientImpl(URL wsdlUrl) {
        this(BusFactory.getThreadDefaultBus(), wsdlUrl, null, null, SimpleEndpointImplFactory.getSingleton());
    }

    public ClientImpl(URL wsdlUrl, QName port) {
        this(BusFactory.getThreadDefaultBus(), wsdlUrl, null, port, SimpleEndpointImplFactory.getSingleton());
    }

    /**
     * Create a Client that uses the default EndpointImpl.
     * @param bus
     * @param wsdlUrl
     * @param service
     * @param port
     */
    public ClientImpl(Bus bus, URL wsdlUrl, QName service, QName port) {
        this(bus, wsdlUrl, service, port, SimpleEndpointImplFactory.getSingleton());
    }

    /**
     * Create a Client that uses a specific EndpointImpl.
     * @param bus
     * @param wsdlUrl
     * @param service
     * @param port
     * @param endpointImplFactory
     */
    public ClientImpl(Bus bus, URL wsdlUrl, QName service,
                      QName port, EndpointImplFactory endpointImplFactory) {
        this.bus = bus;
        outFaultObserver = new ClientOutFaultObserver(bus);

        WSDLServiceFactory sf = (service == null)
            ? (new WSDLServiceFactory(bus, wsdlUrl)) : (new WSDLServiceFactory(bus, wsdlUrl, service));
        Service svc = sf.create();

        EndpointInfo epfo = findEndpoint(svc, port);

        try {
            if (endpointImplFactory != null) {
                getConduitSelector().setEndpoint(endpointImplFactory.newEndpointImpl(bus, svc, epfo));
            } else {
                getConduitSelector().setEndpoint(new EndpointImpl(bus, svc, epfo));
            }
        } catch (EndpointException epex) {
            throw new IllegalStateException("Unable to create endpoint: " + epex.getMessage(), epex);
        }
        notifyLifecycleManager();
    }

    public void destroy() {
        ClientLifeCycleManager mgr = bus.getExtension(ClientLifeCycleManager.class);
        if (null != mgr) {
            mgr.clientDestroyed(this);
        }

        if (conduitSelector != null) {
            getConduit().close();
        }
    }

    private void notifyLifecycleManager() {
        ClientLifeCycleManager mgr = bus.getExtension(ClientLifeCycleManager.class);
        if (null != mgr) {
            mgr.clientCreated(this);
        }
    }

    private EndpointInfo findEndpoint(Service svc, QName port) {
        EndpointInfo epfo;
        if (port != null) {
            epfo = svc.getEndpointInfo(port);
            if (epfo == null) {
                throw new IllegalArgumentException("The service " + svc.getName()
                                                   + " does not have an endpoint " + port + ".");
            }
        } else {
            epfo = null;
            for (ServiceInfo svcfo : svc.getServiceInfos()) {
                for (EndpointInfo e : svcfo.getEndpoints()) {
                    BindingInfo bfo = e.getBinding();

                    if (bfo.getBindingId().equals("http://schemas.xmlsoap.org/wsdl/soap/")) {
                        for (Object o : bfo.getExtensors().get()) {
                            if (o instanceof SOAPBindingImpl) {
                                SOAPBindingImpl soapB = (SOAPBindingImpl)o;
                                if (soapB.getTransportURI().equals("http://schemas.xmlsoap.org/soap/http")) {
                                    epfo = e;
                                    break;
                                }
                            }
                        }

                    }
                }
            }
            if (epfo == null) {
                throw new UnsupportedOperationException(
                     "Only document-style SOAP 1.1 http are supported "
                     + "for auto-selection of endpoint; none were found.");
            }
        }
        return epfo;
    }

    public Endpoint getEndpoint() {
        return getConduitSelector().getEndpoint();
    }


    public Map<String, Object> getRequestContext() {
        if (isThreadLocalRequestContext()) {
            if (!requestContext.containsKey(Thread.currentThread())) {
                requestContext.put(Thread.currentThread(), new EchoContext(currentRequestContext));
            }
            return requestContext.get(Thread.currentThread());
        }
        return currentRequestContext;
    }
    public Map<String, Object> getResponseContext() {
        if (!responseContext.containsKey(Thread.currentThread())) {
            responseContext.put(Thread.currentThread(), new HashMap<String, Object>());
        }
        return responseContext.get(Thread.currentThread());

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


    public Object[] invoke(BindingOperationInfo oi, Object... params) throws Exception {
        return invoke(oi, params, null);
    }

    public Object[] invoke(String operationName, Object... params) throws Exception {
        QName q = new QName(getEndpoint().getService().getName().getNamespaceURI(), operationName);

        return invoke(q, params);
    }

    public Object[] invoke(QName operationName, Object... params) throws Exception {
        BindingOperationInfo op = getEndpoint().getEndpointInfo().getBinding().getOperation(operationName);
        if (op == null) {
            throw new UncheckedException(
                new org.apache.cxf.common.i18n.Message("NO_OPERATION", LOG, operationName));
        }

        if (op.isUnwrappedCapable()) {
            op = op.getUnwrappedOperation();
        }

        return invoke(op, params);
    }

    public Object[] invokeWrapped(String operationName, Object... params) throws Exception {
        QName q = new QName(getEndpoint().getService().getName().getNamespaceURI(), operationName);

        return invokeWrapped(q, params);
    }

    public Object[] invokeWrapped(QName operationName, Object... params) throws Exception {
        BindingOperationInfo op = getEndpoint().getEndpointInfo().getBinding().getOperation(operationName);
        if (op == null) {
            throw new UncheckedException(
                new org.apache.cxf.common.i18n.Message("NO_OPERATION", LOG, operationName));
        }
        return invoke(op, params);
    }

    public Object[] invoke(BindingOperationInfo oi,
                           Object[] params,
                           Exchange exchange) throws Exception {
        Map<String, Object> context = new HashMap<String, Object>();
        Map<String, Object> resp = getResponseContext();
        resp.clear();
        Map<String, Object> req = new HashMap<String, Object>(getRequestContext());
        context.put(RESPONSE_CONTEXT, resp);
        context.put(REQUEST_CONTEXT, req);
        try {
            return invoke(oi, params, context, exchange);
        } finally {
            responseContext.put(Thread.currentThread(), resp);
        }
    }
    public Object[] invoke(BindingOperationInfo oi,
                           Object[] params,
                           Map<String, Object> context) throws Exception {
        try {
            return invoke(oi, params, context, (Exchange)null);
        } finally {
            if (context != null) {
                Map<String, Object> resp = CastUtils.cast((Map<?, ?>)context.get(RESPONSE_CONTEXT));
                if (resp != null) {
                    responseContext.put(Thread.currentThread(), resp);
                }
            }
        }
    }

    public void invoke(ClientCallback callback,
                       String operationName,
                       Object... params) throws Exception {
        QName q = new QName(getEndpoint().getService().getName().getNamespaceURI(), operationName);
        invoke(callback, q, params);
    }

    public void invoke(ClientCallback callback,
                       QName operationName,
                       Object... params) throws Exception {
        BindingOperationInfo op = getEndpoint().getEndpointInfo().getBinding().getOperation(operationName);
        if (op == null) {
            throw new UncheckedException(
                new org.apache.cxf.common.i18n.Message("NO_OPERATION", LOG, operationName));
        }

        if (op.isUnwrappedCapable()) {
            op = op.getUnwrappedOperation();
        }

        invoke(callback, op, params);
    }


    public void invokeWrapped(ClientCallback callback,
                              String operationName,
                              Object... params)
        throws Exception {
        QName q = new QName(getEndpoint().getService().getName().getNamespaceURI(), operationName);
        invokeWrapped(callback, q, params);
    }

    public void invokeWrapped(ClientCallback callback,
                              QName operationName,
                              Object... params)
        throws Exception {
        BindingOperationInfo op = getEndpoint().getEndpointInfo().getBinding().getOperation(operationName);
        if (op == null) {
            throw new UncheckedException(
                new org.apache.cxf.common.i18n.Message("NO_OPERATION", LOG, operationName));
        }
        invoke(callback, op, params);
    }


    public void invoke(ClientCallback callback,
                       BindingOperationInfo oi,
                       Object... params) throws Exception {
        Bus origBus = BusFactory.getThreadDefaultBus(false);
        BusFactory.setThreadDefaultBus(bus);
        try {
            Exchange exchange = new ExchangeImpl();
            exchange.setSynchronous(false);
            Endpoint endpoint = getEndpoint();
            Map<String, Object> context = new HashMap<String, Object>();
            Map<String, Object> resp = getResponseContext();
            resp.clear();
            Map<String, Object> reqContext = new HashMap<String, Object>(getRequestContext());
            context.put(RESPONSE_CONTEXT, resp);
            context.put(REQUEST_CONTEXT, reqContext);

            Message message = endpoint.getBinding().createMessage();
            message.put(Message.INVOCATION_CONTEXT, context);

            //setup the message context
            setContext(reqContext, message);
            setParameters(params, message);

            if (null != reqContext) {
                exchange.putAll(reqContext);
            }
            exchange.setOneWay(oi.getOutput() == null);
            exchange.setOutMessage(message);
            exchange.put(ClientCallback.class, callback);

            setOutMessageProperties(message, oi);
            setExchangeProperties(exchange, endpoint, oi);

            // setup chain

            PhaseInterceptorChain chain = setupInterceptorChain(endpoint);
            message.setInterceptorChain(chain);

            chain.setFaultObserver(outFaultObserver);

            // setup conduit selector
            prepareConduitSelector(message);
            
            // add additional interceptors and such
            modifyChain(chain, message, false);

            // execute chain
            chain.doIntercept(message);

        } finally {
            BusFactory.setThreadDefaultBus(origBus);
        }
    }

    public Object[] invoke(BindingOperationInfo oi,
                           Object[] params,
                           Map<String, Object> context,
                           Exchange exchange) throws Exception {
        Bus origBus = BusFactory.getThreadDefaultBus(false);
        BusFactory.setThreadDefaultBus(bus);
        try {
            if (exchange == null) {
                exchange = new ExchangeImpl();
            }
            exchange.setSynchronous(true);
            Endpoint endpoint = getEndpoint();

            Map<String, Object> reqContext = null;
            Map<String, Object> resContext = null;
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Invoke, operation info: " + oi + ", params: " + params);
            }
            Message message = endpoint.getBinding().createMessage();
            if (null != context) {
                reqContext = CastUtils.cast((Map)context.get(REQUEST_CONTEXT));
                resContext = CastUtils.cast((Map)context.get(RESPONSE_CONTEXT));
                message.put(Message.INVOCATION_CONTEXT, context);
            }
            //setup the message context
            setContext(reqContext, message);
            setParameters(params, message);

            if (null != reqContext) {
                exchange.putAll(reqContext);
            }

            if (null != oi) {
                exchange.setOneWay(oi.getOutput() == null);
            }

            exchange.setOutMessage(message);

            setOutMessageProperties(message, oi);
            setExchangeProperties(exchange, endpoint, oi);

            // setup chain

            PhaseInterceptorChain chain = setupInterceptorChain(endpoint);
            message.setInterceptorChain(chain);

            chain.setFaultObserver(outFaultObserver);

            // setup conduit selector
            prepareConduitSelector(message);

            // add additional interceptors and such
            modifyChain(chain, message, false);
            
            // execute chain
            chain.doIntercept(message);

            return processResult(message, exchange, oi, resContext);

        } finally {
            BusFactory.setThreadDefaultBus(origBus);
        }
    }

    protected Object[] processResult(Message message,
                                   Exchange exchange,
                                   BindingOperationInfo oi,
                                   Map<String, Object> resContext) throws Exception {
     // Check to see if there is a Fault from the outgoing chain
        Exception ex = message.getContent(Exception.class);
        boolean mepCompleteCalled = false;
        if (ex != null) {
            getConduitSelector().complete(exchange);
            mepCompleteCalled = true;
            if (message.getContent(Exception.class) != null) {
                throw ex;
            }
        }
        ex = message.getExchange().get(Exception.class);
        if (ex != null) {
            if (!mepCompleteCalled) {
                getConduitSelector().complete(exchange);
            }
            throw ex;
        }

        // Wait for a response if we need to
        if (oi != null && !oi.getOperationInfo().isOneWay()) {
            synchronized (exchange) {
                waitResponse(exchange);
            }
        }

        // leave the input stream open for the caller
        Boolean keepConduitAlive = (Boolean)exchange.get(Client.KEEP_CONDUIT_ALIVE);
        if (keepConduitAlive == null || !keepConduitAlive) {
            getConduitSelector().complete(exchange);
        }

        // Grab the response objects if there are any
        List resList = null;
        Message inMsg = exchange.getInMessage();
        if (inMsg != null) {
            if (null != resContext) {
                resContext.putAll(inMsg);
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("set responseContext to be" + responseContext);
                }
            }
            resList = inMsg.getContent(List.class);
        }

        // check for an incoming fault
        ex = getException(exchange);

        if (ex != null) {
            throw ex;
        }

        if (resList != null) {
            return resList.toArray();
        }

        return null;
    }
    protected Exception getException(Exchange exchange) {
        if (exchange.getInFaultMessage() != null) {
            return exchange.getInFaultMessage().getContent(Exception.class);
        } else if (exchange.getOutFaultMessage() != null) {
            return exchange.getOutFaultMessage().getContent(Exception.class);
        } else if (exchange.getInMessage() != null) {
            return exchange.getInMessage().getContent(Exception.class);
        }
        return null;
    }

    protected void setContext(Map<String, Object> ctx, Message message) {
        if (ctx != null) {
            message.putAll(ctx);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("set requestContext to message be" + ctx);
            }
        }
    }

    protected void waitResponse(Exchange exchange) {
        int remaining = synchronousTimeout;
        while (!Boolean.TRUE.equals(exchange.get(FINISHED)) && remaining > 0) {
            long start = System.currentTimeMillis();
            try {
                exchange.wait(remaining);
            } catch (InterruptedException ex) {
                // ignore
            }
            long end = System.currentTimeMillis();
            remaining -= (int)(end - start);
        }
        if (!Boolean.TRUE.equals(exchange.get(FINISHED))) {
            LogUtils.log(LOG, Level.WARNING, "RESPONSE_TIMEOUT",
                exchange.get(OperationInfo.class).getName().toString());
        }
    }

    protected void setParameters(Object[] params, Message message) {
        MessageContentsList contents = new MessageContentsList(params);
        message.setContent(List.class, contents);
    }

    public void onMessage(Message message) {

        Endpoint endpoint = message.getExchange().get(Endpoint.class);
        if (endpoint == null) {
            // in this case correlation will occur outside the transport,
            // however there's a possibility that the endpoint may have been
            // rebased in the meantime, so that the response will be mediated
            // via a set of in interceptors provided by a *different* endpoint
            //
            endpoint = getConduitSelector().getEndpoint();
            message.getExchange().put(Endpoint.class, endpoint);
        }
        message = endpoint.getBinding().createMessage(message);
        message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        message.put(Message.INBOUND_MESSAGE, Boolean.TRUE);
        PhaseManager pm = bus.getExtension(PhaseManager.class);

        List<Interceptor> i1 = bus.getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by bus: " + i1);
        }
        List<Interceptor> i2 = getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by client: " + i2);
        }
        List<Interceptor> i3 = endpoint.getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by endpoint: " + i2);
        }
        List<Interceptor> i4 = endpoint.getBinding().getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by binding: " + i4);
        }

        PhaseInterceptorChain chain;
        if (endpoint.getService().getDataBinding() instanceof InterceptorProvider) {
            InterceptorProvider p = (InterceptorProvider)endpoint.getService().getDataBinding();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Interceptors contributed by databinging: " + p.getInInterceptors());
            }
            chain = inboundChainCache.get(pm.getInPhases(), i1, i2, i3, i4,
                                          p.getInInterceptors());
        } else {
            chain = inboundChainCache.get(pm.getInPhases(), i1, i2, i3, i4);
        }
        message.setInterceptorChain(chain);

        chain.setFaultObserver(outFaultObserver);
        modifyChain(chain, message, true);
        modifyChain(chain, message.getExchange().getOutMessage(), true);
        
        Bus origBus = BusFactory.getThreadDefaultBus(false);
        BusFactory.setThreadDefaultBus(bus);
        // execute chain
        ClientCallback callback = message.getExchange().get(ClientCallback.class);
        try {
            if (callback != null) {
                if (callback.isCancelled()) {
                    getConduitSelector().complete(message.getExchange());
                    return;
                }
                callback.start(message);
            }

            String startingAfterInterceptorID = (String) message.get(
                PhaseInterceptorChain.STARTING_AFTER_INTERCEPTOR_ID);
            String startingInterceptorID = (String) message.get(
                PhaseInterceptorChain.STARTING_AT_INTERCEPTOR_ID);
            if (startingAfterInterceptorID != null) {
                chain.doInterceptStartingAfter(message, startingAfterInterceptorID);
            } else if (startingInterceptorID != null) {
                chain.doInterceptStartingAt(message, startingInterceptorID);
            } else if (message.getContent(Exception.class) != null) {
                outFaultObserver.onMessage(message);
            } else {
                chain.doIntercept(message);
            }

            callback = message.getExchange().get(ClientCallback.class);

            if (callback != null && !isPartialResponse(message)) {
                message.getExchange().setInMessage(message);
                Map<String, Object> resCtx = CastUtils.cast((Map<?, ?>)message
                                                                .getExchange()
                                                                .getOutMessage()
                                                                .get(Message.INVOCATION_CONTEXT));
                resCtx = CastUtils.cast((Map<?, ?>)resCtx.get(RESPONSE_CONTEXT));

                try {
                    Object obj[] = processResult(message, message.getExchange(),
                                                 null, resCtx);

                    callback.handleResponse(resCtx, obj);
                } catch (Throwable ex) {
                    callback.handleException(resCtx, ex);
                }
            }
        } finally {
            synchronized (message.getExchange()) {
                if (!isPartialResponse(message) && callback == null) {
                    message.getExchange().put(FINISHED, Boolean.TRUE);
                    message.getExchange().setInMessage(message);
                    message.getExchange().notifyAll();
                }
            }
            BusFactory.setThreadDefaultBus(origBus);
        }
    }

    public Conduit getConduit() {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        setExchangeProperties(exchange, null, null);
        return getConduitSelector().selectConduit(message);
    }

    protected void prepareConduitSelector(Message message) {
        getConduitSelector().prepare(message);
        message.getExchange().put(ConduitSelector.class, getConduitSelector());
    }

    protected void setOutMessageProperties(Message message, BindingOperationInfo boi) {
        message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        message.put(Message.INBOUND_MESSAGE, Boolean.FALSE);
        if (null != boi) {
            message.put(BindingMessageInfo.class, boi.getInput());
            message.put(MessageInfo.class, boi.getOperationInfo().getInput());
        }
    }

    protected void setExchangeProperties(Exchange exchange,
                                         Endpoint endpoint,
                                         BindingOperationInfo boi) {
        if (endpoint != null) {
            exchange.put(Endpoint.class, endpoint);
            exchange.put(Service.class, endpoint.getService());
            if (endpoint.getEndpointInfo().getService() != null) {
                exchange.put(ServiceInfo.class, endpoint.getEndpointInfo().getService());
                exchange.put(InterfaceInfo.class, endpoint.getEndpointInfo().getService().getInterface());
            }
            exchange.put(Binding.class, endpoint.getBinding());
            exchange.put(BindingInfo.class, endpoint.getEndpointInfo().getBinding());
        }
        if (boi != null) {
            exchange.put(BindingOperationInfo.class, boi);
            exchange.put(OperationInfo.class, boi.getOperationInfo());
        }

        if (exchange.isSynchronous() || executor == null) {
            exchange.put(MessageObserver.class, this);
        } else {
            exchange.put(MessageObserver.class, new MessageObserver() {
                public void onMessage(final Message message) {
                    executor.execute(new Runnable() {
                        public void run() {
                            ClientImpl.this.onMessage(message);
                        }
                    });
                }
            });
        }
        exchange.put(Retryable.class, this);
        exchange.put(Client.class, this);
        exchange.put(Bus.class, bus);

        if (endpoint != null && boi != null) {

            EndpointInfo endpointInfo = endpoint.getEndpointInfo();
            exchange.put(Message.WSDL_OPERATION, boi.getName());

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

    protected PhaseInterceptorChain setupInterceptorChain(Endpoint endpoint) {

        PhaseManager pm = bus.getExtension(PhaseManager.class);

        List<Interceptor> i1 = bus.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by bus: " + i1);
        }
        List<Interceptor> i2 = getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by client: " + i2);
        }
        List<Interceptor> i3 = endpoint.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by endpoint: " + i3);
        }
        List<Interceptor> i4 = endpoint.getBinding().getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by binding: " + i4);
        }
        List<Interceptor> i5 = null;
        if (endpoint.getService().getDataBinding() instanceof InterceptorProvider) {
            i5 = ((InterceptorProvider)endpoint.getService().getDataBinding()).getOutInterceptors();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Interceptors contributed by databinding: " + i5);
            }
        }
        if (i5 != null) {
            return outboundChainCache.get(pm.getOutPhases(), i1, i2, i3, i4, i5);
        }
        return outboundChainCache.get(pm.getOutPhases(), i1, i2, i3, i4);
    }

    protected void modifyChain(InterceptorChain chain, Message ctx, boolean in) {
        if (ctx == null) {
            return;
        }
        Collection<InterceptorProvider> providers 
            = CastUtils.cast((Collection<?>)ctx.get(Message.INTERCEPTOR_PROVIDERS));
        if (providers != null) {
            for (InterceptorProvider p : providers) {
                if (in) {
                    chain.add(p.getInInterceptors());
                } else {
                    chain.add(p.getOutInterceptors());
                }
            }
        }
        String key = in ? Message.IN_INTERCEPTORS : Message.OUT_INTERCEPTORS;
        Collection<Interceptor> is = CastUtils.cast((Collection<?>)ctx.get(key));
        if (is != null) {
            chain.add(is);
        }
    }

    protected void setEndpoint(Endpoint e) {
        getConduitSelector().setEndpoint(e);
    }

    public int getSynchronousTimeout() {
        return synchronousTimeout;
    }

    public void setSynchronousTimeout(int synchronousTimeout) {
        this.synchronousTimeout = synchronousTimeout;
    }

    public final ConduitSelector getConduitSelector() {
        return getConduitSelector(null);
    }

    protected final synchronized ConduitSelector getConduitSelector(
        ConduitSelector override
    ) {
        if (null == conduitSelector) {
            setConduitSelector(override != null
                               ? override
                               : new UpfrontConduitSelector());
        }
        return conduitSelector;
    }

    public final void setConduitSelector(ConduitSelector selector) {
        conduitSelector = selector;
    }

    private boolean isPartialResponse(Message in) {
        return Boolean.TRUE.equals(in.get(Message.PARTIAL_RESPONSE_MESSAGE));
    }


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


    public void setExecutor(Executor executor) {
        this.executor = executor;
    }


}
