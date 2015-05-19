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

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.jsse.SSLUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.security.ClientAuthentication;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.transport.HttpUriMapper;
import org.apache.cxf.transport.https.AliasedX509ExtendedKeyManager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;


/**
 * This class is the Jetty HTTP Server Engine that is configured to
 * work off of a designated port. The port will be enabled for 
 * "http" or "https" depending upon its successful configuration.
 */
public class JettyHTTPServerEngine implements ServerEngine {
    public static final String DO_NOT_CHECK_URL_PROP = "org.apache.cxf.transports.http_jetty.DontCheckUrl";
    
    private static final Logger LOG = LogUtils.getL7dLogger(JettyHTTPServerEngine.class);
    
   
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
    
    private Boolean isSessionSupport = false;
    private Boolean isReuseAddress = true;
    private Boolean continuationsEnabled = true;
    private int maxIdleTime = 200000;
    private Boolean sendServerVersion = true;
    private int servantCount;
    private Server server;
    private Connector connector;
    private List<Handler> handlers;
    private ContextHandlerCollection contexts;
    private Container.Listener mBeanContainer;
    private SessionManager sessionManager;
    private ThreadPool threadPool;
    
    
    /**
     * This field holds the TLS ServerParameters that are programatically
     * configured. The tlsServerParamers (due to JAXB) holds the struct
     * placed by SpringConfig.
     */
    private TLSServerParameters tlsServerParameters;
    
    /**
     * This field hold the threading parameters for this particular engine.
     */
    private ThreadingParameters threadingParameters;
    
    /**
     * This boolean signfies that SpringConfig is over. finalizeConfig
     * has been called.
     */
    private boolean configFinalized;
    
    private List<String> registedPaths = new CopyOnWriteArrayList<String>();
        
    /**
     * This constructor is called by the JettyHTTPServerEngineFactory.
     */
    public JettyHTTPServerEngine(
        Container.Listener mBeanContainer,
        String host,
        int port) {
        this.host    = host;
        this.port    = port;
        this.mBeanContainer = mBeanContainer;
    }
    
    public JettyHTTPServerEngine() {
        
    }
    public void setThreadPool(ThreadPool p) {
        threadPool = p;
    }
     
    public void setPort(int p) {
        port = p;
    }

    public void setHost(String host) {
        this.host = host;
    }
    
    public void setContinuationsEnabled(boolean enabled) {
        continuationsEnabled = enabled;
    }
    
    public boolean getContinuationsEnabled() {
        return continuationsEnabled;
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
    
    /**
     * This method will shut down the server engine and
     * remove it from the factory's cache. 
     */
    public void shutdown() {
        registedPaths.clear();
        if (shouldDestroyPort()) {
            if (servantCount == 0) {
                JettyHTTPServerEngineFactory.destroyForPort(port);
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
                .getPropertyOrNull("org.apache.cxf.transports.http_jetty.DontClosePort." + port);
        if (s == null) {
            s = SystemPropertyAction
                .getPropertyOrNull("org.apache.cxf.transports.http_jetty.DontClosePort");
        }
        return !Boolean.valueOf(s);
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
    
    /**
     * get the jetty server instance
     * @return
     */
    public Server getServer() {
        return server;
    }
    
    /**
     * Set the jetty server instance 
     * @param s 
     */
    public void setServer(Server s) {
        server = s;
    }
    
    /**
     * set the jetty server's connector
     * @param c
     */
    public void setConnector(Connector c) {
        connector = c;
    }
    
    /**
     * set the jetty server's handlers
     * @param h
     */
    
    public void setHandlers(List<Handler> h) {
        handlers = h;
    }
    
    public void setSessionSupport(boolean support) {
        isSessionSupport = support;
    }
    
    public boolean isSessionSupport() {
        return isSessionSupport;
    }
    
    public List<Handler> getHandlers() {
        return handlers;
    }
    
    public Connector getConnector() {
        return connector;
    }
    
    public boolean isReuseAddress() {
        return isReuseAddress;
    }
    
    public void setReuseAddress(boolean reuse) {
        isReuseAddress = reuse;
    }
    
    public int getMaxIdleTime() {
        return maxIdleTime;
    }
    
    public void setMaxIdleTime(int maxIdle) {
        maxIdleTime = maxIdle;
    }
    
    protected void checkRegistedContext(URL url) {
        
        String path = url.getPath();
        for (String registedPath : registedPaths) {
            if (path.equals(registedPath)) {
                throw new Fault(new Message("ADD_HANDLER_CONTEXT_IS_USED_MSG", LOG, url, registedPath));
            }
            // There are some context path conflicts which could cause the JettyHTTPServerEngine 
            // doesn't route the message to the right JettyHTTPHandler
            if (path.equals(HttpUriMapper.getContextName(registedPath))) {
                throw new Fault(new Message("ADD_HANDLER_CONTEXT_IS_USED_MSG", LOG, url, registedPath));
            }
            if (registedPath.equals(HttpUriMapper.getContextName(path))) {
                throw new Fault(new Message("ADD_HANDLER_CONTEXT_CONFILICT_MSG", LOG, url, registedPath));
            }
        }
        
    }
    
    private Server createServer() {
        Server s = null;
        if (threadPool != null) {
            try {
                if (!Server.getVersion().startsWith("8")) {
                    s = Server.class.getConstructor(ThreadPool.class).newInstance(threadPool);
                } else {
                    s = new Server();
                    Server.class.getMethod("setThreadPool", ThreadPool.class).invoke(s, threadPool);
                }
            } catch (Exception e) {
                //ignore
            }
        }
        if (s == null) {
            s = new Server();
        }
        if (!Server.getVersion().startsWith("8")) {
            //need an error handler that won't leak information about the exception 
            //back to the client.
            ErrorHandler eh = new ErrorHandler() {
                @SuppressWarnings("deprecation")
                public void handle(String target, Request baseRequest, 
                                   HttpServletRequest request, HttpServletResponse response) 
                    throws IOException {
                    String msg = (String)request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
                    if (StringUtils.isEmpty(msg) || msg.contains("org.apache.cxf.interceptor.Fault")) {
                        msg = HttpStatus.getMessage(response.getStatus());
                        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, msg);
                    }
                    if (response instanceof Response) {
                        //need to use the deprecated method to support compiling with Jetty 8
                        ((Response)response).setStatus(response.getStatus(), msg);
                    }
                    super.handle(target, baseRequest, request, response);
                }
                protected void writeErrorPage(HttpServletRequest request, Writer writer, int code,
                                              String message, boolean showStacks)
                    throws IOException {
                    super.writeErrorPage(request, writer, code, message, false);
                }
            };
            s.addBean(eh);
        }
        return s;
    }
    
    /**
     * Register a servant.
     * 
     * @param url the URL associated with the servant
     * @param handler notified on incoming HTTP requests
     */
    public synchronized void addServant(URL url, JettyHTTPHandler handler) {
        if (shouldCheckUrl(handler.getBus())) {
            checkRegistedContext(url);
        }
        initializeContexts();
        
        SecurityHandler securityHandler = null;
        if (server == null) {
            DefaultHandler defaultHandler = null;
            // create a new jetty server instance if there is no server there
            server = createServer();
            addServerMBean();
                    
            if (connector == null) {
                connector = createConnector(getHost(), getPort());
                if (LOG.isLoggable(Level.FINER)) {
                    logConnector(connector);
                }
            } 
            server.addConnector(connector);
            setupThreadPool();
            /*
             * The server may have no handler, it might have a collection handler,
             * it might have a one-shot. We need to add one or more of ours.
             *
             */
            int numberOfHandlers = 1;
            if (handlers != null) {
                numberOfHandlers += handlers.size();
            }
            Handler existingHandler = server.getHandler();

            HandlerCollection handlerCollection = null;
            boolean existingHandlerCollection = existingHandler instanceof HandlerCollection;
            if (existingHandlerCollection) {
                handlerCollection = (HandlerCollection) existingHandler;
            }

            if (!existingHandlerCollection 
                &&
                (existingHandler != null || numberOfHandlers > 1)) {
                handlerCollection = new HandlerCollection();
                if (existingHandler != null) {
                    handlerCollection.addHandler(existingHandler);
                }
                server.setHandler(handlerCollection);
            }
            
            /*
             * At this point, the server's handler is a collection. It was either
             * one to start, or it is now one containing only the single handler
             * that was there to begin with.
             */
            if (handlers != null && handlers.size() > 0) {
                for (Handler h : handlers) {
                    // Filtering out the jetty default handler 
                    // which should not be added at this point.
                    if (h instanceof DefaultHandler) {
                        defaultHandler = (DefaultHandler) h;
                    } else {
                        if ((h instanceof SecurityHandler) 
                            && ((SecurityHandler)h).getHandler() == null) {
                            //if h is SecurityHandler(such as ConstraintSecurityHandler)
                            //then it need be on top of JettyHTTPHandler
                            //set JettyHTTPHandler as inner handler if 
                            //inner handler is null
                            ((SecurityHandler)h).setHandler(handler);
                            securityHandler = (SecurityHandler)h;
                        } else {
                            handlerCollection.addHandler(h);
                        }
                    }
                }
            }
            /*
             * handlerCollection may be null here if is only one handler to deal with.
             * Which in turn implies that there can't be a 'defaultHander' to deal with.
             */
            if (handlerCollection != null) {
                handlerCollection.addHandler(contexts);
                if (defaultHandler != null) {
                    handlerCollection.addHandler(defaultHandler);
                }
            } else {
                server.setHandler(contexts);
            }

            try {                
                server.start();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "START_UP_SERVER_FAILED_MSG", new Object[] {e.getMessage(), port});
                //problem starting server
                try {                    
                    server.stop();
                    server.destroy();
                } catch (Exception ex) {
                    //ignore - probably wasn't fully started anyway
                }
                server = null;
                throw new Fault(new Message("START_UP_SERVER_FAILED_MSG", LOG, e.getMessage(), port), e);
            }
        }        
        
        String contextName = HttpUriMapper.getContextName(url.getPath());            
        ContextHandler context = new ContextHandler();
        context.setContextPath(contextName);
        // bind the jetty http handler with the context handler
        if (isSessionSupport) {         
            // If we have sessions, we need two handlers.
            if (sessionManager == null) {
                sessionManager = new HashSessionManager();
                HashSessionIdManager idManager = new HashSessionIdManager();
                sessionManager.setSessionIdManager(idManager);
            }
            SessionHandler sessionHandler = new SessionHandler(sessionManager);
            if (securityHandler != null) {
                //use the securityHander which already wrap the jetty http handler
                sessionHandler.setHandler(securityHandler);
            } else {
                sessionHandler.setHandler(handler);
            }
            context.setHandler(sessionHandler);
        } else {
            // otherwise, just the one.
            if (securityHandler != null) {
                //use the securityHander which already wrap the jetty http handler
                context.setHandler(securityHandler);
            } else {
                context.setHandler(handler);
            }
        }
        contexts.addHandler(context);
        
        ServletContext sc = context.getServletContext();
        handler.setServletContext(sc);
       
        final String smap = HttpUriMapper.getResourceBase(url.getPath());
        handler.setName(smap);
        
        if (contexts.isStarted()) {           
            try {                
                context.start();
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "ADD_HANDLER_FAILED_MSG", new Object[] {ex.getMessage()});
            }
        }
         
        registedPaths.add(url.getPath());
        ++servantCount;
    }
    
    private void initializeContexts() {
        if (contexts == null) {
            contexts = new ContextHandlerCollection();
            if (server != null) {
                if (server.getHandler() instanceof ContextHandlerCollection) {
                    contexts = (ContextHandlerCollection) server.getHandler();
                } else {
                    server.setHandler(contexts);
                }
            }
        }
    }

    private void addServerMBean() {
        if (mBeanContainer == null) {
            return;
        }        
        
        try {
            Object o = getContainer(server);
            o.getClass().getMethod("addEventListener", Container.Listener.class).invoke(o, mBeanContainer);
            if (Server.getVersion().startsWith("8")) {
                return;
            }
            mBeanContainer.getClass().getMethod("beanAdded", Container.class, Object.class)
                .invoke(mBeanContainer, null, server);
        } catch (RuntimeException rex) {
            throw rex;
        } catch (Exception r) {
            throw new RuntimeException(r);
        }
    }
    private void removeServerMBean() {
        try {
            mBeanContainer.getClass().getMethod("beanRemoved", Container.class, Object.class)
                .invoke(mBeanContainer, null, server);
        } catch (RuntimeException rex) {
            throw rex;
        } catch (Exception r) {
            throw new RuntimeException(r);
        }
    }

    private Connector createConnector(String hosto, int porto) {
        // now we just use the SelectChannelConnector as the default connector
        SslContextFactory sslcf = null;
        if (tlsServerParameters != null) { 
            sslcf = new SslContextFactory() {
                protected void doStart() throws Exception {
                    setSslContext(createSSLContext(this));
                    super.doStart();
                }
                public void checkKeyStore() {
                    //we'll handle this later
                }
            };
            decorateCXFJettySslSocketConnector(sslcf);
        }
        AbstractConnector result = null;
        if (!Server.getVersion().startsWith("8")) {
            result = createConnectorJetty9(sslcf, hosto, porto);
        } else {
            result = createConnectorJetty8(sslcf, hosto, porto);
        }        
        
        try {
            result.getClass().getMethod("setPort", Integer.TYPE).invoke(result, porto);
            if (hosto != null) {
                result.getClass().getMethod("setHost", String.class).invoke(result, hosto);
            }
            result.getClass().getMethod("setReuseAddress", Boolean.TYPE).invoke(result, isReuseAddress());
        } catch (RuntimeException rex) {
            throw rex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }        
        
        return result;
    }
    
    AbstractConnector createConnectorJetty9(SslContextFactory sslcf, String hosto, int porto) {
        //Jetty 9
        AbstractConnector result = null;
        try {
            Class<?> configClass = ClassLoaderUtils.loadClass("org.eclipse.jetty.server.HttpConfiguration", 
                                                              Server.class); 
            Object httpConfig = configClass.newInstance();
            httpConfig.getClass().getMethod("setSendServerVersion", Boolean.TYPE)
                .invoke(httpConfig, getSendServerVersion());
            
            Object httpFactory = ClassLoaderUtils.loadClass("org.eclipse.jetty.server.HttpConnectionFactory", 
                                                            Server.class)
                                                            .getConstructor(configClass).newInstance(httpConfig); 

            Collection<Object> connectionFactories = new ArrayList<Object>();
            result = (AbstractConnector)ClassLoaderUtils.loadClass("org.eclipse.jetty.server.ServerConnector", 
                                                                   Server.class)
                                                                   .getConstructor(Server.class)
                                                                   .newInstance(server);
            
            if (tlsServerParameters != null) {
                Class<?> src = ClassLoaderUtils.loadClass("org.eclipse.jetty.server.SecureRequestCustomizer",
                                                          Server.class);
                httpConfig.getClass().getMethod("addCustomizer", src.getInterfaces()[0])
                    .invoke(httpConfig, src.newInstance());
                Object scf = ClassLoaderUtils.loadClass("org.eclipse.jetty.server.SslConnectionFactory",
                                                        Server.class).getConstructor(SslContextFactory.class,
                                                                                     String.class)
                                                        .newInstance(sslcf, "HTTP/1.1");
                connectionFactories.add(scf);
                result.getClass().getMethod("setDefaultProtocol", String.class).invoke(result, "SSL-HTTP/1.1");
            }
            connectionFactories.add(httpFactory);
            result.getClass().getMethod("setConnectionFactories", Collection.class)
                .invoke(result, connectionFactories);
            
            if (getMaxIdleTime() > 0) {
                result.getClass().getMethod("setIdleTimeout", Long.TYPE).invoke(result, Long.valueOf(getMaxIdleTime()));
            }

        } catch (RuntimeException rex) {
            throw rex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }
    AbstractConnector createConnectorJetty8(SslContextFactory sslcf, String hosto, int porto) {
        //Jetty 8
        AbstractConnector result = null;
        try {
            if (sslcf == null) { 
                result = (AbstractConnector)ClassLoaderUtils
                    .loadClass("org.eclipse.jetty.server.nio.SelectChannelConnector",
                               Server.class).newInstance();
            } else {
                result = (AbstractConnector)ClassLoaderUtils
                    .loadClass("org.eclipse.jetty.server.ssl.SslSelectChannelConnector",
                               Server.class).getConstructor(SslContextFactory.class)
                               .newInstance(sslcf);
            }
            Server.class.getMethod("setSendServerVersion", Boolean.TYPE).invoke(server, getSendServerVersion());
            if (getMaxIdleTime() > 0) {
                result.getClass().getMethod("setMaxIdleTime", Integer.TYPE).invoke(result, getMaxIdleTime());
            }
        } catch (RuntimeException rex) {
            throw rex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }
    
    
    protected SSLContext createSSLContext(SslContextFactory scf) throws Exception  {
        String proto = tlsServerParameters.getSecureSocketProtocol() == null
            ? "TLS" : tlsServerParameters.getSecureSocketProtocol();
        
        // Jetty 9 excludes SSLv3 by default. So if we want it then we need to 
        // remove it from the default excluded protocols
        if ("SSLv3".equals(proto)) {
            List<String> excludedProtocols = new ArrayList<String>();
            for (String excludedProtocol : scf.getExcludeProtocols()) {
                if (!("SSLv3".equals(excludedProtocol) || "SSLv2Hello".equals(excludedProtocol))) {
                    excludedProtocols.add(excludedProtocol);
                }
            }
            String[] revisedProtocols = new String[excludedProtocols.size()];
            excludedProtocols.toArray(revisedProtocols);
            scf.setExcludeProtocols(revisedProtocols);
        } else if (tlsServerParameters.getExcludeProtocols().isEmpty()) {
            // Exclude SSLv3 + SSLv2Hello by default unless the protocol is given as SSLv3
            scf.addExcludeProtocols("SSLv3");
            scf.addExcludeProtocols("SSLv2Hello");
        }
        
        for (String p : tlsServerParameters.getExcludeProtocols()) {
            scf.addExcludeProtocols(p);
        }
        
        SSLContext context = tlsServerParameters.getJsseProvider() == null
            ? SSLContext.getInstance(proto)
                : SSLContext.getInstance(proto, tlsServerParameters.getJsseProvider());
            
        KeyManager keyManagers[] = tlsServerParameters.getKeyManagers();
        if (tlsServerParameters.getCertAlias() != null) {
            keyManagers = getKeyManagersWithCertAlias(keyManagers);
        }
        context.init(tlsServerParameters.getKeyManagers(), 
                     tlsServerParameters.getTrustManagers(),
                     tlsServerParameters.getSecureRandom());

        // Set the CipherSuites
        final String[] supportedCipherSuites = 
            SSLUtils.getServerSupportedCipherSuites(context);

        if (tlsServerParameters.getCipherSuitesFilter() != null
            && tlsServerParameters.getCipherSuitesFilter().isSetExclude()) {
            String[] excludedCipherSuites = 
                SSLUtils.getFilteredCiphersuites(tlsServerParameters.getCipherSuitesFilter(),
                                                 supportedCipherSuites,
                                                 LOG, 
                                                 true);
            scf.setExcludeCipherSuites(excludedCipherSuites);
        }
        
        String[] includedCipherSuites = 
            SSLUtils.getCiphersuitesToInclude(tlsServerParameters.getCipherSuites(), 
                                              tlsServerParameters.getCipherSuitesFilter(), 
                                              context.getServerSocketFactory().getDefaultCipherSuites(),
                                              supportedCipherSuites, 
                                              LOG);
        scf.setIncludeCipherSuites(includedCipherSuites);
        
        return context;
    }
    protected KeyManager[] getKeyManagersWithCertAlias(KeyManager keyManagers[]) throws Exception {
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
    protected void setClientAuthentication(SslContextFactory con,
                                           ClientAuthentication clientAuth) {
        con.setWantClientAuth(true);
        if (clientAuth != null) {
            if (clientAuth.isSetWant()) {
                con.setWantClientAuth(clientAuth.isWant());
            }
            if (clientAuth.isSetRequired()) {
                con.setNeedClientAuth(clientAuth.isRequired());
            }
        }
    }    
    /**
     * This method sets the security properties for the CXF extension
     * of the JettySslConnector.
     */
    private void decorateCXFJettySslSocketConnector(
            SslContextFactory con
    ) {
        setClientAuthentication(con,
                                tlsServerParameters.getClientAuthentication());
        con.setCertAlias(tlsServerParameters.getCertAlias());
    }
    

    private static Container getContainer(Object server) {
        if (server instanceof Container) {
            return (Container)server;
        }
        try {
            return (Container)server.getClass().getMethod("getContainer").invoke(server);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static void logConnector(Connector connector) {
        try {
            String h = (String)connector.getClass().getMethod("getHost").invoke(connector);
            int port = (Integer)connector.getClass().getMethod("getPort").invoke(connector);
            LOG.finer("connector.host: " 
                + h == null 
                  ? "null" 
                  : "\"" + h + "\"");
            LOG.finer("connector.port: " + port);
        } catch (Throwable t) {
            //ignore
        }
    }

    protected void setupThreadPool() {
        if (isSetThreadingParameters()) {
            
            ThreadPool pl = getThreadPool();
            //threads for the acceptors and selectors are taken from 
            //the pool so we need to have room for those
            AbstractConnector aconn = (AbstractConnector) connector;
            int acc = aconn.getAcceptors() * 2;
            if (getThreadingParameters().isSetMaxThreads()
                && getThreadingParameters().getMaxThreads() <= acc) {
                throw new Fault(new Message("NOT_ENOUGH_THREADS", LOG,
                                            port,
                                            acc + 1,
                                            getThreadingParameters().getMaxThreads(),
                                            acc));
            }
            if (!(pl instanceof QueuedThreadPool)) {
                throw new Fault(new Message("NOT_A_QUEUED_THREAD_POOL", LOG, pl.getClass()));
            }
            if (getThreadingParameters().isThreadNamePrefixSet()) {
                ((QueuedThreadPool) pl).setName(getThreadingParameters().getThreadNamePrefix());
            }
            if (getThreadingParameters().isSetMinThreads()) {
                ((QueuedThreadPool) pl).setMinThreads(getThreadingParameters().getMinThreads());
            }
            if (getThreadingParameters().isSetMaxThreads()) {
                ((QueuedThreadPool) pl).setMaxThreads(getThreadingParameters().getMaxThreads());
            }
        }
    }
    
    private ThreadPool getThreadPool() {
        ThreadPool pool = (ThreadPool)server.getThreadPool();
        if (pool == null) {
            pool = new QueuedThreadPool();
            try {
                server.getClass().getMethod("setThreadPool", ThreadPool.class).invoke(server, pool);
            } catch (RuntimeException t) {
                throw t;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return pool;
    }
    
    /**
     * Remove a previously registered servant.
     * 
     * @param url the URL the servant was registered against.
     */
    public synchronized void removeServant(URL url) {        
        
        final String contextName = HttpUriMapper.getContextName(url.getPath());
        final String smap = HttpUriMapper.getResourceBase(url.getPath());
        
        boolean found = false;
        
        if (server != null && server.isRunning()) {
            for (Handler handler : contexts.getChildHandlersByClass(ContextHandler.class)) {
                ContextHandler contextHandler = null;                
                if (handler instanceof ContextHandler) {
                    contextHandler = (ContextHandler) handler;
                    Handler jh = contextHandler.getHandler();
                    if (jh instanceof JettyHTTPHandler
                        && (contextName.equals(contextHandler.getContextPath())
                            || (StringUtils.isEmpty(contextName) 
                                && "/".equals(contextHandler.getContextPath())))
                        && ((JettyHTTPHandler)jh).getName().equals(smap)) {
                        try {
                            contexts.removeHandler(handler);                            
                            handler.stop();
                            handler.destroy();
                        } catch (Exception ex) {
                            LOG.log(Level.WARNING, "REMOVE_HANDLER_FAILED_MSG", 
                                    new Object[] {ex.getMessage()}); 
                        }
                        found = true;
                        break;                        
                    }                    
                }
            }
        }
        if (!found) {
            LOG.log(Level.WARNING, "CAN_NOT_FIND_HANDLER_MSG", new Object[]{url});
        }
        registedPaths.remove(url.getPath());
        --servantCount;
        
       
    }

    /**
     * Get a registered servant.
     * 
     * @param url the associated URL
     * @return the HttpHandler if registered
     */
    public synchronized Handler getServant(URL url)  {
        String contextName = HttpUriMapper.getContextName(url.getPath());       
        //final String smap = HttpUriMapper.getResourceBase(url.getPath());
        
        Handler ret = null;
        // After a stop(), the server is null, and therefore this 
        // operation should return null.
        if (server != null) {           
            for (Handler handler : server.getChildHandlersByClass(ContextHandler.class)) {
                ContextHandler contextHandler = null;
                if (handler instanceof ContextHandler) {
                    contextHandler = (ContextHandler) handler;
                    if (contextName.equals(contextHandler.getContextPath())) {           
                        ret = contextHandler.getHandler();
                        break;
                    }
                }
            }    
        }
        return ret;
    }
    
    /**
     * Get a registered context handler.
     * 
     * @param url the associated URL
     * @return the HttpHandler if registered
     */
    public synchronized ContextHandler getContextHandler(URL url) {
        String contextName = HttpUriMapper.getContextName(url.getPath());
        ContextHandler ret = null;
        // After a stop(), the server is null, and therefore this 
        // operation should return null.
        if (server != null) {           
            for (Handler handler : server.getChildHandlersByClass(ContextHandler.class)) {
                ContextHandler contextHandler = null;
                if (handler instanceof ContextHandler) {
                    contextHandler = (ContextHandler) handler;
                    if (contextName.equals(contextHandler.getContextPath())) {           
                        ret = contextHandler;
                        break;
                    }
                }
            }    
        }
        return ret;
    }
    
    private boolean isSsl() {
        if (connector == null) {
            return false;
        }
                        
        try {
            //Jetty 8
            return ClassLoaderUtils.loadClass("org.eclipse.jetty.server.ssl.SslConnector",
                                              Server.class).isInstance(connector);
        } catch (ClassNotFoundException e) {
            //Jetty 9
            //return "https".equalsIgnoreCase(connector.getDefaultConnectionFactory().getProtocol());
            try {
                Object o = connector.getClass().getMethod("getDefaultConnectionFactory").invoke(connector);
                return "https".equalsIgnoreCase((String)o.getClass().getMethod("getProtocol").invoke(o));
            } catch (Exception ex) {
                //ignore
            }
        }
        return false;
    }

    protected void retrieveListenerFactory() {
        if (tlsServerParameters != null) {
            if (connector != null && !isSsl()) {
                LOG.warning("Connector " + connector + " for JettyServerEngine Port " 
                        + port + " does not support SSL connections.");
                return;
            }
            protocol = "https";
            
        } else {
            if (isSsl()) {
                throw new RuntimeException("Connector " + connector + " for JettyServerEngine Port " 
                      + port + " does not support non-SSL connections.");
            }
            protocol = "http";
        }
        LOG.fine("Configured port " + port + " for \"" + protocol + "\".");
    }
    
    /**
     * This method is called after configure on this object.
     */
    @PostConstruct
    public void finalizeConfig() 
        throws GeneralSecurityException,
               IOException {
        retrieveListenerFactory();
        checkConnectorPort();
        this.configFinalized = true;
    }
    
    private void checkConnectorPort() throws IOException {
        try {
            int cp = (Integer)connector.getClass().getMethod("getPort").invoke(connector);
            if (null != connector && port != cp) {
                throw new IOException("Error: Connector port " + cp + " does not match"
                            + " with the server engine port " + port);
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable t) {
            //ignore...
        }
    }
    

    
    /**
     * This method is called by the ServerEngine Factory to destroy the 
     * listener.
     *
     */
    protected void stop() throws Exception {
        registedPaths.clear();
        if (server != null) {
            try {
                if (connector != null) {
                    connector.stop();
                    if (connector instanceof Closeable) {
                        ((Closeable)connector).close();
                    } else {
                        connector.getClass().getMethod("close").invoke(connector);
                    }
                }
            } finally {  
                if (contexts != null) {
                    for (Handler h : contexts.getHandlers()) {
                        h.stop();
                    }
                    contexts.stop();
                }
                contexts = null;
                server.stop();
                if (mBeanContainer != null
                    && !Server.getVersion().startsWith("8")) {
                    removeServerMBean();
                }
                server.destroy();
                server = null;
            }
        }
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
    
    /**
     * This method returns the programmatically set TLSServerParameters, not
     * the TLSServerParametersType, which is the JAXB generated type used 
     * in SpringConfiguration.
     * @return
     */
    public TLSServerParameters getTlsServerParameters() {
        return tlsServerParameters;
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

    public void setSendServerVersion(Boolean sendServerVersion) {
        this.sendServerVersion = sendServerVersion;
    }

    public Boolean getSendServerVersion() {
        return sendServerVersion;
    }
    
}
