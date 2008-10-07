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
package org.apache.cxf.jca.outbound;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.EndpointUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jca.core.logging.LoggerHelper;

/**
 * Represents a "physical" connection to EIS, which provides access to target
 * web service.  ManagedConnectionImpl creates connection handles for
 * applications to use the connection backed by this object.
 */
public class ManagedConnectionImpl implements ManagedConnection {
    private static final Logger LOG = LogUtils.getL7dLogger(ManagedConnectionImpl.class);
    
    private Set<ConnectionEventListener> listeners = 
        Collections.synchronizedSet(new HashSet<ConnectionEventListener>());
    
    private Map<Object, Subject> handles = 
        Collections.synchronizedMap(new HashMap<Object, Subject>());
    private PrintWriter printWriter;
    
    private ManagedConnectionFactoryImpl mcf;
    private ConnectionRequestInfo connReqInfo;
    private boolean isClosed;
    private Bus bus;
    private Object associatedHandle;
    
    public ManagedConnectionImpl(ManagedConnectionFactoryImpl mcf,
            ConnectionRequestInfo connReqInfo, Subject subject) {
        this.mcf = mcf;
        this.connReqInfo = connReqInfo;
    }

    /* -------------------------------------------------------------------
     * ManagedConnection Methods
     */
    
    public void addConnectionEventListener(ConnectionEventListener listener) {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("add listener : " + listener);
        }
        listeners.add(listener);
    }

    public void associateConnection(Object connection) throws ResourceException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("associate handle : " + connection);
        }
        associatedHandle = connection;
        // nothing needs to be done as app gets a copy of client proxy
    }

    public void cleanup() throws ResourceException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("cleanup");
        }
        handles.clear();
        isClosed = false;
    }

    public void destroy() throws ResourceException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("destroy");
        }
        handles.clear();
        isClosed = false;
        bus = null;
        connReqInfo = null;
    }

    public Object getConnection(Subject subject,
            ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("get handle for subject=" + subject + " cxRequestInfo="
                    + cxRequestInfo);
        }
        
        if (isClosed) {
            throw new ResourceException("connection has been closed");
        }
        
        // check request info
        if (!connReqInfo.equals(cxRequestInfo)) {
            throw new ResourceException("connection request info: " + cxRequestInfo
                    + " does not match " + connReqInfo);
        }
        
        CXFConnectionSpec spec = (CXFConnectionSpec)cxRequestInfo;

        Object handle = createConnectionHandle(spec);
        handles.put(handle, subject);
        associatedHandle = handle;
        return handle;
        
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("LocalTransaction is not supported.");
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return printWriter;
    }

    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new CXFManagedConnectionMetaData(getUserName());
    }

    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("XAResource is not supported.");
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    public void setLogWriter(PrintWriter out) throws ResourceException {
        printWriter = out;

        if (printWriter != null) {
            LoggerHelper.initializeLoggingOnWriter(printWriter);
        }
    }
    
    /* -------------------------------------------------------------------
     * Public Methods
     */

    public ConnectionRequestInfo getRequestInfo() {
        return connReqInfo;
    }
    
    public ManagedConnectionFactoryImpl getManagedConnectionFactoryImpl() {
        return mcf;
    }
    
    /* -------------------------------------------------------------------
     * Private Methods
     */
    
    private void sendEvent(final ConnectionEvent coEvent) {
        synchronized (listeners) {
            Iterator<ConnectionEventListener> iterator = listeners.iterator();
            while (iterator.hasNext()) {
                sendEventToListener(iterator.next(), coEvent);
            }
        }
    }

    private void sendEventToListener(ConnectionEventListener listener, 
            ConnectionEvent coEvent) {
        if (coEvent.getId() == ConnectionEvent.CONNECTION_CLOSED) {
            listener.connectionClosed(coEvent);
        }

        if (coEvent.getId() == ConnectionEvent.LOCAL_TRANSACTION_COMMITTED) {
            listener.localTransactionCommitted(coEvent);
        }

        if (coEvent.getId() == ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK) {
            listener.localTransactionRolledback(coEvent);
        }

        if (coEvent.getId() == ConnectionEvent.LOCAL_TRANSACTION_STARTED) {
            listener.localTransactionStarted(coEvent);
        }

        if (coEvent.getId() == ConnectionEvent.CONNECTION_ERROR_OCCURRED) {
            listener.connectionErrorOccurred(coEvent);
        }
        
    }
    
    private String getUserName() {
        if (associatedHandle != null) {
            Subject subject = handles.get(associatedHandle);
            if (subject != null) {
                return subject.toString();
            }
        } 
        return null;
        
    }

    private Object createConnectionHandle(final CXFConnectionSpec spec) {
                                
        Class<?> interfaces[] = {CXFConnection.class, BindingProvider.class, 
                spec.getServiceClass()};

        return Proxy.newProxyInstance(spec.getServiceClass().getClassLoader(), 
                interfaces, new ConnectionInvocationHandler(
                        createClientProxy(spec), spec));
    }
    
    private Object createClientProxy(final CXFConnectionSpec spec) {
        
        validateConnectionSpec(spec);
        ClientProxyFactoryBean factory = null;

        if (EndpointUtils.hasWebServiceAnnotation(spec.getServiceClass())) {
            factory = new JaxWsProxyFactoryBean();
        } else {
            factory = new ClientProxyFactoryBean();
        }
              
        factory.setBus(getBus(spec.getBusConfigURL()));
        factory.setServiceClass(spec.getServiceClass());
        factory.getServiceFactory().setEndpointName(spec.getEndpointName());
        factory.getServiceFactory().setServiceName(spec.getServiceName());
        factory.getServiceFactory().setWsdlURL(spec.getWsdlURL());

        if (spec.getAddress() != null) {
            factory.setAddress(spec.getAddress());
        }
        
        configureObject(spec.getEndpointName().toString() + ".jaxws-client.proxyFactory", factory);

        return factory.create();

    }

    private void validateConnectionSpec(CXFConnectionSpec spec) {
        if (spec.getServiceClass() == null) {
            throw new IllegalArgumentException("no serviceClass in connection spec");
        }
        
        if (spec.getEndpointName() == null) {
            throw new IllegalArgumentException("no endpointName in connection spec");
        }
        
        if (spec.getServiceName() == null) {
            throw new IllegalArgumentException("no serviceName in connection spec");
        }
        
        if (spec.getWsdlURL() == null) {
            throw new IllegalArgumentException("no wsdlURL in connection spec");
        }
    }

    private void configureObject(String name, Object instance) {
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(name, instance);
        }
    }
    
    private synchronized Bus getBus(URL busConfigLocation) {
        if (bus == null) {
            if (busConfigLocation != null) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Create bus from location " + busConfigLocation);
                }
                bus = new SpringBusFactory().createBus(busConfigLocation);
            } else if (mcf.getBusConfigURL() != null) {
                
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Create bus from URL " + mcf.getBusConfigURL());
                }
                
                URL url = null;
                try {
                    url = new URL(mcf.getBusConfigURL());
                } catch (MalformedURLException e) {
                    LOG.warning("Malformed URL " + mcf.getBusConfigURL());
                }

                if (url != null) {
                    bus = new SpringBusFactory().createBus(url);
                }
            } 
            
            if (bus == null) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Create default bus");
                }
                bus = BusFactory.getDefaultBus();
            }
        }
        return bus;
    }

    private class ConnectionInvocationHandler implements InvocationHandler {
        private Object target;
        private CXFConnectionSpec spec;
        
        ConnectionInvocationHandler(Object target, CXFConnectionSpec spec) {
            this.target = target;
            this.spec = spec;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
            
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("invoke connection spec:" + spec + " method=" + method);
            }
            
            if ("hashCode".equals(method.getName()) || "equals".equals(method.getName())) {
                return method.invoke(Proxy.getInvocationHandler(proxy), args);
                
            }
            
            if ("toString".equals(method.getName())) {
                return "ManagedConnection: " + spec;
            }
            
            if (!handles.containsKey(proxy)) {
                throw new IllegalArgumentException("Stale connection");
            }
            
            if ("getService".equals(method.getName())) {
                return handleGetServiceMethod(proxy, method, args);
            } else if ("close".equals(method.getName())) {
                return handleCloseMethod(proxy, method, args);

            } else {
                throw new IllegalArgumentException("Unhandled method " + method);
            }
        }

        private Object handleGetServiceMethod(Object proxy, Method method,
                Object[] args) {

            if (!spec.getServiceClass().equals(args[0])) {
                throw new IllegalArgumentException("serviceClass " 
                        + args[0] + " does not match " + spec.getServiceClass());
            }                                      
                                                                          
            return target;
        }
        
        private Object handleCloseMethod(Object proxy, Method method,
                Object[] args) {
            
            handles.remove(proxy);
            associatedHandle = null;
            if (handles.isEmpty()) {
                isClosed = true;
                ConnectionEvent event = new ConnectionEvent(ManagedConnectionImpl.this,
                        ConnectionEvent.CONNECTION_CLOSED);
                event.setConnectionHandle(proxy);
                sendEvent(event);
            }
            
            return null;
        }
    }
   
}
