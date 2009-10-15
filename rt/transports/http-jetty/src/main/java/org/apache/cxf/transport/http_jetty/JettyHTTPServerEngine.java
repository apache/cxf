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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletContext;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.transport.HttpUriMapper;
import org.apache.cxf.transport.https_jetty.JettySslConnectorFactory;
import org.mortbay.jetty.AbstractConnector;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.HashSessionIdManager;
import org.mortbay.jetty.servlet.HashSessionManager;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.thread.QueuedThreadPool;


/**
 * This class is the Jetty HTTP Server Engine that is configured to
 * work off of a designated port. The port will be enabled for 
 * "http" or "https" depending upon its successful configuration.
 */
public class JettyHTTPServerEngine
    implements ServerEngine {
    private static final long serialVersionUID = 1L;
    
    private static final Logger LOG =
        LogUtils.getL7dLogger(JettyHTTPServerEngine.class);
   

    /**
     * The bus.
     */
    protected Bus bus;
    
    /**
     * This is the Jetty HTTP Server Engine Factory. This factory caches some 
     * engines based on port numbers.
     */
    protected JettyHTTPServerEngineFactory factory;
    
    
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
    private int servantCount;
    private Server server;
    private Connector connector;
    private List<Handler> handlers;
    private JettyConnectorFactory connectorFactory;
    private ContextHandlerCollection contexts;
    
    
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
        
    /**
     * This constructor is called by the JettyHTTPServerEngineFactory.
     */
    public JettyHTTPServerEngine(
        JettyHTTPServerEngineFactory fac, 
        Bus bus,
        String host,
        int port) {
        this.bus     = bus;
        this.factory = fac;
        this.host    = host;
        this.port    = port;
    }
    
    public JettyHTTPServerEngine() {
        
    }
     
    public void setJettyHTTPServerEngineFactory(JettyHTTPServerEngineFactory fac) {
        factory = fac;
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
     * The bus.
     */
    @Resource(name = "cxf")
    public void setBus(Bus b) {
        bus = b;
    }
    
    public Bus getBus() {
        return bus;
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
        if (shouldDestroyPort()) {
            if (factory != null && servantCount == 0) {
                factory.destroyForPort(port);
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
        
        String s = System.getProperty("org.apache.cxf.transports.http_jetty.DontClosePort");
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
    
    /**
     * Register a servant.
     * 
     * @param url the URL associated with the servant
     * @param handler notified on incoming HTTP requests
     */
    @SuppressWarnings("deprecation")
    public synchronized void addServant(URL url, JettyHTTPHandler handler) {
        if (server == null) {
            DefaultHandler defaultHandler = null;
            // create a new jetty server instance if there is no server there            
            server = new Server();
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
            if (handlers != null && handlers.size() > 0) {
                HandlerList handlerList = new HandlerList();
                for (Handler h : handlers) {
                    // filting the jetty default handler 
                    // which should not be added at this point
                    if (h instanceof DefaultHandler) {
                        defaultHandler = (DefaultHandler) h;
                    } else {
                        handlerList.addHandler(h);
                    }
                }
                server.addHandler(handlerList);
            }
            contexts = new ContextHandlerCollection();
            server.addHandler(contexts);
            if (defaultHandler != null) {
                server.addHandler(defaultHandler);
            }
            try {                
                setReuseAddress(connector);
                server.start();
               
                AbstractConnector aconn = (AbstractConnector) connector;
                if (isSetThreadingParameters()) {
                    if (aconn.getThreadPool() instanceof org.mortbay.thread.BoundedThreadPool) {
                        org.mortbay.thread.BoundedThreadPool pool 
                            = (org.mortbay.thread.BoundedThreadPool)aconn.getThreadPool();
                        if (getThreadingParameters().isSetMinThreads()) {
                            pool.setMinThreads(getThreadingParameters().getMinThreads());
                        }
                        if (getThreadingParameters().isSetMaxThreads()) {
                            pool.setMaxThreads(getThreadingParameters().getMaxThreads());
                        }
                    } else if (aconn.getThreadPool() instanceof QueuedThreadPool) {
                        QueuedThreadPool pool = (QueuedThreadPool)aconn.getThreadPool();
                        if (getThreadingParameters().isSetMinThreads()) {
                            pool.setMinThreads(getThreadingParameters().getMinThreads());
                        }
                        if (getThreadingParameters().isSetMaxThreads()) {
                            pool.setMaxThreads(getThreadingParameters().getMaxThreads());
                        }
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "START_UP_SERVER_FAILED_MSG", new Object[] {e.getMessage()});
                //problem starting server
                try {                    
                    server.stop();
                    server.destroy();
                } catch (Exception ex) {
                    //ignore - probably wasn't fully started anyway
                }
                server = null;
                throw new Fault(new Message("START_UP_SERVER_FAILED_MSG", LOG, e.getMessage()), e);
            }
        }        
        
        String contextName = HttpUriMapper.getContextName(url.getPath());            
        ContextHandler context = new ContextHandler();
        context.setContextPath(contextName);
        
        // bind the jetty http handler with the context handler        
        context.setHandler(handler);
        if (isSessionSupport) {            
            HashSessionManager sessionManager = new HashSessionManager();
            SessionHandler sessionHandler = new SessionHandler(sessionManager);
            HashSessionIdManager idManager = new HashSessionIdManager();
            sessionManager.setIdManager(idManager);            
            context.addHandler(sessionHandler);           
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
        
            
        ++servantCount;
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
            if (null != connector && !(connector instanceof SslSocketConnector)) {
                LOG.warning("Connector " + connector + " for JettyServerEngine Port " 
                        + port + " does not support SSL connections.");
                return;
            }
            connectorFactory = 
                getHTTPSConnectorFactory(tlsServerParameters);            
            protocol = "https";
            
        } else {
            if (connector instanceof SslSocketConnector) {
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
                assert hosto == host;
                if (hosto != null) {
                    result.setHost(hosto);
                }
                result.setPort(porto);
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
        return new JettySslConnectorFactory(tlsParams);
    }
    
    /**
     * This method is called after configure on this object.
     */
    @PostConstruct
    public void finalizeConfig() 
        throws GeneralSecurityException,
               IOException {
        retrieveEngineFactory();
        retrieveListenerFactory();
        checkConnectorPort();
        this.configFinalized = true;
    }
    
    protected void retrieveEngineFactory() {
        if (null != bus && null == factory) {
            factory = bus.getExtension(JettyHTTPServerEngineFactory.class);
        }        
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
    public void setTlsServerParameters(TLSServerParameters params) throws IOException {
        
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
    
}
