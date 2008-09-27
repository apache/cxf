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

import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.security.auth.Subject;

import org.apache.cxf.Bus;
//import org.apache.cxf.BusFactory;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.core.resourceadapter.AbstractManagedConnectionFactoryImpl;
import org.apache.cxf.jca.core.resourceadapter.AbstractManagedConnectionImpl;
import org.apache.cxf.jca.core.resourceadapter.ResourceAdapterInternalException;

public class ManagedConnectionFactoryImpl 
    extends AbstractManagedConnectionFactoryImpl
    implements CXFManagedConnectionFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(ManagedConnectionFactoryImpl.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(ManagedConnectionFactoryImpl.class);
    
    protected JCABusFactory jcaBusFactory;

    public ManagedConnectionFactoryImpl() {
        super();
        LOG.info("ManagedConnectionFactoryImpl constructed without props by appserver...");
    }

    public ManagedConnectionFactoryImpl(Properties props) {
        super(props);
        LOG.info("ManagedConnectionFactoryImpl constructed with props by appserver. props = " + props);
    }

    public void setLogLevel(String logLevel) {
        setProperty(LOG_LEVEL, logLevel);
    }

    @Deprecated
    public void setConfigurationDomain(String name) {
        setProperty(CONFIG_DOMAIN, name);
    }

    @Deprecated
    public void setConfigurationScope(String name) {
        setProperty(CONFIG_SCOPE, name);
    }
    
    public void setEJBServicePropertiesURL(String name) {
        setProperty(EJB_SERVICE_PROPERTIES_URL, name);
    }

    public void setMonitorEJBServiceProperties(Boolean monitor) {
        setProperty(MONITOR_EJB_SERVICE_PROPERTIES, monitor.toString());
    }

    public void setEJBServicePropertiesPollInterval(Integer pollInterval) {
        setProperty(MONITOR_POLL_INTERVAL, pollInterval.toString());
    }
   
    public String getLogLevel() {
        return getPluginProps().getProperty(LOG_LEVEL);
    }
    
    @Deprecated
    public String getConfigurationDomain() {
        return getPluginProps().getProperty(CONFIG_DOMAIN, CONFIG_DOMAIN);
    }

    @Deprecated
    public String getConfigurationScope() {
        return getPluginProps().getProperty(CONFIG_SCOPE, CONFIG_SCOPE);
    }
    
    public String getEJBServicePropertiesURL() {
        return getPluginProps().getProperty(EJB_SERVICE_PROPERTIES_URL);
    }

    public Boolean getMonitorEJBServiceProperties() {
        return Boolean.valueOf(getPluginProps().getProperty(MONITOR_EJB_SERVICE_PROPERTIES));
    }

    public Integer getEJBServicePropertiesPollInterval() {
        return new Integer(getPluginProps().getProperty(MONITOR_POLL_INTERVAL, 
                                                        DEFAULT_MONITOR_POLL_INTERVAL));
    }
   
    public URL getEJBServicePropertiesURLInstance() throws ResourceException {
        return getPropsURL(getEJBServicePropertiesURL());
    }
    
    public String getEJBServantBaseURL() throws ResourceException {
        return getPluginProps().getProperty(EJB_SERVANT_BASE_URL);
    }
    
    public void setEJBServantBaseURL(String url) throws ResourceException {
        setProperty(EJB_SERVANT_BASE_URL, url);
    }

    // compliance: WL9 checks
    // need to ensure multiple instances with same config properties are equal
    // multiple instances with same config do not make sense to me
   

    protected void validateReference(AbstractManagedConnectionImpl conn, javax.security.auth.Subject subj) {
    }

    public Object createConnectionFactory() throws ResourceException {
        throw new ResourceAdapterInternalException(
                           new Message("NON_MANAGED_CONNECTION_IS_NOT_SUPPORTED", BUNDLE).toString());
    }

    public Object createConnectionFactory(ConnectionManager connMgr) throws ResourceException {
        LOG.info("connManager=" + connMgr);
        if (connMgr == null) {
            throw new ResourceAdapterInternalException(
                            new Message("NON_MANAGED_CONNECTION_IS_NOT_SUPPORTED", BUNDLE).toString());
        }
        init(connMgr.getClass().getClassLoader());
        LOG.fine("Setting AppServer classloader in jcaBusFactory. " + connMgr.getClass().getClassLoader());
        return new ConnectionFactoryImpl(this, connMgr);
    }

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connReqInfo) 
        throws ResourceException {
        LOG.info("create connection, subject=" + subject + " connReqInfo=" + connReqInfo);
        init(Thread.currentThread().getContextClassLoader());
        return (ManagedConnection)new ManagedConnectionImpl(this, connReqInfo, subject);
    }

    public void close() throws javax.resource.spi.ResourceAdapterInternalException {
    }

    protected synchronized void init(ClassLoader appserverClassLoader) throws ResourceException {
        if (jcaBusFactory == null) {
            jcaBusFactory = new JCABusFactory(this);
            jcaBusFactory.create(appserverClassLoader, getBootstrapContext());
        }
    }

    public Bus getBus() {
        return (jcaBusFactory != null) ? jcaBusFactory.getBus() : null;
    }

    protected Object getBootstrapContext() {
        return null;
    }
}
