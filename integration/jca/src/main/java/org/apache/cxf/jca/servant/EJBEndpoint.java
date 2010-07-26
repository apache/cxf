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

package org.apache.cxf.jca.servant;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJBHome;
import javax.jws.WebService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.resource.spi.work.WorkManager;
import javax.rmi.PortableRemoteObject;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jca.cxf.WorkManagerThreadPool;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;


public class EJBEndpoint {
    
    private static final Logger LOG = LogUtils.getL7dLogger(EJBEndpoint.class);
    
    private static final int DEFAULT_HTTP_PORT = 80;
    
    private static final String HTTPS_PREFIX = "https";
    
    private EJBServantConfig config;
    
    private Context jndiContext;
    
    private EJBHome ejbHome;
    
    private String ejbServantBaseURL;
    
    private WorkManager workManager;
    
    public EJBEndpoint(EJBServantConfig ejbConfig) {
        this.config = ejbConfig;
    }
    
    public Server publish() throws Exception {
        jndiContext = new InitialContext();
        Object obj = jndiContext.lookup(config.getJNDIName());
        ejbHome = (EJBHome) PortableRemoteObject.narrow(obj, EJBHome.class);
        
        Class<?> interfaceClass = Class.forName(getServiceClassName());
        boolean isJaxws = isJaxWsServiceInterface(interfaceClass);
        ServerFactoryBean factory = isJaxws ? new JaxWsServerFactoryBean() : new ServerFactoryBean();
        factory.setServiceClass(interfaceClass);
        
        if (config.getWsdlURL() != null) {
            factory.getServiceFactory().setWsdlURL(config.getWsdlURL());
        }
        
        factory.setInvoker(new EJBInvoker(ejbHome));
        
        String baseAddress = isNotNull(getEjbServantBaseURL()) ? getEjbServantBaseURL() 
                                                               : getDefaultEJBServantBaseURL();
        String address = baseAddress + "/" + config.getJNDIName();
        factory.setAddress(address);
        
        if (address.length() >= 5 && HTTPS_PREFIX.equalsIgnoreCase(address.substring(0, 5))) {
            throw new UnsupportedOperationException("EJBEndpoint creation by https protocol is unsupported");
        }
        
        if (getWorkManager() != null) {
            setWorkManagerThreadPoolToJetty(factory.getBus(), baseAddress);
        }
        
        Server server = factory.create();
        LOG.info("Published EJB Endpoint of [" + config.getJNDIName() + "] at [" + address + "]");
        
        return server;
    }
    
    
    protected void setWorkManagerThreadPoolToJetty(Bus bus, String baseAddress) {
        JettyHTTPServerEngineFactory engineFactory = bus.getExtension(JettyHTTPServerEngineFactory.class);
        int port = getAddressPort(baseAddress);
        if (engineFactory.retrieveJettyHTTPServerEngine(port) != null) {
            return;
        }
        JettyHTTPServerEngine engine = new JettyHTTPServerEngine();
        AbstractConnector connector = new SelectChannelConnector();
        connector.setPort(port);
        connector.setThreadPool(new WorkManagerThreadPool(getWorkManager()));
        engine.setConnector(connector);
        engine.setPort(port);
        
        List<JettyHTTPServerEngine> engineList = new ArrayList<JettyHTTPServerEngine>();
        engineList.add(engine);
        engineFactory.setEnginesList(engineList);
    }
    
    public String getServiceClassName() throws Exception {
        String packageName = PackageUtils.parsePackageName(config.getServiceName().getNamespaceURI(), null);
        String interfaceName = packageName + "." 
                               + config.getJNDIName().substring(0, config.getJNDIName().length() - 4);
        return interfaceName;
    }
    
    public String getDefaultEJBServantBaseURL() throws Exception {
        String hostName = "";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostName = addr.getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostName = "localhost";
        }
        return "http://" + hostName + ":9999";
    }
    
    public int getAddressPort(String address) {
        int index = address.lastIndexOf(":");
        int end = address.lastIndexOf("/");
        if (index == 4) {
            return DEFAULT_HTTP_PORT;
        }
        if (end < index) {
            return new Integer(address.substring(index + 1)).intValue();
        } 
        return new Integer(address.substring(index + 1, end)).intValue();
    }
    
    private static boolean isJaxWsServiceInterface(Class<?> cls) {
        if (cls == null) {
            return false;
        }
        if (null != cls.getAnnotation(WebService.class)) {
            return true;
        }
        return false;
    }

    public String getEjbServantBaseURL() {
        return ejbServantBaseURL;
    }

    public void setEjbServantBaseURL(String ejbServantBaseURL) {
        this.ejbServantBaseURL = ejbServantBaseURL;
    }
    
    private static boolean isNotNull(String value) {
        if (value != null && !"".equals(value.trim())) {
            return true;
        }
        return false;
    }

    public WorkManager getWorkManager() {
        return workManager;
    }

    public void setWorkManager(WorkManager workManager) {
        this.workManager = workManager;
    }
    
    
    
}
