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
package org.apache.cxf.jca.cxf;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ResourceBundle;

import javax.jws.WebService;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.connector.Connection;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jca.core.resourceadapter.AbstractManagedConnectionImpl;
import org.apache.cxf.jca.core.resourceadapter.ResourceAdapterInternalException;
import org.apache.cxf.jca.cxf.handlers.InvocationHandlerFactory;

public class ManagedConnectionImpl 
    extends AbstractManagedConnectionImpl 
    implements CXFManagedConnection, Connection {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(ConnectionFactoryImpl.class);   

    private InvocationHandlerFactory handlerFactory;    
    private Object cxfService;
    private boolean connectionHandleActive;

    public ManagedConnectionImpl(ManagedConnectionFactoryImpl managedFactory, ConnectionRequestInfo crInfo,
                                 Subject subject) throws ResourceException {
        super(managedFactory, crInfo, subject);
    }

    public void associateConnection(Object connection) throws ResourceException {
        try {           
            CXFInvocationHandler handler = 
                    (CXFInvocationHandler)Proxy.getInvocationHandler((Proxy)connection);
            Object managedConnection = handler.getData().getManagedConnection();

            if (managedConnection != this) {
                handler.getData().setManagedConnection(this);
                ((ManagedConnectionImpl)managedConnection).disassociateConnectionHandle(connection);

                if (getCXFService() == null) { 
                    // Very unlikely as THIS
                    // managed connection is
                    // already involved in a transaction.
                    cxfService = connection;
                    connectionHandleActive = true;
                }
            }
        } catch (Exception ex) {         
            throw new ResourceAdapterInternalException(
                              new Message("ASSOCIATED_ERROR", BUNDLE).toString(), ex);
        }
    }

    public CXFManagedConnectionFactory getManagedConnectionFactory() {
        return (ManagedConnectionFactoryImpl)theManagedConnectionFactory();
    }

    public Object getCXFService() {
        return cxfService;
    }

    private void initializeCXFConnection(ConnectionRequestInfo crInfo, Subject subject)
        throws ResourceException {
        this.crinfo = crInfo;
        this.subject = subject;
        cxfService = getCXFConnection(subject, crInfo);
    }

    public Object getConnection(Subject subject, ConnectionRequestInfo crInfo) throws ResourceException {

        Object connection = null;
        
        if (getCXFService() == null) {
            initializeCXFConnection(crInfo, subject);
            connection = getCXFService();            
        } else {
            if (!connectionHandleActive && this.crinfo.equals(crInfo)) {
                connection = getCXFService();
            } else {
                connection = getCXFConnection(subject, crInfo);
            }
        }
        connectionHandleActive = true;
        return connection;
    }

    public synchronized Object getCXFConnection(Subject subject, ConnectionRequestInfo crInfo)
        throws ResourceException {

        CXFConnectionRequestInfo requestInfo = (CXFConnectionRequestInfo)crInfo;
        Class<?> serviceInterface = requestInfo.getInterface();
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            ClientProxyFactoryBean factoryBean = null;
            if (isJaxWsServiceInterface(serviceInterface)) {
                factoryBean = new JaxWsProxyFactoryBean();
            } else {
                factoryBean = new ClientProxyFactoryBean();
            }
            factoryBean.setServiceClass(serviceInterface);
            if (requestInfo.getServiceName() != null) {
                factoryBean.getServiceFactory().setServiceName(requestInfo.getServiceName());
            }
            if (requestInfo.getPortName() != null) {
                factoryBean.getServiceFactory().setEndpointName(requestInfo.getPortName());
            }
            if (requestInfo.getWsdlLocation() != null) {
                factoryBean.getServiceFactory().setWsdlURL(requestInfo.getWsdlLocation());
            }
            if (requestInfo.getAddress() != null) {
                factoryBean.setAddress(requestInfo.getAddress());
            }
            
            Object obj = factoryBean.create();
            
            setSubject(subject);
            
            return createConnectionProxy(obj, requestInfo, subject);
        } catch (WebServiceException wse) {
            throw new ResourceAdapterInternalException(new Message("FAILED_TO_GET_CXF_CONNECTION", 
                                                                   BUNDLE, requestInfo).toString() , wse);
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }

    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new CXFManagedConnectionMetaData();
    }
    
    
    private boolean isJaxWsServiceInterface(Class<?> cls) {
        if (cls == null) {
            return false;
        }
        if (null != cls.getAnnotation(WebService.class)) {
            return true;
        }
        return false;
    }
    
    public boolean isBound() {
        return getCXFService() != null;
    }

    
    // Compliance: WL9 checks
    // implemention of Connection method - never used as real Connection impl is
    // a java.lang.Proxy
    public void close() throws ResourceException {
        //TODO 
    }

    void disassociateConnectionHandle(Object handle) {
        if (cxfService == handle) {
            connectionHandleActive = false;
            cxfService = null;
        }
    }

    private Object createConnectionProxy(Object obj, CXFConnectionRequestInfo cri, Subject subject)
        throws ResourceException {

        Class classes[] = {Connection.class, BindingProvider.class, cri.getInterface()};

        return Proxy.newProxyInstance(cri.getInterface().getClassLoader(), classes, 
                                      createInvocationHandler(obj, subject));
    }
       
    private InvocationHandler createInvocationHandler(Object obj, Subject subject) throws ResourceException {

        return getHandlerFactory().createHandlers(obj, subject);
    }

    private InvocationHandlerFactory getHandlerFactory() throws ResourceException {
        if (handlerFactory == null) {
            handlerFactory = new InvocationHandlerFactory(getBus(), this);
        }
        return handlerFactory;
    }

    private Bus getBus() {
        return ((ManagedConnectionFactoryImpl)getManagedConnectionFactory()).getBus();
    }

    
    public void close(Object closingHandle) throws ResourceException {
        if (closingHandle == cxfService) {
            connectionHandleActive = false;
        }
        super.close(closingHandle);
    }

    // beging chucked from the pool
    public void destroy() throws ResourceException {
        connectionHandleActive = false;
        this.cxfService = null;
        super.destroy();
    }
   
    public CXFTransaction getCXFTransaction() {
        //TODO should throw the exception  
        return null;
    }

    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException();
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException();
    }
}
