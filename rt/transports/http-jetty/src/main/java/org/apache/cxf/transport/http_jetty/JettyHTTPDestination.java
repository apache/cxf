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

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.security.CertificateConstraintsType;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CopyingOutputStream;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPSession;
import org.apache.cxf.transport.http_jetty.continuations.JettyContinuationProvider;
import org.apache.cxf.transport.https.CertConstraintsJaxBUtils;
import org.eclipse.jetty.http.Generator;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.server.AbstractHttpConnection.Output;
import org.eclipse.jetty.server.Request;
import org.springframework.util.ClassUtils;

public class JettyHTTPDestination extends AbstractHTTPDestination {
    
    private static final Logger LOG =
        LogUtils.getL7dLogger(JettyHTTPDestination.class);

    protected JettyHTTPServerEngine engine;
    protected JettyHTTPServerEngineFactory serverEngineFactory;
    protected ServletContext servletContext;
    protected URL nurl;
    protected ClassLoader loader;
    
    /**
     * This variable signifies that finalizeConfig() has been called.
     * It gets called after this object has been spring configured.
     * It is used to automatically reinitialize things when resources
     * are reset, such as setTlsServerParameters().
     */
    private boolean configFinalized;
     
    /**
     * Constructor
     *
     * @param b  the associated Bus
     * @param registry the associated destinationRegistry
     * @param ei the endpoint info of the destination
     * @param serverEngineFactory the serverEngineFactory which could be used to create ServerEngine
     * @throws java.io.IOException
     */
    public JettyHTTPDestination(
            Bus bus,
            DestinationRegistry registry, 
            EndpointInfo ei, 
            JettyHTTPServerEngineFactory serverEngineFactory
    ) throws IOException {
        //Add the default port if the address is missing it
        super(bus, registry, ei, getAddressValue(ei, true).getAddress(), true);
        this.serverEngineFactory = serverEngineFactory;
        nurl = new URL(endpointInfo.getAddress());
        loader = bus.getExtension(ClassLoader.class);
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
                createJettyHTTPServerEngine(nurl.getHost(), nurl.getPort(), nurl.getProtocol());
        }

        assert engine != null;
        TLSServerParameters serverParameters = engine.getTlsServerParameters();
        if (serverParameters != null && serverParameters.getCertConstraints() != null) {
            CertificateConstraintsType constraints = serverParameters.getCertConstraints();
            if (constraints != null) {
                certConstraints = CertConstraintsJaxBUtils.createCertConstraints(constraints);
            }
        }
        
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
    public void finalizeConfig() {
        assert !configFinalized;
        
        try {
            retrieveEngine();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        configFinalized = true;
    }
    
    /**
     * Activate receipt of incoming messages.
     */
    protected void activate() {
        super.activate();
        LOG.log(Level.FINE, "Activating receipt of incoming messages");
        URL url = null;
        try {
            url = new URL(endpointInfo.getAddress());
        } catch (Exception e) {
            throw new Fault(e);
        }
        engine.addServant(url, 
                          new JettyHTTPHandler(this, contextMatchOnExact()));
    }

    /**
     * Deactivate receipt of incoming messages.
     */
    protected void deactivate() {
        super.deactivate();
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
       
    protected void doService(HttpServletRequest req,
                             HttpServletResponse resp) throws IOException {
        doService(servletContext, req, resp);
    }
    
    static AbstractConnection getConnectionForRequest(Request r) {
        try {
            return (AbstractConnection)r.getClass().getMethod("getConnection").invoke(r);
        } catch (Exception ex) {
            return null;
        }
    }
    
    private void setHeadFalse(AbstractConnection con) {
        try {
            Generator gen = (Generator)con.getClass().getMethod("getGenerator").invoke(con);
            gen.setHead(false);
        } catch (Exception ex) {
            //ignore - can continue
        }
    }
    
    protected void doService(ServletContext context,
                             HttpServletRequest req,
                             HttpServletResponse resp) throws IOException {
        if (context == null) {
            context = servletContext;
        }
        Request baseRequest = (req instanceof Request) 
            ? (Request)req : getCurrentRequest();
            
        if (!"HEAD".equals(req.getMethod())) {
            //bug in Jetty with persistent connections that if a HEAD is
            //sent, a _head flag is never reset
            AbstractConnection c = getConnectionForRequest(baseRequest);
            if (c != null) {
                setHeadFalse(c);
            }
        }
        if (getServer().isSetRedirectURL()) {
            resp.sendRedirect(getServer().getRedirectURL());
            resp.flushBuffer();
            baseRequest.setHandled(true);
            return;
        }

        // REVISIT: service on executor if associated with endpoint
        ClassLoaderHolder origLoader = null;
        Bus origBus = BusFactory.getAndSetThreadDefaultBus(bus);
        try {
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            serviceRequest(context, req, resp);
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
            if (origLoader != null) { 
                origLoader.reset();
            }
        }    
    }

    protected void serviceRequest(final ServletContext context, 
                                  final HttpServletRequest req, 
                                  final HttpServletResponse resp)
        throws IOException {
        Request baseRequest = (req instanceof Request) 
            ? (Request)req : getCurrentRequest();
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Service http request on thread: " + Thread.currentThread());
        }
        Message inMessage = retrieveFromContinuation(req);
        
        if (inMessage == null) {
            
            inMessage = new MessageImpl();
            ExchangeImpl exchange = new ExchangeImpl();
            exchange.setInMessage(inMessage);
            setupMessage(inMessage, context, req, resp);
            
            ((MessageImpl)inMessage).setDestination(this);
    
            exchange.setSession(new HTTPSession(req));
        }
        
        try {    
            incomingObserver.onMessage(inMessage);
            resp.flushBuffer();
            baseRequest.setHandled(true);
        } catch (SuspendedInvocationException ex) {
            if (ex.getRuntimeException() != null) {
                throw ex.getRuntimeException();
            }
            //else nothing to do
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
    
    protected OutputStream flushHeaders(Message outMessage, boolean getStream) throws IOException {
        OutputStream out = super.flushHeaders(outMessage, getStream);
        return wrapOutput(out);
    }
    
    private OutputStream wrapOutput(OutputStream out) {
        try {
            if (out instanceof Output) {
                out = new JettyOutputStream((Output)out);
            }
        } catch (Throwable t) {
            //ignore
        }
        return out;
    }
    
    
    static class JettyOutputStream extends FilterOutputStream implements CopyingOutputStream {
        final Output out;
        boolean written;
        public JettyOutputStream(Output o) {
            super(o);
            out = o;
        }

        @Override
        public int copyFrom(InputStream in) throws IOException {
            if (written) {
                return IOUtils.copy(in, out);
            }
            CountingInputStream c = new CountingInputStream(in);
            out.sendContent(c);
            return c.getCount();
        }
        public void write(int b) throws IOException {
            written = true;
            out.write(b);
        }
        public void write(byte b[], int off, int len) throws IOException {
            written = true;
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            // Avoid calling flush() here. It interferes with
            // content length calculation in the generator.
            out.close();
        }
    }
    static class CountingInputStream extends FilterInputStream {
        int count;
        public CountingInputStream(InputStream in) {
            super(in);
        }
        public int getCount() {
            return count;
        }
        
        @Override
        public int read() throws IOException {
            int i = super.read();
            if (i != -1) {
                ++count;
            }
            return i;
        }
        @Override
        public int read(byte[] b) throws IOException {
            int i = super.read(b);
            if (i != -1) {
                count += i;
            }
            return i;
        }
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int i = super.read(b, off, len);
            if (i != -1) {
                count += i;
            }
            return i;
        }
    }
    
 
    public ServerEngine getEngine() {
        return engine;
    }
   
    protected Message retrieveFromContinuation(HttpServletRequest req) {
        return (Message)req.getAttribute(CXF_CONTINUATION_MESSAGE);
    }
    protected void setupContinuation(Message inMessage,
                      final HttpServletRequest req, 
                      final HttpServletResponse resp) {
        if (engine.getContinuationsEnabled()) {
            inMessage.put(ContinuationProvider.class.getName(), 
                      new JettyContinuationProvider(req, resp, inMessage));
        }
    }
    
    private AbstractConnection getCurrentConnection() {
        // AbstractHttpConnection on Jetty 7.6, HttpConnection on Jetty <=7.5
        Class<?> cls = null;
        try {
            cls = ClassUtils.forName("org.eclipse.jetty.server.AbstractHttpConnection",
                                     AbstractConnection.class.getClassLoader());
        } catch (Exception e) {
            //ignore
        }
        if (cls == null) {
            try {
                cls = ClassUtils.forName("org.eclipse.jetty.server.HttpConnection",
                                         AbstractConnection.class.getClassLoader());
            } catch (Exception e) {
                //ignore
            }
        }

        try {
            return (AbstractConnection)ReflectionUtil
                .setAccessible(cls.getMethod("getCurrentConnection")).invoke(null);
        } catch (Exception e) {
            //ignore
        }
        return null;
    }
    private Request getCurrentRequest() {
        AbstractConnection con = getCurrentConnection();
        try {
            return (Request)ReflectionUtil
                .setAccessible(con.getClass().getMethod("getRequest")).invoke(con);
        } catch (Exception e) {
            //ignore
        }
        return null;
    }
}
