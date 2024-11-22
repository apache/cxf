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

package org.apache.cxf.transport.http.asyncclient.hc5;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

/**
 *
 */
@NoJSR250Annotations
public class AsyncHTTPConduitFactory implements HTTPConduitFactory {
    //TCP related properties
    public static final String TCP_NODELAY = "org.apache.cxf.transport.http.async.TCP_NODELAY";
    public static final String SO_KEEPALIVE = "org.apache.cxf.transport.http.async.SO_KEEPALIVE";
    public static final String SO_LINGER = "org.apache.cxf.transport.http.async.SO_LINGER";
    public static final String SO_TIMEOUT = "org.apache.cxf.transport.http.async.SO_TIMEOUT";

    //ConnectionPool
    public static final String MAX_CONNECTIONS = "org.apache.cxf.transport.http.async.MAX_CONNECTIONS";
    public static final String MAX_PER_HOST_CONNECTIONS
        = "org.apache.cxf.transport.http.async.MAX_PER_HOST_CONNECTIONS";
    public static final String CONNECTION_TTL = "org.apache.cxf.transport.http.async.CONNECTION_TTL";
    public static final String CONNECTION_MAX_IDLE = "org.apache.cxf.transport.http.async.CONNECTION_MAX_IDLE";

    //AsycClient specific props
    public static final String THREAD_COUNT = "org.apache.cxf.transport.http.async.ioThreadCount";
    public static final String SELECT_INTERVAL = "org.apache.cxf.transport.http.async.selectInterval";

    //CXF specific
    public static final String USE_POLICY = "org.apache.cxf.transport.http.async.usePolicy";

    private static final Logger LOG = LogUtils.getL7dLogger(AsyncHTTPConduitFactory.class);
    
    
    public enum UseAsyncPolicy {
        ALWAYS, ASYNC_ONLY, NEVER;

        public static UseAsyncPolicy getPolicy(Object st) {
            if (st instanceof UseAsyncPolicy) {
                return (UseAsyncPolicy)st;
            } else if (st instanceof String) {
                String s = ((String)st).toUpperCase();
                if ("ALWAYS".equals(s)) {
                    return ALWAYS;
                } else if ("NEVER".equals(s)) {
                    return NEVER;
                } else if ("ASYNC_ONLY".equals(s)) {
                    return ASYNC_ONLY;
                } else {
                    st = Boolean.parseBoolean(s);
                }
            }
            if (st instanceof Boolean) {
                return ((Boolean)st).booleanValue() ? ALWAYS : NEVER;
            }
            return ASYNC_ONLY;
        }
    };
    
    /**
     * See please https://issues.apache.org/jira/browse/CXF-8678 and 
     * https://issues.apache.org/jira/browse/HTTPCLIENT-2209, the context propagation
     * is necessary to remove per-HTTPClientPolicy caching.
     */
    private static class AsyncClient {
        private final PoolingAsyncClientConnectionManager connectionManager;
        private final CloseableHttpAsyncClient client;
        
        AsyncClient(PoolingAsyncClientConnectionManager connectionManager, CloseableHttpAsyncClient client) {
            this.connectionManager = connectionManager;
            this.client = client;
        }
        
        public CloseableHttpAsyncClient getClient() {
            return client;
        }
        
        public PoolingAsyncClientConnectionManager getConnectionManager() {
            return connectionManager;
        }
    }

    private volatile Map<HTTPClientPolicy, AsyncClient> clients = new ConcurrentHashMap<>();

    private boolean isShutdown;
    private UseAsyncPolicy policy;
    private int maxConnections = 5000;
    private int maxPerRoute = 1000;
    private int connectionTTL = 60000;
    private int connectionMaxIdle = 60000;

    private int ioThreadCount = IOReactorConfig.DEFAULT.getIoThreadCount();
    private long selectInterval = IOReactorConfig.DEFAULT.getSelectInterval().toMilliseconds();
    private int soLinger = IOReactorConfig.DEFAULT.getSoLinger().toMillisecondsIntBound();
    private int soTimeout = IOReactorConfig.DEFAULT.getSoTimeout().toMillisecondsIntBound();
    private boolean soKeepalive = IOReactorConfig.DEFAULT.isSoKeepalive();
    private boolean tcpNoDelay = true;


    AsyncHTTPConduitFactory() {
        super();
    }

    public AsyncHTTPConduitFactory(Map<String, Object> conf) {
        this();
        setProperties(conf);
    }

    public AsyncHTTPConduitFactory(Bus b) {
        this();
        addListener(b);
        setProperties(b.getProperties());
    }

    public UseAsyncPolicy getUseAsyncPolicy() {
        return policy;
    }

    public void update(Map<String, Object> props) {
        if (setProperties(props) && !clients.isEmpty()) {
            restartReactor();
        }
    }

    private void restartReactor() {
        final Map<HTTPClientPolicy, AsyncClient> clients2 = clients;
        clients = new ConcurrentHashMap<>();
        shutdown(clients2);
    }

    private boolean setProperties(Map<String, Object> s) {
        //properties that can be updated "live"
        if (s == null) {
            return false;
        }
        Object st = s.get(USE_POLICY);
        if (st == null) {
            st = SystemPropertyAction.getPropertyOrNull(USE_POLICY);
        }
        policy = UseAsyncPolicy.getPolicy(st);

        maxConnections = getInt(s.get(MAX_CONNECTIONS), maxConnections);
        connectionTTL = getInt(s.get(CONNECTION_TTL), connectionTTL);
        connectionMaxIdle = getInt(s.get(CONNECTION_MAX_IDLE), connectionMaxIdle);
        maxPerRoute = getInt(s.get(MAX_PER_HOST_CONNECTIONS), maxPerRoute);

        if (!clients.isEmpty()) {
            for (Map.Entry<HTTPClientPolicy, AsyncClient> entry: clients.entrySet()) {
                final PoolingAsyncClientConnectionManager connectionManager = entry.getValue().getConnectionManager();
                connectionManager.setMaxTotal(maxConnections);
                connectionManager.setDefaultMaxPerRoute(maxPerRoute);
            }
        }

        //properties that need a restart of the reactor
        boolean changed = false;

        int i = ioThreadCount;
        ioThreadCount = getInt(s.get(THREAD_COUNT), Runtime.getRuntime().availableProcessors());
        changed |= i != ioThreadCount;

        long l = selectInterval;
        selectInterval = getInt(s.get(SELECT_INTERVAL), 1000);
        changed |= l != selectInterval;

        i = soLinger;
        soLinger = getInt(s.get(SO_LINGER), -1);
        changed |= i != soLinger;

        i = soTimeout;
        soTimeout = getInt(s.get(SO_TIMEOUT), 0);
        changed |= i != soTimeout;

        boolean b = tcpNoDelay;
        tcpNoDelay = getBoolean(s.get(TCP_NODELAY), true);
        changed |= b != tcpNoDelay;

        b = soKeepalive;
        soKeepalive = getBoolean(s.get(SO_KEEPALIVE), false);
        changed |= b != soKeepalive;

        return changed;
    }

    private int getInt(Object s, int defaultv) {
        int i = defaultv;
        if (s instanceof String) {
            i = Integer.parseInt((String)s);
        } else if (s instanceof Number) {
            i = ((Number)s).intValue();
        }
        if (i == -1) {
            i = defaultv;
        }
        return i;
    }

    private boolean getBoolean(Object s, boolean defaultv) {
        if (s instanceof String) {
            return Boolean.parseBoolean((String)s);
        } else if (s instanceof Boolean) {
            return ((Boolean)s).booleanValue();
        }
        return defaultv;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public HTTPConduit createConduit(HTTPTransportFactory f, Bus bus, EndpointInfo localInfo,
            EndpointReferenceType target) throws IOException {
        return createConduit(bus, localInfo, target);
    }

    public HTTPConduit createConduit(Bus bus, EndpointInfo localInfo, 
            EndpointReferenceType target) throws IOException {
        if (isShutdown) {
            return null;
        }
        if (HTTPTransportFactory.isForceURLConnectionConduit()) {
            return new URLConnectionAsyncHTTPConduit(bus, localInfo, target, this);
        } else {
            return new AsyncHTTPConduit(bus, localInfo, target, this);
        }
    }

    public void shutdown() {
        shutdown(clients);
        clients.clear();
        isShutdown = true;
    }

    private static void shutdown(Map<HTTPClientPolicy, AsyncClient> clients) {
        if (!clients.isEmpty()) {
            for (Map.Entry<HTTPClientPolicy, AsyncClient> entry: clients.entrySet()) {
                shutdown(entry.getValue().getClient());
                entry.getValue().getConnectionManager().close();
            }
        }
    }

    private static void shutdown(CloseableHttpAsyncClient client) {
        try {
            client.close();
        } catch (IOException ex) {
            LOG.warning(ex.getMessage());
        }
    }

    private void addListener(Bus b) {
        BusLifeCycleManager manager = b.getExtension(BusLifeCycleManager.class);
        if (manager != null) {
            manager.registerLifeCycleListener(new BusLifeCycleListener() {
                public void initComplete() {
                }
                public void preShutdown() {
                    shutdown();
                }
                public void postShutdown() {
                }
            });
        }
    }

    public synchronized void setupNIOClient(HTTPClientPolicy clientPolicy, final TlsStrategy tlsStrategy) {
        final AsyncClient client = clients.get(clientPolicy);

        if (client != null) {
            return;
        }
        
        clients.computeIfAbsent(clientPolicy, key -> createNIOClient(key, tlsStrategy));
    }
    
    private AsyncClient createNIOClient(HTTPClientPolicy clientPolicy, final TlsStrategy tlsStrategy) {
        final IOReactorConfig config = IOReactorConfig.custom()
            .setIoThreadCount(ioThreadCount)
            .setSelectInterval(TimeValue.ofMilliseconds(selectInterval))
            .setSoLinger(TimeValue.ofMilliseconds(soLinger))
            .setSoTimeout(Timeout.ofMilliseconds(soTimeout))
            .setSoKeepAlive(soKeepalive)
            .setTcpNoDelay(tcpNoDelay)
            .build();

        
        final Lookup<TlsStrategy> tlsLookupStrategy = RegistryBuilder.<TlsStrategy>create()
            .register("https", (tlsStrategy != null) ? tlsStrategy : DefaultClientTlsStrategy.createSystemDefault())
            .build();

        final PoolingAsyncClientConnectionManager connectionManager = new PoolingAsyncClientConnectionManager(
            tlsLookupStrategy,
            PoolConcurrencyPolicy.STRICT,
            PoolReusePolicy.LIFO,
            TimeValue.ofMilliseconds(connectionTTL),
            DefaultSchemePortResolver.INSTANCE,
            SystemDefaultDnsResolver.INSTANCE);

        connectionManager.setDefaultMaxPerRoute(maxPerRoute);
        connectionManager.setMaxTotal(maxConnections);

        if (!"2.0".equals(clientPolicy.getVersion())) {
            connectionManager.setDefaultTlsConfig(TlsConfig
                .custom()
                .setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1)
                .build());
        }

        final RedirectStrategy redirectStrategy = new RedirectStrategy() {
            public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
                    throws ProtocolException {
                return false;
            }
            public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context)
                    throws ProtocolException {
                return null;
            }
        };

        final HttpAsyncClientBuilder httpAsyncClientBuilder = HttpAsyncClients
            .custom()
            .setConnectionManager(connectionManager)
            .setRedirectStrategy(redirectStrategy)
            .setDefaultCookieStore(new BasicCookieStore() {
                private static final long serialVersionUID = 1L;
                public void addCookie(Cookie cookie) {
                }
            });
        adaptClientBuilder(httpAsyncClientBuilder);

        final CloseableHttpAsyncClient client = httpAsyncClientBuilder
            .setIOReactorConfig(config)
            .build();

        // Start the client thread
        client.start();
        //Always start the idle checker thread to validate pending requests and
        //use the ConnectionMaxIdle to close the idle connection
        new CloseIdleConnectionThread(connectionManager, client).start();

        return new AsyncClient(connectionManager, client);
    }

    //provide a hook to customize the builder
    protected void adaptClientBuilder(HttpAsyncClientBuilder httpAsyncClientBuilder) {
    }

    public CloseableHttpAsyncClient createClient(final URLConnectionHTTPConduit c, final TlsStrategy tlsStrategy) 
            throws IOException {

        return clients
            .computeIfAbsent(c.getClient(), key -> createNIOClient(key, tlsStrategy))
            .getClient();
    }
    
    int getMaxConnections() {
        return maxConnections;
    }

    public class CloseIdleConnectionThread extends Thread {
        private final PoolingAsyncClientConnectionManager connMgr;
        private final CloseableHttpAsyncClient client;

        public CloseIdleConnectionThread(PoolingAsyncClientConnectionManager connMgr, CloseableHttpAsyncClient client) {
            super("CXFCloseIdleConnectionThread");
            this.connMgr = connMgr;
            this.client = client;
        }

        @Override
        public void run() {
            long nextIdleCheck = System.currentTimeMillis() + connectionMaxIdle;
            try {
                while (client.getStatus() == IOReactorStatus.ACTIVE) {
                    synchronized (this) {
                        sleep(selectInterval);

                        if (connectionTTL == 0
                            && connectionMaxIdle > 0 && System.currentTimeMillis() >= nextIdleCheck) {
                            nextIdleCheck += connectionMaxIdle;
                            // close connections
                            // that have been idle longer than specified connectionMaxIdle
                            connMgr.closeIdle(TimeValue.ofMilliseconds(connectionMaxIdle));
                        }
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }
    }

    public void close(HTTPClientPolicy clientPolicy) {
        final AsyncClient client = clients.remove(clientPolicy);
        if (client != null) {
            shutdown(client.getClient());
            client.getConnectionManager().close();
        }
    }
}
