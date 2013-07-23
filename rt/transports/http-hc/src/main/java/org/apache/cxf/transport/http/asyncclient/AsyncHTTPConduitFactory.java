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

package org.apache.cxf.transport.http.asyncclient;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.RequestAuthCache;
import org.apache.http.client.protocol.RequestClientConnControl;
import org.apache.http.client.protocol.RequestDefaultHeaders;
import org.apache.http.client.protocol.RequestProxyAuthentication;
import org.apache.http.client.protocol.RequestTargetAuthentication;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.EntityEnclosingRequestWrapper;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.impl.nio.conn.DefaultClientAsyncConnection;
import org.apache.http.impl.nio.conn.PoolingClientAsyncConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.ClientAsyncConnection;
import org.apache.http.nio.conn.ClientAsyncConnectionFactory;
import org.apache.http.nio.conn.scheme.AsyncScheme;
import org.apache.http.nio.conn.scheme.AsyncSchemeRegistry;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ssl.SSLIOSession;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

/**
 * 
 */
@NoJSR250Annotations(unlessNull = "bus")
public class AsyncHTTPConduitFactory implements BusLifeCycleListener, HTTPConduitFactory {

    
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
    
    //AsycClient specific props
    public static final String THREAD_COUNT = "org.apache.cxf.transport.http.async.ioThreadCount";
    public static final String INTEREST_OP_QUEUED = "org.apache.cxf.transport.http.async.interestOpQueued";
    public static final String SELECT_INTERVAL = "org.apache.cxf.transport.http.async.selectInterval";
    
    //CXF specific
    public static final String USE_POLICY = "org.apache.cxf.transport.http.async.usePolicy";
    
    
    public static enum UseAsyncPolicy {
        ALWAYS, ASYNC_ONLY, NEVER
    };
        
    
    final IOReactorConfig config = new IOReactorConfig();
    volatile ConnectingIOReactor ioReactor;
    volatile PoolingClientAsyncConnectionManager connectionManager;
    
    boolean isShutdown;
    UseAsyncPolicy policy;
    int maxConnections = 5000;
    int maxPerRoute = 1000;
    int connectionTTL = 60000;

    
    // these have per-instance Logger instances that have sync methods to setup.
    private final TargetAuthenticationStrategy targetAuthenticationStrategy = new TargetAuthenticationStrategy();
    private final ProxyAuthenticationStrategy proxyAuthenticationStrategy = new ProxyAuthenticationStrategy();
    private final BasicHttpProcessor httpproc;
    
    AsyncHTTPConduitFactory() {
        super();
        httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new RequestDefaultHeaders());
        // Required protocol interceptors
        httpproc.addInterceptor(new RequestContent());
        httpproc.addInterceptor(new RequestTargetHost());
        // Recommended protocol interceptors
        httpproc.addInterceptor(new RequestClientConnControl());
        httpproc.addInterceptor(new RequestUserAgent());
        httpproc.addInterceptor(new RequestExpectContinue());
        // HTTP authentication interceptors
        httpproc.addInterceptor(new RequestAuthCache());
        httpproc.addInterceptor(new RequestTargetAuthentication());
        httpproc.addInterceptor(new RequestProxyAuthentication());        

    }
    public AsyncHTTPConduitFactory(Map<String, Object> conf) {
        this();
        config.setTcpNoDelay(true);
        setProperties(conf);
    }
    
    
    public AsyncHTTPConduitFactory(Bus b) {
        this();
        addListener(b);
        config.setTcpNoDelay(true);
        setProperties(b.getProperties());
    }
    
    
    public BasicHttpProcessor getDefaultHttpProcessor() {
        return httpproc;
    }
    
    public UseAsyncPolicy getUseAsyncPolicy() {
        return policy;
    }
    
    public void update(Map<String, Object> props) {
        if (setProperties(props) && ioReactor != null) {
            restartReactor(); 
        }
    }
    private void restartReactor() {
        ConnectingIOReactor ioReactor2 = ioReactor;
        PoolingClientAsyncConnectionManager connectionManager2 = connectionManager;
        resetVars();
        shutdown(ioReactor2, connectionManager2);
    }
    private synchronized void resetVars() {
        ioReactor = null;
        connectionManager = null;
    }
    

    private boolean setProperties(Map<String, Object> s) {
        //properties that can be updated "live"
        Object st = s.get(USE_POLICY);
        if (st == null) {
            st = SystemPropertyAction.getPropertyOrNull(USE_POLICY);
        }
        if (st instanceof UseAsyncPolicy) {
            policy = (UseAsyncPolicy)st;
        } else if (st instanceof String) {
            policy = UseAsyncPolicy.valueOf((String)st);
        } else {
            //policy = UseAsyncPolicy.ALWAYS;
            policy = UseAsyncPolicy.ASYNC_ONLY;
        }
        
        maxConnections = getInt(s.get(MAX_CONNECTIONS), maxConnections);
        connectionTTL = getInt(s.get(CONNECTION_TTL), connectionTTL);
        maxPerRoute = getInt(s.get(MAX_PER_HOST_CONNECTIONS), maxPerRoute);
        if (connectionManager != null) {
            connectionManager.setMaxTotal(maxConnections);
            connectionManager.setDefaultMaxPerRoute(maxPerRoute);
        }
        
        //properties that need a restart of the reactor
        boolean changed = false;
        
        int i = config.getIoThreadCount();
        config.setIoThreadCount(getInt(s.get(THREAD_COUNT), Runtime.getRuntime().availableProcessors()));
        changed |= i != config.getIoThreadCount();
        
        long l = config.getSelectInterval();
        config.setSelectInterval(getInt(s.get(SELECT_INTERVAL), 1000));
        changed |= l != config.getSelectInterval();

        i = config.getSoLinger();
        config.setSoLinger(getInt(s.get(SO_LINGER), -1));
        changed |= i != config.getSoLinger();

        i = config.getSoTimeout();
        config.setSoTimeout(getInt(s.get(SO_TIMEOUT), 0));
        changed |= i != config.getSoTimeout();

        boolean b = config.isInterestOpQueued();
        config.setInterestOpQueued(getBoolean(s.get(INTEREST_OP_QUEUED), false));
        changed |= b != config.isInterestOpQueued();
        
        b = config.isTcpNoDelay();
        config.setTcpNoDelay(getBoolean(s.get(TCP_NODELAY), true));
        changed |= b != config.isTcpNoDelay();

        b = config.isSoKeepalive();
        config.setSoKeepalive(getBoolean(s.get(SO_KEEPALIVE), false));
        changed |= b != config.isSoKeepalive();
                
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
    public HTTPConduit createConduit(HTTPTransportFactory f,
                                     Bus bus,
                                     EndpointInfo localInfo,
                                     EndpointReferenceType target) throws IOException {
       
        return createConduit(bus, localInfo, target);
    }
    
    public HTTPConduit createConduit(Bus bus,
                                     EndpointInfo localInfo,
                                     EndpointReferenceType target) throws IOException {
        if (isShutdown) {
            return null;
        }
        return new AsyncHTTPConduit(bus, localInfo, target, this);
    }

    @Resource 
    public void setBus(Bus b) {
        addListener(b);
    }
    public void initComplete() {
    }
    public synchronized void preShutdown() {
        shutdown();
    }
    public void postShutdown() {
    }    
    
    public void shutdown() {
        if (ioReactor != null) {
            shutdown(ioReactor, connectionManager);
            connectionManager = null;
            ioReactor = null;
        }
        isShutdown = true;
    }
    private static void shutdown(ConnectingIOReactor ioReactor2,
                          PoolingClientAsyncConnectionManager connectionManager2) {
        
        try {
            connectionManager2.shutdown();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            ioReactor2.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void addListener(Bus b) {
        b.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
    }
    
    
    public synchronized void setupNIOClient() throws IOReactorException {
        if (connectionManager != null) {
            return;
        }
        // Create client-side I/O reactor
        final IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(new HttpAsyncRequestExecutor(),
                                                                                new BasicHttpParams());
        ioReactor = new DefaultConnectingIOReactor(config);
        

        // Run the I/O reactor in a separate thread
        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    // Ready to go!
                    ioReactor.execute(ioEventDispatch);
                } catch (InterruptedIOException ex) {
                    System.err.println("Interrupted");
                } catch (IOException e) {
                    System.err.println("I/O error: " + e.getMessage());
                }
            }

        });
        // Start the client thread
        t.start();
        
        AsyncSchemeRegistry registry = new AsyncSchemeRegistry();
        registry.register(new AsyncScheme("http", 80, null));
        registry.register(new AsyncScheme("https", 443, null));

        connectionManager = new PoolingClientAsyncConnectionManager(ioReactor, registry, 
                                                                    connectionTTL, TimeUnit.MILLISECONDS) {
            @Override
            protected ClientAsyncConnectionFactory createClientAsyncConnectionFactory() {
                final HttpResponseFactory responseFactory = new DefaultHttpResponseFactory();
                final ByteBufferAllocator allocator = new HeapByteBufferAllocator();

                return new ClientAsyncConnectionFactory() {
                    @Override
                    public ClientAsyncConnection create(String id, IOSession iosession, HttpParams params) {
                        return new DefaultClientAsyncConnection(id, iosession, 
                                                                responseFactory, 
                                                                allocator, params) {
                            @Override
                            protected void onRequestSubmitted(HttpRequest request) {
                                super.onRequestSubmitted(request);
                                if (request instanceof EntityEnclosingRequestWrapper) {
                                    request = ((EntityEnclosingRequestWrapper)request).getOriginal();
                                }
                                if (getIOSession() instanceof SSLIOSession) {
                                    SSLIOSession sslio = (SSLIOSession)getIOSession();
                                    getIOSession().setAttribute(CXFHttpRequest.class.getName(), request);
                                    if (getIOSession().getAttribute("cxf.handshake.done") != null) {
                                        ((CXFHttpRequest)request).getOutputStream()
                                            .setSSLSession(sslio.getSSLSession());
                                    }
                                }
                            }
                        };
                    }
                };
            }
            
        };
        connectionManager.setDefaultMaxPerRoute(maxPerRoute);
        connectionManager.setMaxTotal(maxConnections);
    }
    
    public DefaultHttpAsyncClient createClient(final AsyncHTTPConduit c) throws IOException {
        if (connectionManager == null) {
            setupNIOClient();
        }
        
        DefaultHttpAsyncClient dhac = new DefaultHttpAsyncClient(connectionManager) {
            @Override
            protected HttpParams createHttpParams() {
                HttpParams params = new SyncBasicHttpParams();
                HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                HttpConnectionParams.setTcpNoDelay(params, true);
                int bufSize = c.getClient().getChunkLength() > 0 ? c.getClient().getChunkLength() : 16332;
                HttpConnectionParams.setSocketBufferSize(params, bufSize);
                HttpConnectionParams.setConnectionTimeout(params, (int)c.getClient().getConnectionTimeout());
                return params;
            }
            @Override
            protected BasicHttpProcessor createHttpProcessor() {
                return httpproc;
            }            
        };
        //CXF handles redirects ourselves
        dhac.setRedirectStrategy(new RedirectStrategy() {
            public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
                throws ProtocolException {
                return false;
            }
            public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context)
                throws ProtocolException {
                return null;
            }
        });
        dhac.setTargetAuthenticationStrategy(targetAuthenticationStrategy);
        dhac.setProxyAuthenticationStrategy(proxyAuthenticationStrategy);
        return dhac;
    }



}
