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
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.EntityEnclosingRequestWrapper;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
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
import org.apache.http.params.HttpParams;

/**
 * 
 */
@NoJSR250Annotations(unlessNull = "bus")
public class AsyncHTTPConduitFactory implements BusLifeCycleListener, HTTPConduitFactory {

    
    //TCP related properties
    public static final String TCP_NODELAY = "org.apache.cxf.transport.http.async.TCP_NODELAY";
    public static final String SO_KEEPALIVE = "org.apache.cxf.transport.http.async.SO_KEEPALIVE";
    public static final String SO_LINGER = "org.apache.cxf.transport.http.async.SO_LINGER";
    public static final String SO_TIMEOUT = "org.apache.cxf.transport.http.async.SO_LINGER";

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
    CXFAsyncRequester requester;
    ConnectingIOReactor ioReactor;
    PoolingClientAsyncConnectionManager connectionManager;
    boolean isShutdown;
    UseAsyncPolicy policy;
    
    
    public AsyncHTTPConduitFactory(Map<String, Object> conf) {
        super();
        System.out.println(conf);
        config.setTcpNoDelay(true);
        setProperties(conf);
    }
    
    
    public AsyncHTTPConduitFactory(Bus b) {
        addListener(b);
        config.setTcpNoDelay(true);
        setProperties(b.getProperties());
    }
    
    public UseAsyncPolicy getUseAsyncPolicy() {
        return policy;
    }
    
    private void setProperties(Map<String, Object> s) {
        config.setIoThreadCount(getInt(s.get(THREAD_COUNT), Runtime.getRuntime().availableProcessors()));
        config.setInterestOpQueued(getBoolean(s.get(INTEREST_OP_QUEUED), false));
        config.setSelectInterval(getInt(s.get(SO_LINGER), 1000));
        
        config.setTcpNoDelay(getBoolean(s.get(TCP_NODELAY), true));
        config.setSoLinger(getInt(s.get(SO_LINGER), -1));
        config.setSoKeepalive(getBoolean(s.get(SO_KEEPALIVE), false));
        config.setSoTimeout(getInt(s.get(SO_TIMEOUT), 0));
        
        Object st = s.get(USE_POLICY);
        if (st == null) {
            st = SystemPropertyAction.getPropertyOrNull(USE_POLICY);
        }
        if (st instanceof UseAsyncPolicy) {
            policy = (UseAsyncPolicy)st;
        } else if (st instanceof String) {
            policy = UseAsyncPolicy.valueOf((String)st);
        } else {
            policy = UseAsyncPolicy.ASYNC_ONLY;
        }
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
                                     EndpointInfo localInfo,
                                     EndpointReferenceType target) throws IOException {
        if (isShutdown) {
            return null;
        }
        return new AsyncHTTPConduit(f.getBus(), localInfo, target, this);
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
            try {
                connectionManager.shutdown();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            try {
                ioReactor.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connectionManager = null;
            ioReactor = null;
            requester = null;
        }
        isShutdown = true;
    }
    
    private void addListener(Bus b) {
        b.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
    }
    
    
    public synchronized void setupNIOClient() throws IOReactorException {
        if (requester != null) {
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

        connectionManager = new PoolingClientAsyncConnectionManager(ioReactor, registry) {
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
        connectionManager.setDefaultMaxPerRoute(2500);
        connectionManager.setMaxTotal(5000);
        requester = new CXFAsyncRequester(connectionManager);
    }
    
    public CXFAsyncRequester getRequester() throws IOException {
        if (requester == null) {
            setupNIOClient();
        }

        return requester;
    }



}
