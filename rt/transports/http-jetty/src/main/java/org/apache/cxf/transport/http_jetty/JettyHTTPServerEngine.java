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
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.transport.HttpUriMapper;
import org.apache.cxf.transport.https_jetty.JettySslConnectorFactory;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.server.ssl.SslConnector;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;


/**
 * This class is the Jetty HTTP Server Engine that is configured to
 * work off of a designated port. The port will be enabled for 
 * "http" or "https" depending upon its successful configuration.
 */
public class JettyHTTPServerEngine
    implements ServerEngine {
    
    private static final Logger LOG =
        LogUtils.getL7dLogger(JettyHTTPServerEngine.class);
   
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
    private JettyConnectorFactory connectorFactory;
    private ContextHandlerCollection contexts;
    private Container.Listener mBeanContainer;
    private SessionManager sessionManager;
    
    
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
    
    private boolean shouldCheckUrl() {
        String s = SystemPropertyAction
            .getPropertyOrNull("org.apache.cxf.transports.http_jetty.DontCheckUrl");
        return !Boolean.valueOf(s);
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
        if (shouldCheckUrl()) {
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
    }
    
    
    /**
     * Register a servant.
     * 
     * @param url the URL associated with the servant
     * @param handler notified on incoming HTTP requests
     */
    public synchronized void addServant(URL url, JettyHTTPHandler handler) {
        
        checkRegistedContext(url);
        
        SecurityHandler securityHandler = null;
        if (server == null) {
            DefaultHandler defaultHandler = null;
            // create a new jetty server instance if there is no server there            
            server = new Server();
            
            server.setSendServerVersion(getSendServerVersion());
            
            if (mBeanContainer != null) {
                server.getContainer().addEventListener(mBeanContainer);
            }
            
            if (connector == null) {
                connector = connectorFactory.createConnector(getHost(), getPort());
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("connector.host: " 
                              + connector.getHost() == null 
                                ? "null" 
                                : "\"" + connector.getHost() + "\"");
                    LOG.finer("connector.port: " + connector.getPort());
                }
            } 

            server.addConnector(connector);
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
            contexts = new ContextHandlerCollection();
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
                setReuseAddress(connector);
                setupThreadPool();
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
    
    protected void setupThreadPool() {
        AbstractConnector aconn = (AbstractConnector) connector;
        if (isSetThreadingParameters()) {
            ThreadPool pool = aconn.getThreadPool();
            if (pool == null) {
                pool = aconn.getServer().getThreadPool();
            }
            if (pool == null) {
                pool = new QueuedThreadPool();
                aconn.getServer().setThreadPool(pool);
                aconn.setThreadPool(pool);
            }
            //threads for the acceptors and selectors are taken from 
            //the pool so we need to have room for those
            int acc = aconn.getAcceptors() * 2;
            if (getThreadingParameters().isSetMaxThreads()
                && getThreadingParameters().getMaxThreads() <= acc) {
                throw new Fault(new Message("NOT_ENOUGH_THREADS", LOG,
                                            port,
                                            acc + 1,
                                            getThreadingParameters().getMaxThreads(),
                                            acc));
            }
            if (pool instanceof QueuedThreadPool) {
                QueuedThreadPool pl = (QueuedThreadPool)pool;
                if (getThreadingParameters().isSetMinThreads()) {
                    pl.setMinThreads(getThreadingParameters().getMinThreads());
                }
                if (getThreadingParameters().isSetMaxThreads()) {
                    pl.setMaxThreads(getThreadingParameters().getMaxThreads());
                }
            } else {
                try {
                    if (getThreadingParameters().isSetMinThreads()) {
                        pool.getClass().getMethod("setMinThreads", Integer.TYPE)
                            .invoke(pool, getThreadingParameters().getMinThreads());
                    }
                    if (getThreadingParameters().isSetMaxThreads()) {
                        pool.getClass().getMethod("setMaxThreads", Integer.TYPE)
                            .invoke(pool, getThreadingParameters().getMaxThreads());
                    }
                } catch (Throwable t) {
                    //ignore - this won't happen for Jetty 7.1 - 7.2 and 7.3 and newer 
                    //will be instanceof QueuedThreadPool above
                }
            }
        }
    }
    
    private void setReuseAddress(Connector conn) throws IOException {
        if (conn instanceof AbstractConnector) {
            ((AbstractConnector)conn).setReuseAddress(isReuseAddress());
        } else {
            LOG.log(Level.INFO, "UNKNOWN_CONNECTOR_MSG", new Object[] {conn});
        }
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
                        && contextName.equals(contextHandler.getContextPath())
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

    protected void retrieveListenerFactory() {
        if (tlsServerParameters != null) {
            if (null != connector && !(connector instanceof SslConnector)) {
                LOG.warning("Connector " + connector + " for JettyServerEngine Port " 
                        + port + " does not support SSL connections.");
                return;
            }
            connectorFactory = 
                getHTTPSConnectorFactory(tlsServerParameters);            
            protocol = "https";
            
        } else {
            if (connector instanceof SslConnector) {
                throw new RuntimeException("Connector " + connector + " for JettyServerEngine Port " 
                      + port + " does not support non-SSL connections.");
            }
            connectorFactory = getHTTPConnectorFactory();            
            protocol = "http";
        }
        LOG.fine("Configured port " + port + " for \"" + protocol + "\".");
    }

    /**
     * This method creates a connector factory. If there are TLS parameters
     * then it creates a TLS enabled one.
     */
    protected JettyConnectorFactory getHTTPConnectorFactory() {
        return new JettyConnectorFactory() {
            public AbstractConnector createConnector(int porto) {
                return createConnector(null, porto);
            }
            public AbstractConnector createConnector(String hosto, int porto) {
                // now we just use the SelectChannelConnector as the default connector
                SelectChannelConnector result = 
                    new SelectChannelConnector();
                
                // Regardless the port has to equal the one
                // we are configured for.
                assert porto == port;
                assert hosto == null ? host == null : hosto.equals(host);
                if (hosto != null) {
                    result.setHost(hosto);
                }
                result.setPort(porto);
                if (getMaxIdleTime() > 0) {
                    result.setMaxIdleTime(getMaxIdleTime());
                }
                return result;
            }
        };
    }
    
    /**
     * This method creates a connector factory enabled with the JSSE
     */
    protected JettyConnectorFactory getHTTPSConnectorFactory(
            TLSServerParameters tlsParams
    ) {
        return new JettySslConnectorFactory(tlsParams, getMaxIdleTime());
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
        if (null != connector && port != connector.getPort()) {
            throw new IOException("Error: Connector port " + connector.getPort() + " does not match"
                        + " with the server engine port " + port);
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
                connector.stop();
                connector.close();            
            } finally {         
                server.stop();
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
