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
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.https.CertConstraintsJaxBUtils;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;



public class UndertowHTTPDestination extends ServletDestination {

    private static final Logger LOG =
        LogUtils.getL7dLogger(UndertowHTTPDestination.class);

    protected UndertowHTTPServerEngine engine;
    protected UndertowHTTPServerEngineFactory serverEngineFactory;
    protected URL nurl;
    protected ClassLoader loader;
    protected ServletContext servletContext;
    /**
     * This variable signifies that finalizeConfig() has been called.
     * It gets called after this object has been spring configured.
     * It is used to automatically reinitialize things when resources
     * are reset.
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
    public UndertowHTTPDestination(Bus bus,
                                   DestinationRegistry registry,
                                   EndpointInfo ei,
                                   UndertowHTTPServerEngineFactory serverEngineFactory)
        throws IOException {
        //Add the default port if the address is missing it
        super(bus, registry, ei, getAddressValue(ei, true).getAddress(), true);
        this.serverEngineFactory = serverEngineFactory;
        loader = bus.getExtension(ClassLoader.class);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }



    /**
     * Post-configure retreival of server engine.
     */
    protected void retrieveEngine()
        throws GeneralSecurityException,
               IOException {
        if (serverEngineFactory == null) {
            return;
        }
        nurl = new URL(getAddress(endpointInfo));
        engine =
            serverEngineFactory.retrieveUndertowHTTPServerEngine(nurl.getPort());
        if (engine == null) {
            engine = serverEngineFactory.
                createUndertowHTTPServerEngine(nurl.getHost(), nurl.getPort(), nurl.getProtocol());
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

        if (engine != null) {
            UndertowHTTPHandler jhd = createUndertowHTTPHandler(this, contextMatchOnExact());
            engine.addServant(nurl, jhd);
        }
    }

    protected UndertowHTTPHandler createUndertowHTTPHandler(UndertowHTTPDestination jhd,
                                                    boolean cmExact) {
        return new UndertowHTTPHandler(jhd, cmExact);
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

    public void setServletContext(ServletContext sc) {
        servletContext = sc;
    }

    protected Message retrieveFromContinuation(HttpServletRequest req) {
        return (Message)req.getAttribute(CXF_CONTINUATION_MESSAGE);
    }

    protected void setupContinuation(Message inMessage, final HttpServletRequest req,
                                     final HttpServletResponse resp) {
        if (engine != null && engine.getContinuationsEnabled()) {
            super.setupContinuation(inMessage, req, resp);
        }
    }

    protected String getAddress(EndpointInfo endpointInfo) {
        return endpointInfo.getAddress();
    }

    public ServerEngine getEngine() {
        return engine;
    }

}
