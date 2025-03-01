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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.jsse.SSLContextServerParameters;
import org.apache.cxf.configuration.jsse.SSLUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.transport.HttpUriMapper;
import org.apache.cxf.transport.http.HttpServerEngineSupport;
import org.apache.cxf.transport.https.AliasedX509ExtendedKeyManager;
import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.SslClientAuthMode;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.HttpContinueReadHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.core.ServletContainerImpl;
import io.undertow.servlet.handlers.ServletPathMatches;
import io.undertow.util.CopyOnWriteMap;


public class UndertowHTTPServerEngine implements ServerEngine, HttpServerEngineSupport {

    public static final String DO_NOT_CHECK_URL_PROP = "org.apache.cxf.transports.http_undertow.DontCheckUrl";
    
    /**
     * Please use {@link HttpServerEngineSupport#ENABLE_HTTP2} instead.
     */
    @Deprecated
    public static final String ENABLE_HTTP2_PROP = "org.apache.cxf.transports.http_undertow.EnableHttp2";
    
    public static final String ENABLE_RECORD_REQUEST_START_TIME_PROP = 
        "org.apache.cxf.transports.http_undertow.EnableRecordRequestStartTime";

    private static final Logger LOG = LogUtils.getL7dLogger(UndertowHTTPServerEngine.class);

    /**
     * This is the network port for which this engine is allocated.
     */
    private int port;

    /**
     * This is the network address for which this engine is allocated.
     */
    private String host;

    /**
     * This field holds the protocol for which this engine is
     * enabled, i.e. "http" or "https".
     */
    private String protocol = "http";

    private int servantCount;

    private Undertow server;

    /**
     * This field holds the TLS ServerParameters that are programatically
     * configured. The tlsServerParamers (due to JAXB) holds the struct
     * placed by SpringConfig.
     */
    private TLSServerParameters tlsServerParameters;

    private SSLContext sslContext;

    /**
     * This boolean signfies that SpringConfig is over. finalizeConfig
     * has been called.
     */
    private boolean configFinalized;

    private ConcurrentMap<String, UndertowHTTPHandler> registedPaths =
        new CopyOnWriteMap<>();

    private boolean continuationsEnabled = true;

    private ServletContext servletContext;

    private PathHandler path;

    private int maxIdleTime = 200000;

    private org.apache.cxf.transport.http_undertow.ThreadingParameters threadingParameters;

    private List<CXFUndertowHttpHandler> handlers;
    
    private String[] includedCipherSuites;

    public UndertowHTTPServerEngine(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public UndertowHTTPServerEngine() {

    }

    @Override
    public void addServant(URL url, UndertowHTTPHandler handler) {
        if (shouldCheckUrl(handler.getBus())) {
            checkRegistedContext(url);
        }

        if (server == null) {
            try {
                // create a new undertow server instance if there is no server there
                String contextName = HttpUriMapper.getContextName(url.getPath());
                servletContext = buildServletContext(contextName);
                handler.setServletContext(servletContext);
                server = createServer(url, handler);
                server.start();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "START_UP_SERVER_FAILED_MSG", new Object[] {e.getMessage(), port});
                //problem starting server
                try {
                    server.stop();
                } catch (Exception ex) {
                    //ignore - probably wasn't fully started anyway
                }
                server = null;
                throw new Fault(new Message("START_UP_SERVER_FAILED_MSG", LOG, e.getMessage(), port), e);
            }

        } else {
            String contextName = HttpUriMapper.getContextName(url.getPath());
            try {
                servletContext = buildServletContext(contextName);
            } catch (ServletException e) {
                throw new Fault(new Message("START_UP_SERVER_FAILED_MSG", LOG, e.getMessage(), port), e);
            }
            handler.setServletContext(servletContext);

            if (handler.isContextMatchExact()) {
                path.addExactPath(url.getPath(), handler);
            } else {
                String urlPath = url.getPath();
                if (urlPath.isEmpty()) {
                    urlPath = "/";
                }
                
                path.addPrefixPath(urlPath, handler);
            }

        }

        final String smap = HttpUriMapper.getResourceBase(url.getPath());
        handler.setName(smap);
        registedPaths.put(url.getPath(), handler);
        servantCount = servantCount + 1;
    }

    @Override
    public boolean isHttp2Enabled(Bus bus) {
        Object prop = null;
        if (bus != null) {
            prop = bus.getProperty(ENABLE_HTTP2_PROP);
        }
        if (prop == null) {
            prop = SystemPropertyAction.getPropertyOrNull(ENABLE_HTTP2_PROP);
        }
        return PropertyUtils.isTrue(prop) || HttpServerEngineSupport.super.isHttp2Enabled(bus);
    }

    private ServletContext buildServletContext(String contextName)
        throws ServletException {
        ServletContainer servletContainer = new ServletContainerImpl();
        DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setClassLoader(Thread.currentThread().getContextClassLoader());
        deploymentInfo.setDeploymentName("cxf-undertow");
        deploymentInfo.setContextPath(contextName);
        ServletInfo asyncServlet = new ServletInfo(ServletPathMatches.DEFAULT_SERVLET_NAME, CxfUndertowServlet.class);
        deploymentInfo.addServlet(asyncServlet);
        servletContainer.addDeployment(deploymentInfo);
        DeploymentManager deploymentManager = servletContainer.getDeployment(deploymentInfo.getDeploymentName());
        deploymentManager.deploy();
        deploymentManager.start();
        return deploymentManager.getDeployment().getServletContext();
    }

    private Undertow createServer(URL url, UndertowHTTPHandler undertowHTTPHandler) throws Exception {
        Undertow.Builder result = Undertow.builder();
        result.setServerOption(UndertowOptions.IDLE_TIMEOUT, getMaxIdleTime());
        if (this.isHttp2Enabled(undertowHTTPHandler.getBus())) {
            result.setServerOption(UndertowOptions.ENABLE_HTTP2, Boolean.TRUE);
        }
        if (this.shouldEnableRecordRequestStartTime(undertowHTTPHandler.getBus())) {
            result.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, Boolean.TRUE);
        }
        if (tlsServerParameters != null) {
            if (this.sslContext == null) {
                this.sslContext = createSSLContext();
            }
            result = result.addHttpsListener(getPort(), getHost(), this.sslContext);
        } else {
            result = result.addHttpListener(getPort(), getHost());
        }
        path = Handlers.path(new NotFoundHandler());

        if (url.getPath().length() == 0) {
            result = result.setHandler(Handlers.trace(undertowHTTPHandler));
        } else {
            if (undertowHTTPHandler.isContextMatchExact()) {
                path.addExactPath(url.getPath(), undertowHTTPHandler);
            } else {
                path.addPrefixPath(url.getPath(), undertowHTTPHandler);
            }

            result = result.setHandler(wrapHandler(new HttpContinueReadHandler(path)));
        }

        result = decorateUndertowSocketConnection(result);
        result = disableSSLv3(result);
        result = configureThreads(result);
        return result.build();
    }

    private Builder configureThreads(Builder builder) {
        if (this.threadingParameters != null) {
            if (this.threadingParameters.isWorkerIOThreadsSet()) {
                builder = builder.setWorkerOption(Options.WORKER_IO_THREADS,
                              this.threadingParameters.getWorkerIOThreads());
            }
            if (this.threadingParameters.isMinThreadsSet()) {
                builder = builder.setWorkerOption(Options.WORKER_TASK_CORE_THREADS,
                              this.threadingParameters.getMinThreads());
            }
            if (this.threadingParameters.isMaxThreadsSet()) {
                builder = builder.setWorkerOption(Options.WORKER_TASK_MAX_THREADS,
                              this.threadingParameters.getMaxThreads());
            }
            if (this.threadingParameters.isWorkerIONameSet()) {
                builder = builder.setWorkerOption(Options.WORKER_NAME,
                              this.threadingParameters.getWorkerIOName());
            }
        }
        
        return builder;
    }

    private HttpHandler wrapHandler(HttpHandler handler) {
        HttpHandler nextHandler = handler;
        for (CXFUndertowHttpHandler h : getHandlers()) {
            h.setNext(nextHandler);
            nextHandler = h;
        }
        return nextHandler;
    }

    private Builder disableSSLv3(Builder result) {
        final List<String> defaultProtocols = Arrays.asList(TLSServerParameters.getPreferredServerProtocols());
        //SSLv3 isn't safe, disable it by default unless explicitly use it
        if (tlsServerParameters != null
            && ("SSLv3".equals(tlsServerParameters.getSecureSocketProtocol())
                || !tlsServerParameters.getIncludeProtocols().isEmpty())) {
            final List<String> protocols = new LinkedList<>(defaultProtocols);
            protocols.add("SSLv3");
            for (String excludedProtocol : tlsServerParameters.getExcludeProtocols()) {
                if (protocols.contains(excludedProtocol)) {
                    protocols.remove(excludedProtocol);
                }
            }
            final Sequence<String> supportProtocols = Sequence.of(protocols);
            return result.setSocketOption(Options.SSL_ENABLED_PROTOCOLS, supportProtocols);
        }
        final Sequence<String> supportProtocols = Sequence.of(defaultProtocols);
        return result.setSocketOption(Options.SSL_ENABLED_PROTOCOLS, supportProtocols);
    }


    public Undertow.Builder decorateUndertowSocketConnection(Undertow.Builder builder) {
        if (this.tlsServerParameters != null && this.tlsServerParameters.getClientAuthentication() != null
            && this.tlsServerParameters.getClientAuthentication().isRequired()) {
            builder = builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE, SslClientAuthMode.REQUIRED);
        }
        if (this.tlsServerParameters != null && this.tlsServerParameters.getClientAuthentication() != null
            && this.tlsServerParameters.getClientAuthentication().isSetWant()
            && this.tlsServerParameters.getClientAuthentication().isWant()
            && !this.tlsServerParameters.getClientAuthentication().isRequired()) {
            builder = builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE, SslClientAuthMode.REQUESTED);
        }
        if (includedCipherSuites != null) {
            builder = builder.setSocketOption(Options.SSL_ENABLED_CIPHER_SUITES, 
                                              Sequence.of(includedCipherSuites));
        }
        return builder;
    }

    private boolean shouldCheckUrl(Bus bus) {

        Object prop = null;
        if (bus != null) {
            prop = bus.getProperty(DO_NOT_CHECK_URL_PROP);
        }
        if (prop == null) {
            prop = SystemPropertyAction.getPropertyOrNull(DO_NOT_CHECK_URL_PROP);
        }
        return !PropertyUtils.isTrue(prop);
    }
    
    private boolean shouldEnableRecordRequestStartTime(Bus bus) {

        Object prop = null;
        if (bus != null) {
            prop = bus.getProperty(ENABLE_RECORD_REQUEST_START_TIME_PROP);
        }
        if (prop == null) {
            prop = SystemPropertyAction.getPropertyOrNull(ENABLE_RECORD_REQUEST_START_TIME_PROP);
        }
        return PropertyUtils.isTrue(prop);
    }

    protected void checkRegistedContext(URL url) {

        String urlPath = url.getPath();
        for (String registedPath : registedPaths.keySet()) {
            if (urlPath.equals(registedPath)) {
                throw new Fault(new Message("ADD_HANDLER_CONTEXT_IS_USED_MSG", LOG, url, registedPath));
            }
            // There are some context path conflicts which could cause the UndertowHTTPServerEngine
            // doesn't route the message to the right UndertowHTTPHandler
            if (urlPath.equals(HttpUriMapper.getContextName(registedPath))) {
                throw new Fault(new Message("ADD_HANDLER_CONTEXT_IS_USED_MSG", LOG, url, registedPath));
            }
            if (registedPath.equals(HttpUriMapper.getContextName(urlPath))) {
                throw new Fault(new Message("ADD_HANDLER_CONTEXT_CONFILICT_MSG", LOG, url, registedPath));
            }
        }

    }

    @Override
    public void removeServant(URL url) {
        UndertowHTTPHandler handler = registedPaths.remove(url.getPath());
        if (handler == null) {
            return;
        }
        --servantCount;
        if (url.getPath().isEmpty()) {
            return;
        }
        if (handler.isContextMatchExact()) {
            path.removeExactPath(url.getPath());
        } else {
            path.removePrefixPath(url.getPath());
        }
    }

    @Override
    public UndertowHTTPHandler getServant(URL url) {
        return registedPaths.get(url.getPath());
    }

    /**
     * Returns the protocol "http" or "https" for which this engine
     * was configured.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Returns the port number for which this server engine was configured.
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the host for which this server engine was configured.
     * @return
     */
    public String getHost() {
        return host;
    }

    public void setPort(int p) {
        port = p;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void finalizeConfig() {
        retrieveListenerFactory();
        this.configFinalized = true;
    }

    /**
     * This method is used to programmatically set the TLSServerParameters.
     * This method may only be called by the factory.
     * @throws IOException
     */
    public void setTlsServerParameters(TLSServerParameters params) {

        tlsServerParameters = params;
        if (this.configFinalized) {
            this.retrieveListenerFactory();
        }
    }

    private void retrieveListenerFactory() {
        if (tlsServerParameters != null) {
            protocol = "https";

        } else {
            protocol = "http";
        }
        LOG.fine("Configured port " + port + " for \"" + protocol + "\".");
    }


    /**
     * This method returns the programmatically set TLSServerParameters, not
     * the TLSServerParametersType, which is the JAXB generated type used
     * in SpringConfiguration.
     * @return
     */
    public TLSServerParameters getTlsServerParameters() {
        return tlsServerParameters;
    }


    public void stop() {
        if (this.server != null) {
            this.server.stop();
        }
    }

    /**
     * This method will shut down the server engine and
     * remove it from the factory's cache.
     */
    public void shutdown() {
        if (shouldDestroyPort()) {
            if (servantCount == 0) {
                UndertowHTTPServerEngineFactory.destroyForPort(port);
                registedPaths.clear();
            } else {
                LOG.log(Level.WARNING, "FAILED_TO_SHUTDOWN_ENGINE_MSG", port);
            }
        }
    }

    private boolean shouldDestroyPort() {
        //if we shutdown the port, on SOME OS's/JVM's, if a client
        //in the same jvm had been talking to it at some point and keep alives
        //are on, then the port is held open for about 60 seconds
        //afterwards and if we restart, connections will then
        //get sent into the old stuff where there are
        //no longer any servant registered.   They pretty much just hang.

        //this is most often seen in our unit/system tests that
        //test things in the same VM.

        String s = SystemPropertyAction
                .getPropertyOrNull("org.apache.cxf.transports.http_undertow.DontClosePort." + port);
        if (s == null) {
            s = SystemPropertyAction
                .getPropertyOrNull("org.apache.cxf.transports.http_undertow.DontClosePort");
        }
        return !Boolean.valueOf(s);
    }


    protected SSLContext createSSLContext() throws Exception  {
        // The full SSL context is provided by SSLContextServerParameters
        if (tlsServerParameters instanceof SSLContextServerParameters sslContextServerParameters) {
            return sslContextServerParameters.getSslContext();
        }

        String proto = tlsServerParameters.getSecureSocketProtocol() == null
            ? "TLS" : tlsServerParameters.getSecureSocketProtocol();

        SSLContext context = tlsServerParameters.getJsseProvider() == null
            ? SSLContext.getInstance(proto)
                : SSLContext.getInstance(proto, tlsServerParameters.getJsseProvider());

        KeyManager[] keyManagers = tlsServerParameters.getKeyManagers();
        if (tlsServerParameters.getCertAlias() != null) {
            keyManagers = getKeyManagersWithCertAlias(keyManagers);
        }
        context.init(keyManagers,
                     tlsServerParameters.getTrustManagers(),
                     tlsServerParameters.getSecureRandom());
        
        // Set the CipherSuites
        final String[] supportedCipherSuites =
            SSLUtils.getServerSupportedCipherSuites(context);

        
        includedCipherSuites =
            SSLUtils.getCiphersuitesToInclude(tlsServerParameters.getCipherSuites(),
                                              tlsServerParameters.getCipherSuitesFilter(),
                                              context.getServerSocketFactory().getDefaultCipherSuites(),
                                              supportedCipherSuites,
                                              LOG);

        return context;
    }

    protected KeyManager[] getKeyManagersWithCertAlias(KeyManager[] keyManagers) throws Exception {
        if (tlsServerParameters.getCertAlias() != null) {
            for (int idx = 0; idx < keyManagers.length; idx++) {
                if (keyManagers[idx] instanceof X509KeyManager) {
                    keyManagers[idx] = new AliasedX509ExtendedKeyManager(
                        tlsServerParameters.getCertAlias(), (X509KeyManager)keyManagers[idx]);
                }
            }
        }
        return keyManagers;
    }

    /**
     * This method sets the threading parameters for this particular
     * server engine.
     * This method may only be called by the factory.
     */
    public void setThreadingParameters(ThreadingParameters params) {
        threadingParameters = params;
    }

    /**
     * This method returns whether the threading parameters are set.
     */
    public boolean isSetThreadingParameters() {
        return threadingParameters != null;
    }

    /**
     * This method returns the threading parameters that have been set.
     * This method may return null, if the threading parameters have not
     * been set.
     */
    public ThreadingParameters getThreadingParameters() {
        return threadingParameters;
    }

    public void setContinuationsEnabled(boolean enabled) {
        continuationsEnabled = enabled;
    }

    public boolean getContinuationsEnabled() {
        return continuationsEnabled;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }


    /**
     * set the Undertow server's handlers
     * @param h
     */

    public void setHandlers(List<CXFUndertowHttpHandler> h) {
        handlers = h;
    }

    public List<CXFUndertowHttpHandler> getHandlers() {
        return handlers != null ? handlers : new ArrayList<>();
    }
}
