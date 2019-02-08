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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
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
import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.jmx.export.runtime.ModelMBeanAssembler;
import org.apache.cxf.management.jmx.type.JMXConnectorPolicyType;

/**
 * The manager class for the JMXManagedComponent which hosts the JMXManagedComponents.
 */
public class InstrumentationManagerImpl extends JMXConnectorPolicyType
    implements InstrumentationManager, BusLifeCycleListener {
    private static final Logger LOG = LogUtils.getL7dLogger(InstrumentationManagerImpl.class);

    private static Map<String, String>mbeanServerIDMap = new HashMap<>();

    private Bus bus;
    private MBServerConnectorFactory mcf;
    private MBeanServer mbs;
    private Set<ObjectName> busMBeans = new HashSet<>();
    private boolean connectFailed;
    private String persistentBusId;

    /**
     * For backward compatibility, {@link #createMBServerConnectorFactory} is <code>true</code> by default.
     */
    private boolean createMBServerConnectorFactory = true;
    private String mbeanServerName = ManagementConstants.DEFAULT_DOMAIN_NAME;
    private boolean usePlatformMBeanServer;

    public InstrumentationManagerImpl() {
        super();
    }

    public InstrumentationManagerImpl(Bus bus) {
        this();
        readJMXProperties(bus);
        this.bus = bus;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        if (this.bus == null) {
            readJMXProperties(bus);
        } else {
            // possibly this bus was reassigned from another im bean
            InstrumentationManager im = bus.getExtension(InstrumentationManager.class);
            if (this != im) {
                bus.setExtension(this, InstrumentationManager.class);
                try {
                    ManagedBus mbus = new ManagedBus(bus);
                    im.unregister(mbus);
                    if (LOG.isLoggable(Level.INFO)) {
                        LOG.info("unregistered " + mbus.getObjectName());
                    }
                } catch (JMException e) {
                    // ignore
                }
            }
        }
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

    @Deprecated
    public void register() {
    }

    @PostConstruct
    public void init() {
        if (bus != null && bus.getExtension(MBeanServer.class) != null) {
            enabled = true;
            createMBServerConnectorFactory = false;
            mbs = bus.getExtension(MBeanServer.class);
        }
        if (isEnabled()) {
            if (mbs == null) {
                // return platform mbean server if the option is specified.
                if (usePlatformMBeanServer) {
                    mbs = ManagementFactory.getPlatformMBeanServer();
                } else {
                    String mbeanServerID = mbeanServerIDMap.get(mbeanServerName);
                    List<MBeanServer> servers = null;
                    if (mbeanServerID != null) {
                        servers = CastUtils.cast(MBeanServerFactory.findMBeanServer(mbeanServerID));
                    }
                    if (servers == null || servers.isEmpty()) {
                        mbs = MBeanServerFactory.createMBeanServer(mbeanServerName);
                        try {
                            mbeanServerID = (String) mbs.getAttribute(getDelegateName(),
                                                                     "MBeanServerId");
                            mbeanServerIDMap.put(mbeanServerName, mbeanServerID);
                        } catch (JMException e) {
                            // ignore
                        }
                    } else {
                        mbs = servers.get(0);
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
                    if (LOG.isLoggable(Level.INFO)) {
                        LOG.info("registered " + mbus.getObjectName());
                    }
                } catch (JMException jmex) {
                    LOG.log(Level.SEVERE, "REGISTER_FAILURE_MSG", new Object[]{bus, jmex});
                }
            }
        }

        if (null != bus) {
            bus.setExtension(this, InstrumentationManager.class);
            BusLifeCycleManager blcm = bus.getExtension(BusLifeCycleManager.class);
            if (null != blcm) {
                blcm.registerLifeCycleListener(this);
            }
        }
    }

    private ObjectName getDelegateName() throws JMException {
        try {
            return (ObjectName)MBeanServerDelegate.class.getField("DELEGATE_NAME").get(null);
        } catch (Throwable t) {
            //ignore, likely on Java5
        }
        try {
            return new ObjectName("JMImplementation:type=MBeanServerDelegate");
        } catch (MalformedObjectNameException e) {
            JMException jme = new JMException(e.getMessage());
            jme.initCause(e);
            throw jme;
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
            registerMBeanWithServer(obj, persist(name), forceRegistration);
        } catch (NotCompliantMBeanException e) {
            //If this is not a "normal" MBean, then try to deploy it using JMX annotations
            ModelMBeanAssembler assembler = new ModelMBeanAssembler();
            ModelMBeanInfo mbi = assembler.getModelMbeanInfo(obj.getClass());
            register(obj, name, mbi, forceRegistration);
        }
    }

    public ObjectName register(ManagedComponent i) throws JMException {
        return register(i, false);
    }

    public ObjectName register(ManagedComponent i, boolean forceRegistration) throws JMException {
        ObjectName name = i.getObjectName();
        register(i, name, forceRegistration);
        return name;
    }


    public void unregister(ManagedComponent component) throws JMException {
        ObjectName name = component.getObjectName();
        unregister(persist(name));
    }

    public void unregister(ObjectName name) throws JMException {
        if (!isEnabled() || connectFailed) {
            return;
        }

        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("unregistering MBean " + name);
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

        //Using the array to hold the busMBeans to avoid the CurrentModificationException
        Object[] mBeans = busMBeans.toArray();
        for (Object name : mBeans) {
            busMBeans.remove(name);
            try {
                unregister((ObjectName)name);
            } catch (JMException jmex) {
                LOG.log(Level.SEVERE, "UNREGISTER_FAILURE_MSG", new Object[]{name, jmex});
            }
        }
    }

    public void initComplete() {

    }

    public void preShutdown() {

    }

    public void postShutdown() {
        this.shutdown();
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
        registerMBeanWithServer(rtMBean, persist(name), forceRegistration);
    }

    private void registerMBeanWithServer(Object obj, ObjectName name, boolean forceRegistration)
        throws JMException {
        ObjectInstance instance = null;
        try {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("registering MBean " + name + ": " + obj);
            }
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

    public String getPersistentBusId() {
        return persistentBusId;
    }

    public void setPersistentBusId(String id) {
        persistentBusId = sanitize(id);
    }

    private ObjectName persist(ObjectName original) throws JMException {
        ObjectName persisted = original;
        if (!(persistentBusId == null
              || "".equals(persistentBusId)
              || persistentBusId.startsWith("${"))) {
            String originalStr = original.toString();
            if (originalStr.indexOf(ManagementConstants.BUS_ID_PROP) != -1) {
                String persistedStr =
                    originalStr.replaceFirst(ManagementConstants.BUS_ID_PROP + "=" + bus.getId(),
                                             ManagementConstants.BUS_ID_PROP + "=" + persistentBusId);
                persisted = new ObjectName(persistedStr);
            }
        }
        return persisted;
    }

    private String sanitize(String in) {
        StringBuilder str = new StringBuilder(in.length());
        for (int x = 0; x < in.length(); x++) {
            char ch = in.charAt(x);
            switch (ch) {
            case ':':
            case '/':
            case '\\':
            case '?':
            case '=':
            case ',':
                str.append('_');
                break;
            default:
                str.append(ch);
            }
        }
        return str.toString();
    }

    private void readJMXProperties(Bus b) {
        if (b != null) {
            persistentBusId = getBusProperty(b, "bus.jmx.persistentBusId", persistentBusId);
            mbeanServerName =
                getBusProperty(b, "bus.jmx.serverName", mbeanServerName);
            usePlatformMBeanServer =
                getBusProperty(b, "bus.jmx.usePlatformMBeanServer", usePlatformMBeanServer);
            createMBServerConnectorFactory =
                getBusProperty(b, "bus.jmx.createMBServerConnectorFactory", createMBServerConnectorFactory);
            daemon = getBusProperty(b, "bus.jmx.daemon", daemon);
            threaded = getBusProperty(b, "bus.jmx.threaded", threaded);
            enabled = getBusProperty(b, "bus.jmx.enabled", enabled);
            jmxServiceURL = getBusProperty(b, "bus.jmx.JMXServiceURL", jmxServiceURL);
        }
    }

    private static String getBusProperty(Bus b, String key, String dflt) {
        String v = (String)b.getProperty(key);
        return v != null ? v : dflt;
    }

    private static boolean getBusProperty(Bus b, String key, boolean dflt) {
        Object v = b.getProperty(key);
        if (v instanceof Boolean) {
            return (Boolean)v;
        }
        return v != null ? Boolean.valueOf(v.toString()) : dflt;
    }
}

