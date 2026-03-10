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
package org.apache.cxf.transport.http_undertow;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;

import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.transport.http.HTTPServerEngineFactoryParametersProvider;



/**
 * This Bus Extension handles the configuration of network port
 * numbers for use with "http" or "https". This factory
 * caches the UndertowHTTPServerEngines so that they may be
 * retrieved if already previously configured.
 */
@NoJSR250Annotations(unlessNull = "bus")
public class UndertowHTTPServerEngineFactory {

    private static final Logger LOG =
        LogUtils.getL7dLogger(UndertowHTTPServerEngineFactory.class);

    private static final int FALLBACK_THREADING_PARAMS_KEY = 0;

    /**
     * This map holds references for allocated ports.
     */
    // Still use the static map to hold the port information
    // in the same JVM
    private static ConcurrentHashMap<Integer, UndertowHTTPServerEngine> portMap =
        new ConcurrentHashMap<>();



    private BusLifeCycleManager lifeCycleManager;
    /**
     * This map holds the threading parameters that are to be applied
     * to new Engines when bound to the reference id.
     */
    private Map<String, ThreadingParameters> threadingParametersMap =
        new TreeMap<>();

    private ThreadingParameters fallbackThreadingParameters;

    /**
     * This map holds TLS Server Parameters that are to be used to
     * configure a subsequently created UndertowHTTPServerEngine.
     */
    private Map<String, TLSServerParameters> tlsParametersMap =
        new TreeMap<>();


    /**
     * The bus.
     */
    private Bus bus;


    public UndertowHTTPServerEngineFactory() {
        // Empty
    }
    public UndertowHTTPServerEngineFactory(Bus b) {
        setBus(b);
    }
    public UndertowHTTPServerEngineFactory(Bus b,
                                        Map<String, TLSServerParameters> tls,
                                        Map<String, ThreadingParameters> threading) {
        tlsParametersMap.putAll(tls);
        threadingParametersMap.putAll(threading);
        setBus(b);
    }

    private static UndertowHTTPServerEngine getOrCreate(UndertowHTTPServerEngineFactory factory,
                    String host,
                    int port,
                    TLSServerParameters tlsParams) throws IOException, GeneralSecurityException {

        UndertowHTTPServerEngine ref = portMap.get(port);
        if (ref == null) {
            ref = new UndertowHTTPServerEngine(host, port);
            if (tlsParams != null) {
                ref.setTlsServerParameters(tlsParams);
            }
            UndertowHTTPServerEngine tmpRef = portMap.putIfAbsent(port, ref);
            ref.finalizeConfig();
            if (tmpRef != null) {
                ref = tmpRef;
            }
        }
        return ref;
    }


    /**
     * This call is used to set the bus. It should only be called once.
     * @param bus
     */
    @Resource(name = "cxf")
    public final void setBus(Bus bus) {
        this.bus = bus;
        if (bus != null) {
            bus.setExtension(this, UndertowHTTPServerEngineFactory.class);
            lifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
            if (null != lifeCycleManager) {
                lifeCycleManager.registerLifeCycleListener(new UndertowBusLifeCycleListener());
            }
        }
    }
    private final class UndertowBusLifeCycleListener implements BusLifeCycleListener {
        public void initComplete() {
            UndertowHTTPServerEngineFactory.this.initComplete();
        }

        public void preShutdown() {
            UndertowHTTPServerEngineFactory.this.preShutdown();
        }

        public void postShutdown() {
            UndertowHTTPServerEngineFactory.this.postShutdown();
        }
    }

    public Bus getBus() {
        return bus;
    }


    /**
     * This call sets TLSParametersMap for a UndertowHTTPServerEngine
     *
     */
    public void setTlsServerParametersMap(
        Map<String, TLSServerParameters>  tlsParamsMap) {

        tlsParametersMap = tlsParamsMap;
    }

    public Map<String, TLSServerParameters> getTlsServerParametersMap() {
        return tlsParametersMap;
    }

    public void setEnginesList(List<UndertowHTTPServerEngine> enginesList) {
        for (UndertowHTTPServerEngine engine : enginesList) {
            if (engine.getPort() == FALLBACK_THREADING_PARAMS_KEY) {
                fallbackThreadingParameters = engine.getThreadingParameters();
            }
            portMap.putIfAbsent(engine.getPort(), engine);
        }
    }

    /**
     * This call sets the ThreadingParameters for a UndertowHTTPServerEngine
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
     * This call sets TLSServerParameters for a UndertowHTTPServerEngine
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
        UndertowHTTPServerEngine ref = retrieveUndertowHTTPServerEngine(port);
        if (null == ref) {
            getOrCreate(this, host, port, tlsParams);
        } else {
            ref.setTlsServerParameters(tlsParams);
        }
    }

    /**
     * calls thru to {{@link #createUndertowHTTPServerEngine(String, int, String)} with 'null' for host value
     */
    public void setTLSServerParametersForPort(
        int port,
        TLSServerParameters tlsParams) throws GeneralSecurityException, IOException {
        setTLSServerParametersForPort(null, port, tlsParams);
    }

    /**
     * This call retrieves a previously configured UndertowHTTPServerEngine for the
     * given port. If none exists, this call returns null.
     */
    public synchronized UndertowHTTPServerEngine retrieveUndertowHTTPServerEngine(int port) {
        return portMap.get(port);
    }

    /**
     * This call creates a new UndertowHTTPServerEngine initialized for "http"
     * or "https" on the given port. The determination of "http" or "https"
     * will depend on configuration of the engine's bean name.
     *
     * If an UndertowHTTPEngine already exists, or the port
     * is already in use, a BindIOException will be thrown. If the
     * engine is being Spring configured for TLS a GeneralSecurityException
     * may be thrown.
     *
     * @param host if not null, server will listen on this host/address, otherwise
     *        server will listen on all local addresses.
     * @param port listen port for server
     * @param protocol "http" or "https"
     * @param id The key to reference into the tlsParametersMap. Can be null.
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public synchronized UndertowHTTPServerEngine createUndertowHTTPServerEngine(String host, int port,
        String protocol, String id) throws GeneralSecurityException, IOException {
        LOG.fine("Creating Undertow HTTP Server Engine for port " + port + ".");
        TLSServerParameters tlsParameters = null;
        if (id != null && tlsParametersMap != null && tlsParametersMap.containsKey(id)) {
            tlsParameters = tlsParametersMap.get(id);
        }

        if (tlsParameters == null && bus != null) {
            final HTTPServerEngineFactoryParametersProvider provider = 
                bus.getExtension(HTTPServerEngineFactoryParametersProvider.class);
            if (provider != null) {
                tlsParameters = provider.getDefaultTlsServerParameters(bus, host, port, protocol, id).orElse(null);
            }
        }

        UndertowHTTPServerEngine ref = getOrCreate(this, host, port, tlsParameters);
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
                        new Object[] {port, min, max, ""});
            }
            ref.setThreadingParameters(fallbackThreadingParameters);
        }

        return ref;
    }

    /**
     * Calls thru to {{@link #createUndertowHTTPServerEngine(String, int, String)} with a 'null' host value
     */
    public synchronized UndertowHTTPServerEngine createUndertowHTTPServerEngine(int port,
        String protocol) throws GeneralSecurityException, IOException {
        return createUndertowHTTPServerEngine(null, port, protocol);
    }
    
    
    public synchronized UndertowHTTPServerEngine createUndertowHTTPServerEngine(String host, int port,
                                                                          String protocol)
        throws GeneralSecurityException, IOException {
        return createUndertowHTTPServerEngine(host, port, protocol, null);
    }

    /**
     * This method removes the Server Engine from the port map and stops it.
     */
    public static synchronized void destroyForPort(int port) {
        UndertowHTTPServerEngine ref = portMap.remove(port);
        if (ref != null) {
            LOG.fine("Stopping Undertow HTTP Server Engine on port " + port + ".");
            try {
                ref.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public MBeanServer getMBeanServer() {
        if (bus != null && bus.getExtension(InstrumentationManager.class) != null) {
            return bus.getExtension(InstrumentationManager.class).getMBeanServer();
        }
        return null;
    }


    public void initComplete() {
        // do nothing here
    }

    public void postShutdown() {
        // shut down the Undertow server in the portMap
        // To avoid the CurrentModificationException,
        // do not use portMap.values directly
        UndertowHTTPServerEngine[] engines =
            portMap.values().toArray(new UndertowHTTPServerEngine[portMap.values().size()]);
        for (UndertowHTTPServerEngine engine : engines) {
            engine.shutdown();
        }
        // clean up the collections
        threadingParametersMap.clear();
        tlsParametersMap.clear();
    }

    public void preShutdown() {
        // do nothing here
        // just let server registry to call the server stop first
    }




}
