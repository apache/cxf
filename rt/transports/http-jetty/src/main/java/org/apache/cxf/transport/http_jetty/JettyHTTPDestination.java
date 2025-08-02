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

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.security.CertificateConstraintsType;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CopyingOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.https.CertConstraintsJaxBUtils;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.eclipse.jetty.ee11.servlet.HttpOutput;


public class JettyHTTPDestination extends ServletDestination {

    private static final Logger LOG =
        LogUtils.getL7dLogger(JettyHTTPDestination.class);

    protected JettyHTTPServerEngine engine;
    protected JettyHTTPServerEngineFactory serverEngineFactory;
    protected JettyHTTPHandler handler;
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
     * @param bus  the associated Bus
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
        this(bus, registry, ei, 
             serverEngineFactory == null ? null : new URL(getAddressValue(ei, true).getAddress()),
             serverEngineFactory);
    }

    
    protected JettyHTTPDestination(Bus bus,
                                   DestinationRegistry registry,
                                   EndpointInfo ei,
                                   URL nurl,
                                   JettyHTTPServerEngineFactory serverEngineFactory)
        throws IOException {
        //Add the default port if the address is missing it
        super(bus, registry, ei, getAddressValue(ei, true).getAddress(), true);
        this.serverEngineFactory = serverEngineFactory;
        this.nurl = nurl;
        loader = bus.getExtension(ClassLoader.class);
    }
    
    protected Logger getLogger() {
        return LOG;
    }

    public void setServletContext(ServletContext sc) {
        servletContext = sc;
    }

    /**
     * Post-configure retrieval of server engine.
     */
    protected void retrieveEngine()
        throws GeneralSecurityException,
               IOException {
        if (serverEngineFactory == null) {
            return;
        }
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

    protected String getAddress(EndpointInfo endpointInfo) {
        return endpointInfo.getAddress();
    }

    /**
     * Activate receipt of incoming messages.
     */
    protected void activate() {
        super.activate();
        LOG.log(Level.FINE, "Activating receipt of incoming messages");
        // pick the handler supporting websocket if jetty-websocket is available otherwise pick the default handler.

        if (engine != null) {
            handler = createJettyHTTPHandler(this, contextMatchOnExact());
            engine.addServant(nurl, handler);
        }
    }

    protected JettyHTTPHandler createJettyHTTPHandler(JettyHTTPDestination jhd,
                                                    boolean cmExact) {
        return new JettyHTTPHandler(jhd, cmExact);
    }

    /**
     * Deactivate receipt of incoming messages.
     */
    protected void deactivate() {
        super.deactivate();
        LOG.log(Level.FINE, "Deactivating receipt of incoming messages");
        if (engine != null) {
            engine.removeServant(nurl);
        }
        handler = null;
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

    protected void doService(ServletContext context,
                             HttpServletRequest req,
                             HttpServletResponse resp) throws IOException {
        if (context == null) {
            context = servletContext;
        }
        
        HTTPServerPolicy sp = getServer();
        if (sp.isSetRedirectURL()) {
            resp.sendRedirect(sp.getRedirectURL());
            resp.flushBuffer();
            return;
        }

        // REVISIT: service on executor if associated with endpoint
        ClassLoaderHolder origLoader = null;
        Bus origBus = BusFactory.getAndSetThreadDefaultBus(bus);
        try {
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            invoke(null, context, req, resp);
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
            if (origLoader != null) {
                origLoader.reset();
            }
        }
    }

    protected void invokeComplete(final ServletContext context,
                                  final HttpServletRequest req,
                                  final HttpServletResponse resp,
                                  Message m) throws IOException {
        resp.flushBuffer();
       
        super.invokeComplete(context, req, resp, m);
    }

    protected OutputStream flushHeaders(Message outMessage, boolean getStream) throws IOException {
        OutputStream out = super.flushHeaders(outMessage, getStream);
        return wrapOutput(out);
    }

    private OutputStream wrapOutput(OutputStream out) {
        try {
            if (out instanceof HttpOutput) {
                out = new JettyOutputStream((HttpOutput)out);
            }
        } catch (Throwable t) {
            //ignore
        }
        return out;
    }


    static class JettyOutputStream extends FilterOutputStream implements CopyingOutputStream {
        final HttpOutput out;
        boolean written;
        JettyOutputStream(HttpOutput o) {
            super(o);
            out = o;
        }

        private boolean sendContent(Class<?> type, InputStream c) throws IOException {
            try {
                out.sendContent(c);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        @Override
        public int copyFrom(InputStream in) throws IOException {
            if (written) {
                return IOUtils.copy(in, out);
            }
            CountingInputStream c = new CountingInputStream(in);
            if (!sendContent(InputStream.class, c)
                && !sendContent(Object.class, c)) {
                IOUtils.copy(c, out);
            }
            return c.getCount();
        }
        public void write(int b) throws IOException {
            written = true;
            out.write(b);
        }
        public void write(byte[] b, int off, int len) throws IOException {
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
        CountingInputStream(InputStream in) {
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
    
}
