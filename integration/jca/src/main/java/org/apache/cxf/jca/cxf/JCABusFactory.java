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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jca.core.resourceadapter.ResourceAdapterInternalException;
import org.apache.cxf.jca.core.resourceadapter.UriHandlerInit;
import org.apache.cxf.jca.servant.EJBEndpoint;
import org.apache.cxf.jca.servant.EJBServantConfig;


public class JCABusFactory {
    
    private static final Logger LOG = LogUtils.getL7dLogger(JCABusFactory.class);
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JCABusFactory.class);
    
    private Bus bus;
    private List<Server> servantsCache = new ArrayList<Server>();
    private ClassLoader appserverClassLoader;
    private ManagedConnectionFactoryImpl mcf;
    private Object raBootstrapContext;
    

    public JCABusFactory(ManagedConnectionFactoryImpl aMcf) {
        this.mcf = aMcf;
    }
    
    protected synchronized void init() throws ResourceException {
        
        LOG.info("Initializing the CXF Bus ...");
        new UriHandlerInit();
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader cl = this.getClass().getClassLoader();

            // ensure resourceadapter: url handler can be found by URLFactory
            Thread.currentThread().setContextClassLoader(cl);
            
            //TODO Check for the managed connection factory properties
            //TODO We may need get the configuration file from properties 
            
            BusFactory bf = BusFactory.newInstance();
            bus = bf.createBus();
            initializeServants();
        } catch (Exception ex) {
            if (ex instanceof ResourceAdapterInternalException) {
                throw (ResourceException)ex;
            } else {
                throw new ResourceAdapterInternalException(
                                  new Message("FAIL_TO_INITIALIZE_JCABUSFACTORY", BUNDLE).toString(), ex);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    
    protected void initializeServants() throws ResourceException {
        if (isMonitorEJBServicePropertiesEnabled()) {            
            LOG.info("Ejb service properties auto-detect enabled. ");
            startPropertiesMonitorWorker();
        } else {            
            URL propsUrl = mcf.getEJBServicePropertiesURLInstance();
            if (propsUrl != null) {
                initializeServantsFromProperties(loadProperties(propsUrl));
            }
        }
    }
    
    private void initializeServantsFromProperties(Properties ejbServants) throws ResourceException {
        
        deregisterServants(bus);
        LOG.info("Initializing EJB endpoints from properties file...");
        
        try {           
            Enumeration keys = ejbServants.keys();
            while (keys.hasMoreElements()) {
                String theJNDIName = (String)keys.nextElement();
                String value = (String)ejbServants.get(theJNDIName);
                EJBServantConfig config = new EJBServantConfig(theJNDIName, value);
                EJBEndpoint ejbEndpoint = new EJBEndpoint(config);
                ejbEndpoint.setEjbServantBaseURL(mcf.getEJBServantBaseURL());
                ejbEndpoint.setWorkManager(getWorkManager());
                Server servant = ejbEndpoint.publish();
                
                synchronized (servantsCache) {
                    if (servant != null) {
                        servantsCache.add(servant);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResourceException(new Message("FAIL_TO_START_EJB_SERVANTS", BUNDLE).toString(), e);
        }
        
    }
    

    private void startPropertiesMonitorWorker() throws ResourceException {
        Integer pollIntervalInteger = mcf.getEJBServicePropertiesPollInterval();
        int pollInterval = pollIntervalInteger.intValue();
        
        LOG.info("Ejb service properties poll interval is: [" + pollInterval + " seconds]");
        
        EJBServicePropertiesMonitorWorker worker = new EJBServicePropertiesMonitorWorker(pollInterval);
        if (getWorkManager() != null) {
            getWorkManager().startWork(worker, CXFWorkAdapter.DEFAULT_START_TIME_OUT, null, worker);
        } else {
            Thread t = new Thread(worker);
            t.setDaemon(true);
            t.start();
        }
    }

    private boolean isMonitorEJBServicePropertiesEnabled() throws ResourceException {
        boolean retVal = false;

        if (mcf.getMonitorEJBServiceProperties().booleanValue()) {
            URL url = mcf.getEJBServicePropertiesURLInstance();
            if (url == null) {
                throw new ResourceAdapterInternalException(
                                  new Message("EJB_SERVANT_PROPERTIES_IS_NULL", BUNDLE).toString());
            }
            retVal = isFileURL(url);
        }

        return retVal;
    }

    boolean isFileURL(URL url) {
        return url != null && "file".equals(url.getProtocol());
    }

    private void deregisterServants(Bus aBus) {
        synchronized (servantsCache) {
            for (Server servant : servantsCache) {
                //REVISIT: seems using server.stop() doesn't release resource properly.
                servant.destroy();
                LOG.info("Shutdown the EJB Endpoint: " + servant.getEndpoint().getEndpointInfo().getName());
            }
            servantsCache.clear();
        }
    }
    
    protected Properties loadProperties(URL propsUrl) throws ResourceException {
        Properties props = null;
        InputStream istream = null;

        LOG.info("loadProperties, url=" + propsUrl);

        try {
            istream = propsUrl.openStream();
            props = new Properties();
            props.load(istream);
        } catch (IOException e) {
            throw new ResourceAdapterInternalException(
                       new Message("FAIL_TO_LOAD_EJB_SERVANT_PROPERTIES", BUNDLE, propsUrl).toString(), e);
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (IOException e) {
                    //DO Nothing
                }
            }
        }

        return props;
    }
    

    protected List getRegisteredServants() {
        return servantsCache;
    }

    public ClassLoader getAppserverClassLoader() {
        return appserverClassLoader;
    }

    public void setAppserverClassLoader(ClassLoader classLoader) {
        this.appserverClassLoader = classLoader;
    }

    public Object getBootstrapContext() {
        return raBootstrapContext;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus b) {
        bus = b;
    }
   
    public void create(ClassLoader classLoader, Object context) throws ResourceException {
        this.appserverClassLoader = classLoader;
        this.raBootstrapContext = context;
        init();
    }
    
    private class EJBServicePropertiesMonitorWorker extends CXFWorkAdapter implements Work {
        private long previousModificationTime;
        private final int pollIntervalSeconds;
        private final File propsFile;
        
        //The release() method will be called on separate thread while the run() is processing.
        private volatile boolean continuing = true;

        EJBServicePropertiesMonitorWorker(int pollInterval) throws ResourceException {
            pollIntervalSeconds = pollInterval;
            propsFile = new File(mcf.getEJBServicePropertiesURLInstance().getPath());
        }

        public void run() {
            do {
                try {
                    if (isPropertiesFileModified()) {
                        LOG.info("ejbServicePropertiesFile modified, initializing/updating servants");
                        initializeServantsFromProperties(loadProperties(propsFile.toURI().toURL()));
                    }
                    Thread.sleep(pollIntervalSeconds * 1000);
                } catch (Exception e) {
                    LOG.info("MonitorThread: failed to initialiseServantsFromProperties "
                              + "with properties absolute path=" + propsFile.getAbsolutePath());
                }
            } while (continuing);
        }
        
        public void release() {
            this.continuing = false;
        }

        protected boolean isPropertiesFileModified() {
            boolean fileModified = false;
            if (propsFile.exists()) {
                long currentModificationTime = propsFile.lastModified();
                if (currentModificationTime > previousModificationTime) {
                    previousModificationTime = currentModificationTime;
                    fileModified = true;
                }
            }
            return fileModified;
        }
    }

    // for unit test
    protected void setBootstrapContext(Object ctx) {
        raBootstrapContext = ctx;
    }

    public WorkManager getWorkManager() {
        if (getBootstrapContext() instanceof BootstrapContext) {
            BootstrapContext context = (BootstrapContext)getBootstrapContext();
            return context.getWorkManager();
        }
        return null;
    }
    
    

    
}
