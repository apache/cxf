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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.continuations.ContinuationInfo;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.HTTPSession;
import org.apache.cxf.transport.http_jetty.continuations.JettyContinuationProvider;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.transports.http.StemMatchingQueryHandler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

public class JettyHTTPDestination extends AbstractHTTPDestination {
    
    private static final Logger LOG =
        LogUtils.getL7dLogger(JettyHTTPDestination.class);

    protected JettyHTTPServerEngine engine;
    protected JettyHTTPTransportFactory transportFactory;
    protected JettyHTTPServerEngineFactory serverEngineFactory;
    protected ServletContext servletContext;
    protected URL nurl;
    
    /**
     * This variable signifies that finalizeConfig() has been called.
     * It gets called after this object has been spring configured.
     * It is used to automatically reinitialize things when resources
     * are reset, such as setTlsServerParameters().
     */
    private boolean configFinalized;
     
    /**
     * Constructor, using Jetty server engine.
     * 
     * @param b the associated Bus
     * @param ci the associated conduit initiator
     * @param endpointInfo the endpoint info of the destination
     * @throws IOException
     */
    public JettyHTTPDestination(
            Bus                       b,
            JettyHTTPTransportFactory ci, 
            EndpointInfo              endpointInfo
    ) throws IOException {
        
        //Add the defualt port if the address is missing it
        super(b, ci, endpointInfo, true);
        this.transportFactory = ci;
        this.serverEngineFactory = ci.getJettyHTTPServerEngineFactory();
        nurl = new URL(endpointInfo.getAddress());
    }

    protected Logger getLogger() {
        return LOG;
    }
    
    public void setServletContext(ServletContext sc) {
        servletContext = sc;
    }
    
    /**
     * Post-configure retreival of server engine.
     */
    protected void retrieveEngine()
        throws GeneralSecurityException, 
               IOException {
        
        engine = 
            serverEngineFactory.retrieveJettyHTTPServerEngine(nurl.getPort());
        if (engine == null) {
            engine = serverEngineFactory.
                createJettyHTTPServerEngine(nurl.getPort(), nurl.getProtocol());
        }
        
        assert engine != null;
        
        // When configuring for "http", however, it is still possible that
        // Spring configuration has configured the port for https. 
        if (!nurl.getProtocol().equals(engine.getProtocol())) {
            throw new IllegalStateException(
                "Port " + engine.getPort() 
                + " is configured with wrong protocol \"" 
                + engine.getProtocol()
                + "\" for \"" + nurl + "\"");
        }
    }
    
    /**
     * This method is used to finalize the configuration
     * after the configuration items have been set.
     *
     */
    public void finalizeConfig() 
        throws GeneralSecurityException,
               IOException {
        
        assert !configFinalized;
        
        retrieveEngine();
        configFinalized = true;
    }
    
    /**
     * Activate receipt of incoming messages.
     */
    protected void activate() {
        LOG.log(Level.FINE, "Activating receipt of incoming messages");
        URL url = null;
        try {
            url = new URL(endpointInfo.getAddress());
        } catch (Exception e) {
            throw new Fault(new Message("START_UP_SERVER_FAILED_MSG", LOG, e.getMessage()), e);
        }
        engine.addServant(url, 
                          new JettyHTTPHandler(this, contextMatchOnExact()));
    }

    /**
     * Deactivate receipt of incoming messages.
     */
    protected void deactivate() {
        LOG.log(Level.FINE, "Deactivating receipt of incoming messages");
        engine.removeServant(nurl);   
    }   
     

    
    protected String getBasePathForFullAddress(String addr) {
        try {
            return new URL(addr).getPath();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private String removeTrailingSeparator(String addr) {
        if (addr != null && addr.length() > 0 
            && addr.lastIndexOf('/') == addr.length() - 1) {
            return addr.substring(0, addr.length() - 1);
        } else {
            return addr;
        }
    }
    
    private synchronized String updateEndpointAddress(String addr) {
        // only update the EndpointAddress if the base path is equal
        // make sure we don't broke the get operation?parament query 
        String address = removeTrailingSeparator(endpointInfo.getAddress());
        if (getBasePathForFullAddress(address)
            .equals(removeTrailingSeparator(getStem(getBasePathForFullAddress(addr))))) {
            endpointInfo.setAddress(addr);
        }
        return address;
    }
   
    protected void doService(HttpServletRequest req,
                             HttpServletResponse resp) throws IOException {
        doService(servletContext, req, resp);
    }
    protected void doService(ServletContext context,
                             HttpServletRequest req,
                             HttpServletResponse resp) throws IOException {
        if (context == null) {
            context = servletContext;
        }
        Request baseRequest = (req instanceof Request) 
            ? (Request)req : HttpConnection.getCurrentConnection().getRequest();
            
        if (getServer().isSetRedirectURL()) {
            resp.sendRedirect(getServer().getRedirectURL());
            resp.flushBuffer();
            baseRequest.setHandled(true);
            return;
        }
        QueryHandlerRegistry queryHandlerRegistry = bus.getExtension(QueryHandlerRegistry.class);
        
        if (null != req.getQueryString() && queryHandlerRegistry != null) {   
            String reqAddr = req.getRequestURL().toString();
            String requestURL =  reqAddr + "?" + req.getQueryString();
            String pathInfo = req.getPathInfo();                     
            for (QueryHandler qh : queryHandlerRegistry.getHandlers()) {
                boolean recognized =
                    qh instanceof StemMatchingQueryHandler
                    ? ((StemMatchingQueryHandler)qh).isRecognizedQuery(requestURL,
                                                                       pathInfo,
                                                                       endpointInfo,
                                                                       contextMatchOnExact())
                    : qh.isRecognizedQuery(requestURL, pathInfo, endpointInfo);
                if (recognized) {
                    //replace the endpointInfo address with request url only for get wsdl
                    String errorMsg = null;
                    CachedOutputStream out = new CachedOutputStream();
                    try {
                        synchronized (endpointInfo) {
                            String oldAddress = updateEndpointAddress(reqAddr);   
                            resp.setContentType(qh.getResponseContentType(requestURL, pathInfo));
                            try {
                                qh.writeResponse(requestURL, pathInfo, endpointInfo, out);
                            } catch (Exception ex) {
                                LOG.log(Level.WARNING, "writeResponse failed: ", ex);
                                errorMsg = ex.getMessage();
                            }
                            endpointInfo.setAddress(oldAddress);
                        }
                        if (errorMsg != null) {
                            resp.sendError(500, errorMsg);
                        } else {
                            out.writeCacheTo(resp.getOutputStream());
                            resp.getOutputStream().flush();                     
                        }
                    } finally {
                        out.close();
                    }
                    baseRequest.setHandled(true);
                    return;
                }
            }
        }

        // REVISIT: service on executor if associated with endpoint
        try {
            BusFactory.setThreadDefaultBus(bus); 
            serviceRequest(context, req, resp);
        } finally {
            BusFactory.setThreadDefaultBus(null);  
        }    
    }

    protected void serviceRequest(final ServletContext context, 
                                  final HttpServletRequest req, 
                                  final HttpServletResponse resp)
        throws IOException {
        Request baseRequest = (req instanceof Request) 
            ? (Request)req : HttpConnection.getCurrentConnection().getRequest();
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Service http request on thread: " + Thread.currentThread());
        }
        MessageImpl inMessage = retrieveFromContinuation(req);
        
        
        if (inMessage == null) {
            
            inMessage = new MessageImpl();
            if (engine.getContinuationsEnabled()) {
                inMessage.put(ContinuationProvider.class.getName(), 
                          new JettyContinuationProvider(req, inMessage));
            }
            
            setupMessage(inMessage, context, req, resp);
            
            inMessage.setDestination(this);
    
            ExchangeImpl exchange = new ExchangeImpl();
            exchange.setInMessage(inMessage);
            exchange.setSession(new HTTPSession(req));
        }

        try {    
            incomingObserver.onMessage(inMessage);
            resp.flushBuffer();
            baseRequest.setHandled(true);
        } catch (SuspendedInvocationException ex) {
            throw ex.getRuntimeException();
        } catch (Fault ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            } else {
                throw ex;
            }
        } catch (RuntimeException ex) {
            throw ex;
        } finally {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Finished servicing http request on thread: " + Thread.currentThread());
            }
        }
    }

    protected MessageImpl retrieveFromContinuation(HttpServletRequest req) {
        MessageImpl m = null;
        
        if (!engine.getContinuationsEnabled()) {
            return null;
        }
        
        Continuation cont = ContinuationSupport.getContinuation(req, null);
        synchronized (cont) {
            Object o = cont.getObject();
            if (o instanceof ContinuationInfo) {
                ContinuationInfo ci = (ContinuationInfo)o;
                m = (MessageImpl)ci.getMessage();
                
                // now that we got the message we don't need ContinuationInfo
                // as we don't know how continuation was suspended, by jetty wrapper
                // or directly in which (latter) case we need to ensure that an original user object
                // if any, need to be restored
                cont.setObject(ci.getUserObject());
            }
            if (m == null && !cont.isNew()) {
                String message = "No message for existing continuation, status : "
                    + (cont.isPending() ? "Pending" : "Resumed");
                if (!(o instanceof ContinuationInfo)) {
                    message += ", ContinuationInfo object is unavailable";
                }
                LOG.warning(message);
            }
        }
        
        return m;
    }
    
    @Override
    public void shutdown() {
        transportFactory.removeDestination(endpointInfo);
        
        super.shutdown();
    }
    
    public ServerEngine getEngine() {
        return engine;
    }
   
    private String getStem(String baseURI) {    
        return baseURI.substring(0, baseURI.lastIndexOf("/"));
    }
}
