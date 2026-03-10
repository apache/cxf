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

package org.apache.cxf.transport.http.netty.server;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.transport.http.HTTPServerEngineFactoryParametersProvider;


@NoJSR250Annotations(unlessNull = "bus")
public class NettyHttpServerEngineFactory implements BusLifeCycleListener {
    private static final Logger LOG =
            LogUtils.getL7dLogger(NettyHttpServerEngineFactory.class);

    private static ConcurrentHashMap<Integer, NettyHttpServerEngine> portMap =
            new ConcurrentHashMap<>();

    private Bus bus;

    private BusLifeCycleManager lifeCycleManager;

    /**
     * This map holds the threading parameters that are to be applied
     * to new Engines when bound to the reference id.
     */
    private Map<String, ThreadingParameters> threadingParametersMap =
        new TreeMap<>();

    private Map<String, TLSServerParameters> tlsServerParametersMap =
        new TreeMap<>();

    public NettyHttpServerEngineFactory() {
        // Empty
    }

    public NettyHttpServerEngineFactory(Bus b) {
        setBus(b);
    }

    public NettyHttpServerEngineFactory(Bus b,
                                        Map<String, TLSServerParameters> tls,
                                        Map<String, ThreadingParameters> threads) {
        setBus(b);
        tlsServerParametersMap = tls;
        threadingParametersMap = threads;
    }

    public Bus getBus() {
        return bus;
    }

    /**
     * This call is used to set the bus. It should only be called once.
     *
     * @param bus
     */
    @Resource(name = "cxf")
    public final void setBus(Bus bus) {
        this.bus = bus;
        if (bus != null) {
            bus.setExtension(this, NettyHttpServerEngineFactory.class);
            lifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
            if (null != lifeCycleManager) {
                lifeCycleManager.registerLifeCycleListener(this);
            }
        }
    }

    public Map<String, TLSServerParameters> getTlsServerParametersMap() {
        return tlsServerParametersMap;
    }

    public void setTlsServerParameters(Map<String, TLSServerParameters> tlsParametersMap) {
        this.tlsServerParametersMap = tlsParametersMap;
    }

    public Map<String, ThreadingParameters> getThreadingParametersMap() {
        return threadingParametersMap;
    }

    public void setThreadingParametersMap(Map<String, ThreadingParameters> parameterMap) {
        this.threadingParametersMap = parameterMap;
    }

    public void setEnginesList(List<NettyHttpServerEngine> enginesList) {
        for (NettyHttpServerEngine engine : enginesList) {
            portMap.putIfAbsent(engine.getPort(), engine);
        }
    }


    public void initComplete() {
        // do nothing here
    }

    public void postShutdown() {
        // shut down the Netty server in the portMap
        // To avoid the CurrentModificationException,
        // do not use portMap.values directly
        NettyHttpServerEngine[] engines = portMap.values().toArray(new NettyHttpServerEngine[portMap.values().size()]);
        for (NettyHttpServerEngine engine : engines) {
            engine.shutdown();
        }
        // The engine which is in shutdown status cannot be started anymore
        portMap.clear();
        threadingParametersMap.clear();
        tlsServerParametersMap.clear();
    }

    public void preShutdown() {
        // do nothing here
        // just let server registry to call the server stop first
    }

    private static NettyHttpServerEngine getOrCreate(NettyHttpServerEngineFactory factory,
                                                     String host,
                                                     int port,
                                                     TLSServerParameters tlsParams
                                                     ) throws IOException {

        NettyHttpServerEngine ref = portMap.get(port);
        if (ref == null) {
            ref = new NettyHttpServerEngine(host, port, factory.getBus());
            if (tlsParams != null) {
                ref.setTlsServerParameters(tlsParams);
            }
            ref.finalizeConfig();
            NettyHttpServerEngine tmpRef = portMap.putIfAbsent(port, ref);
            if (tmpRef != null) {
                ref = tmpRef;
            }
        }
        return ref;
    }


    public synchronized NettyHttpServerEngine retrieveNettyHttpServerEngine(int port) {
        return portMap.get(port);
    }


    public synchronized NettyHttpServerEngine createNettyHttpServerEngine(String host, int port,
                                                                          String protocol) throws IOException {
        LOG.log(Level.FINE, "CREATING_NETTY_SERVER_ENGINE",  port);
        TLSServerParameters tlsServerParameters = null;
        if ("https".equals(protocol) && tlsServerParametersMap != null) {
            tlsServerParameters = tlsServerParametersMap.get(Integer.toString(port));
        }
        
        if (tlsServerParameters == null && bus != null) {
            final HTTPServerEngineFactoryParametersProvider provider = 
                bus.getExtension(HTTPServerEngineFactoryParametersProvider.class);
            if (provider != null) {
                tlsServerParameters = provider
                    .getDefaultTlsServerParameters(bus, host, port, protocol, null)
                    .orElse(null);
            }
        }

        NettyHttpServerEngine ref = getOrCreate(this, host, port, tlsServerParameters);
        // checking the protocol
        if (!protocol.equals(ref.getProtocol())) {
            throw new IOException("Protocol mismatch for port " + port + ": "
                    + "engine's protocol is " + ref.getProtocol()
                    + ", the url protocol is " + protocol);
        }


        return ref;
    }

    public synchronized NettyHttpServerEngine createNettyHttpServerEngine(int port,
                                                                          String protocol) throws IOException {
        return createNettyHttpServerEngine(null, port, protocol);
    }

    /**
     * This method removes the Server Engine from the port map and stops it.
     */
    public static synchronized void destroyForPort(int port) {
        NettyHttpServerEngine ref = portMap.remove(port);
        if (ref != null) {
            LOG.log(Level.FINE, "STOPPING_NETTY_SERVER_ENGINE", port);
            try {
                ref.shutdown();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


}
