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

package org.apache.cxf.management.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.ManagedBus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.JMXConnectorPolicyType;
import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.jmx.export.runtime.ModelMBeanAssembler;

/**
 * The manager class for the JMXManagedComponent which hosts the JMXManagedComponents.
 */
public class InstrumentationManagerImpl extends JMXConnectorPolicyType 
    implements InstrumentationManager, BusLifeCycleListener {
    private static final Logger LOG = LogUtils.getL7dLogger(InstrumentationManagerImpl.class);

    private Bus bus;
    private MBServerConnectorFactory mcf;    
    private MBeanServer mbs;
    private Set<ObjectName> busMBeans = new HashSet<ObjectName>();
    private boolean connectFailed;
    /**
     * For backward compatibility, {@link #createMBServerConnectorFactory} is <code>true</code> by default.
     */
    private boolean createMBServerConnectorFactory = true;
    private String mbeanServerName = ManagementConstants.DEFAULT_DOMAIN_NAME;
    private boolean usePlatformMBeanServer;
    
    public InstrumentationManagerImpl() {
        super();
    }
    
    public Bus getBus() {
        return bus;
    }
    
    @Resource(name = "cxf")
    public void setBus(Bus bus) {        
        this.bus = bus;
    }

    public void setServerName(String s) {
        mbeanServerName = s;
    }
    
    public void setCreateMBServerConnectorFactory(boolean createMBServerConnectorFactory) {
        this.createMBServerConnectorFactory = createMBServerConnectorFactory;
    }
    
    public void setUsePlatformMBeanServer(Boolean flag) {
        usePlatformMBeanServer = flag;
    }

    @PostConstruct     
    public void register() {    
        if (null != bus) {
            bus.setExtension(this, InstrumentationManager.class);
            BusLifeCycleManager blcm = bus.getExtension(BusLifeCycleManager.class);
            if (null != blcm) {
                blcm.registerLifeCycleListener(this);
            }    
        }
    }
    
    @PostConstruct     
    public void init() {    
        if (isEnabled()) {
            
            if (mbs == null) {
                // return platform mbean server if the option is specified.
                if (usePlatformMBeanServer) {
                    mbs = ManagementFactory.getPlatformMBeanServer();
                } else {
                    List<MBeanServer> servers = CastUtils
                        .cast(MBeanServerFactory.findMBeanServer(mbeanServerName));
                    if (servers.size() <= 1) {
                        mbs = MBeanServerFactory.createMBeanServer(mbeanServerName);
                    } else {
                        mbs = (MBeanServer)servers.get(0);
                    }
                }
            }
            
            if (createMBServerConnectorFactory) {
                mcf = MBServerConnectorFactory.getInstance();
                mcf.setMBeanServer(mbs);
                mcf.setThreaded(isThreaded());
                mcf.setDaemon(isDaemon());
                mcf.setServiceUrl(getJMXServiceURL());
                try {
                    mcf.createConnector();
                } catch (IOException ex) {
                    connectFailed = true;
                    LOG.log(Level.SEVERE, "START_CONNECTOR_FAILURE_MSG", new Object[] {ex});
                }
            }

            if (!connectFailed && null != bus) {            
                try {
                    //Register Bus here since we can guarantee that Instrumentation 
                    //infrastructure has been initialized.
                    ManagedBus mbus = new ManagedBus(bus);                    
                    register(mbus);
                } catch (JMException jmex) {
                    LOG.log(Level.SEVERE, "REGISTER_FAILURE_MSG", new Object[]{bus, jmex});
                }
            }
        }
    }

    public void register(Object obj, ObjectName name) throws JMException {
        register(obj, name, false);        
    }
    
    public void register(Object obj, ObjectName name, boolean forceRegistration) throws JMException {
        if (!isEnabled() || connectFailed) {
            return;           
        }
        //Try to register as a Standard MBean
        try {
            registerMBeanWithServer(obj, name, forceRegistration);           
        } catch (NotCompliantMBeanException e) {        
            //If this is not a "normal" MBean, then try to deploy it using JMX annotations
            ModelMBeanAssembler assembler = new ModelMBeanAssembler();
            ModelMBeanInfo mbi = assembler.getModelMbeanInfo(obj.getClass());
            register(obj, name, mbi, forceRegistration);
        }                
    }

    public ObjectName register(ManagedComponent i) throws JMException {
        ObjectName name = register(i, false);
        
        return name;
    }
    
    public ObjectName register(ManagedComponent i, boolean forceRegistration) throws JMException {
        ObjectName name = i.getObjectName();
        register(i, name, forceRegistration);
        
        return name;
    }    

    
    public void unregister(ManagedComponent component) throws JMException {
        ObjectName name = component.getObjectName();
        unregister(name);
    }
    
    public void unregister(ObjectName name) throws JMException {  
        if (!isEnabled() || connectFailed) {
            return;           
        }
        
        busMBeans.remove(name);       
        mbs.unregisterMBean(name);                    
    }  
    
    public MBeanServer getMBeanServer() {        
        return mbs;
    }

    public void setServer(MBeanServer server) {
        this.mbs = server;
    }
    
    public void shutdown() {
        if (!isEnabled()) {
            return;           
        }
        
        if (mcf != null) {
            try {
                mcf.destroy();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "STOP_CONNECTOR_FAILURE_MSG", new Object[] {ex});
            }
        }
    }

    public void initComplete() {
        
    }
    
    public void preShutdown() {
                
    }
    
   
    public void postShutdown() {
        //Using the array to hold the busMBeans to avoid the CurrentModificationException
        Object[] mBeans = busMBeans.toArray();
        for (Object name : mBeans) {
            busMBeans.remove((ObjectName)name);
            try {
                unregister((ObjectName)name);
            } catch (JMException jmex) {
                LOG.log(Level.SEVERE, "UNREGISTER_FAILURE_MSG", new Object[]{name, jmex});
            }
        }
    }
    
    private void register(Object obj, ObjectName name, ModelMBeanInfo mbi, boolean forceRegistration) 
        throws JMException {                  
        RequiredModelMBean rtMBean = 
            (RequiredModelMBean)mbs.instantiate("javax.management.modelmbean.RequiredModelMBean");
        rtMBean.setModelMBeanInfo(mbi);
        try {
            rtMBean.setManagedResource(obj, "ObjectReference");
        } catch (InvalidTargetObjectTypeException itotex) {
            throw new JMException(itotex.getMessage());
        }
        registerMBeanWithServer(rtMBean, name, forceRegistration);
    }

    private void registerMBeanWithServer(Object obj, ObjectName name, boolean forceRegistration) 
        throws JMException {
        ObjectInstance instance = null;
        try {
            instance = mbs.registerMBean(obj, name);           
        } catch (InstanceAlreadyExistsException e) {            
            if (forceRegistration) {
                mbs.unregisterMBean(name);                
                instance = mbs.registerMBean(obj, name);
            } else {
                throw e;
            }
        }
        
        if (instance != null) {
            busMBeans.add(name);
        }
    }
    

}

