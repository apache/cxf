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
package org.apache.cxf.transport.http_jetty;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.management.MBeanServer;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.management.InstrumentationManager;
import org.eclipse.jetty.util.component.Container;


/**
 * This Bus Extension handles the configuration of network port
 * numbers for use with "http" or "https". This factory 
 * caches the JettyHTTPServerEngines so that they may be 
 * retrieved if already previously configured.
 */
public class JettyHTTPServerEngineFactory implements BusLifeCycleListener {
    private static final Logger LOG =
        LogUtils.getL7dLogger(JettyHTTPServerEngineFactory.class);    
    
    private static final int FALLBACK_THREADING_PARAMS_KEY = 0;

    /**
     * This map holds references for allocated ports.
     */
    // Still use the static map to hold the port information
    // in the same JVM
    private static Map<Integer, JettyHTTPServerEngine> portMap =
        new HashMap<Integer, JettyHTTPServerEngine>();
   
    private BusLifeCycleManager lifeCycleManager;
    /**
     * This map holds the threading parameters that are to be applied
     * to new Engines when bound to the reference id.
     */
    private Map<String, ThreadingParameters> threadingParametersMap =
        new TreeMap<String, ThreadingParameters>();

    private ThreadingParameters fallbackThreadingParameters;
    
    /**
     * This map holds TLS Server Parameters that are to be used to
     * configure a subsequently created JettyHTTPServerEngine.
     */
    private Map<String, TLSServerParameters> tlsParametersMap =
        new TreeMap<String, TLSServerParameters>();
    
    
    /**
     * The bus.
     */
    private Bus bus;
    
    /**
     * The Jetty {@link MBeanContainer} to use when enabling JMX in Jetty.
     */
    private Container.Listener mBeanContainer;
    
    public JettyHTTPServerEngineFactory() {
        // Empty
    }    
    public JettyHTTPServerEngineFactory(Bus b,
                                        Map<String, TLSServerParameters> tls,
                                        Map<String, ThreadingParameters> threading) {
        tlsParametersMap.putAll(tls);
        threadingParametersMap.putAll(threading);
        setBus(b);
    }    
    
    
    /**
     * This call is used to set the bus. It should only be called once.
     * @param bus
     */
    @Resource(name = "cxf")
    public final void setBus(Bus bus) {
        assert this.bus == null || this.bus == bus;
        this.bus = bus;
        if (bus != null) {
            bus.setExtension(this, JettyHTTPServerEngineFactory.class);
            lifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
            if (null != lifeCycleManager) {
                lifeCycleManager.registerLifeCycleListener(this);
            }        
        }
    }
    
    public Bus getBus() {
        return bus;
    }
    
    
    /**
     * This call sets TLSParametersMap for a JettyHTTPServerEngine
     * 
     */
    public void setTlsServerParametersMap(
        Map<String, TLSServerParameters>  tlsParamsMap) {
        
        tlsParametersMap = tlsParamsMap;
    }
    
    public Map<String, TLSServerParameters> getTlsServerParametersMap() {
        return tlsParametersMap;
    }
    
    public void setEnginesList(List<JettyHTTPServerEngine> enginesList) {
        for (JettyHTTPServerEngine engine : enginesList) {
            if (engine.getPort() == FALLBACK_THREADING_PARAMS_KEY) {
                fallbackThreadingParameters = engine.getThreadingParameters();
            }
            portMap.put(engine.getPort(), engine);
        }    
    }
    
    /**
     * This call sets the ThreadingParameters for a JettyHTTPServerEngine
     * 
     */
    public void setThreadingParametersMap(
        Map<String, ThreadingParameters> threadingParamsMap) {
        
        threadingParametersMap = threadingParamsMap;
    }
    
    public Map<String, ThreadingParameters> getThreadingParametersMap() {
        return threadingParametersMap;
    }
    
    /**
     * This call sets TLSServerParameters for a JettyHTTPServerEngine
     * that will be subsequently created. It will not alter an engine
     * that has already been created for that network port.
     * @param host       if not null, server will listen on this address/host, 
     *                   otherwise, server will listen on all local addresses.
     * @param port       The network port number to bind to the engine.
     * @param tlsParams  The tls server parameters. Cannot be null.
     * @throws IOException 
     * @throws GeneralSecurityException 
     */
    public void setTLSServerParametersForPort(
        String host,
        int port, 
        TLSServerParameters tlsParams) throws GeneralSecurityException, IOException {
        if (tlsParams == null) {
            throw new IllegalArgumentException("tlsParams cannot be null");
        }
        JettyHTTPServerEngine ref = retrieveJettyHTTPServerEngine(port);
        if (null == ref) {
            ref = new JettyHTTPServerEngine(this, bus, host, port);
            ref.setTlsServerParameters(tlsParams);
            portMap.put(port, ref);
            ref.finalizeConfig();
        } else {
            if (ref.getConnector() != null && ref.getConnector().isRunning()) {
                throw new IOException("can't set the TLS params on the opened connector");
            }
            ref.setTlsServerParameters(tlsParams);            
        }
    }

    /**
     * calls thru to {{@link #createJettyHTTPServerEngine(String, int, String)} with 'null' for host value
     */
    public void setTLSServerParametersForPort(
        int port, 
        TLSServerParameters tlsParams) throws GeneralSecurityException, IOException {
        setTLSServerParametersForPort(null, port, tlsParams);
    }

    /**
     * This call retrieves a previously configured JettyHTTPServerEngine for the
     * given port. If none exists, this call returns null.
     */
    public synchronized JettyHTTPServerEngine retrieveJettyHTTPServerEngine(int port) {
        return portMap.get(port);
    }

    /**
     * This call creates a new JettyHTTPServerEngine initialized for "http"
     * or "https" on the given port. The determination of "http" or "https"
     * will depend on configuration of the engine's bean name.
     * 
     * If an JettyHTTPEngine already exists, or the port
     * is already in use, a BindIOException will be thrown. If the 
     * engine is being Spring configured for TLS a GeneralSecurityException
     * may be thrown.
     * 
     * @param host if not null, server will listen on this host/address, otherwise
     *        server will listen on all local addresses.
     * @param port listen port for server
     * @param protocol "http" or "https"
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public synchronized JettyHTTPServerEngine createJettyHTTPServerEngine(String host, int port, 
        String protocol) throws GeneralSecurityException, IOException {
        LOG.fine("Creating Jetty HTTP Server Engine for port " + port + ".");        
        JettyHTTPServerEngine ref = retrieveJettyHTTPServerEngine(port);
        if (null == ref) {
            ref = new JettyHTTPServerEngine(this, bus, host, port);            
            portMap.put(port, ref);
            ref.finalizeConfig();
        } 
        // checking the protocol    
        if (!protocol.equals(ref.getProtocol())) {
            throw new IOException("Protocol mismatch for port " + port + ": "
                        + "engine's protocol is " + ref.getProtocol()
                        + ", the url protocol is " + protocol);
        }

        if (!(ref.isSetThreadingParameters()
              || null == fallbackThreadingParameters)) {
            if (LOG.isLoggable(Level.INFO)) {
                final int min = fallbackThreadingParameters.getMinThreads();
                final int max = fallbackThreadingParameters.getMaxThreads();
                LOG.log(Level.INFO,
                        "FALLBACK_THREADING_PARAMETERS_MSG",
                        new Object[] {port, min, max});
            }
            ref.setThreadingParameters(fallbackThreadingParameters);
        }
                
        return ref;
    }

    /**
     * Calls thru to {{@link #createJettyHTTPServerEngine(String, int, String)} with a 'null' host value
     */
    public synchronized JettyHTTPServerEngine createJettyHTTPServerEngine(int port, 
        String protocol) throws GeneralSecurityException, IOException {
        return createJettyHTTPServerEngine(null, port, protocol);
    }
    
    /**
     * This method removes the Server Engine from the port map and stops it.
     */
    public synchronized void destroyForPort(int port) {
        JettyHTTPServerEngine ref = portMap.remove(port);
        if (ref != null) {
            LOG.fine("Stopping Jetty HTTP Server Engine on port " + port + ".");
            try {
                ref.stop();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }            
        }
    }
    
    public synchronized Container.Listener getMBeanContainer() {
        if (this.mBeanContainer != null) {
            return mBeanContainer;
        }
        
        if (bus != null && bus.getExtension(InstrumentationManager.class) != null) {
            MBeanServer mbs =  bus.getExtension(InstrumentationManager.class).getMBeanServer();
            if (mbs != null) {
                try {
                    Class<?> cls = ClassLoaderUtils.loadClass("org.eclipse.jetty.jmx.MBeanContainer", 
                                                          getClass());
                    
                    mBeanContainer = (Container.Listener) cls.
                        getConstructor(MBeanServer.class).newInstance(mbs);
                    
                    cls.getMethod("start", (Class<?>[]) null).invoke(mBeanContainer, (Object[]) null);
                } catch (Throwable ex) {
                    //ignore - just won't instrument jetty.  Probably don't have the
                    //jetty-management jar available
                    LOG.info("Could not load or start org.eclipse.management.MBeanContainer.  "
                             + "Jetty JMX support will not be enabled: " + ex.getMessage());
                }
            }
        }
        
        return mBeanContainer;
    }

    public void initComplete() {
        // do nothing here
    }

    public void postShutdown() {
        // shut down the jetty server in the portMap
        // To avoid the CurrentModificationException, 
        // do not use portMap.vaules directly       
        JettyHTTPServerEngine[] engines = portMap.values().toArray(new JettyHTTPServerEngine[0]);
        for (JettyHTTPServerEngine engine : engines) {
            engine.shutdown();
        }
        // clean up the collections
        threadingParametersMap.clear();
        tlsParametersMap.clear();
        mBeanContainer = null;
    }

    public void preShutdown() {
        // do nothing here 
        // just let server registry to call the server stop first
    }
    
}
