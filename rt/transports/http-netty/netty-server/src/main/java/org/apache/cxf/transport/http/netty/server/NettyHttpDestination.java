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

package org.apache.cxf.transport.http.netty.server;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;

public class NettyHttpDestination extends AbstractHTTPDestination {

    private static final Logger LOG =
            LogUtils.getL7dLogger(NettyHttpDestination.class);

    protected NettyHttpServerEngine engine;
    protected NettyHttpServerEngineFactory serverEngineFactory;
    protected ServletContext servletContext;
    protected ClassLoader loader;
    protected URL nurl;

    private boolean configFinalized;

    /**
     * Constructor
     *
     * @param b                   the associated Bus
     * @param registry            the associated destinationRegistry
     * @param ei                  the endpoint info of the destination
     * @param serverEngineFactory the serverEngineFactory which could be used to create ServerEngine
     * @throws java.io.IOException
     */
    public NettyHttpDestination(Bus b, DestinationRegistry registry,
                                EndpointInfo ei, NettyHttpServerEngineFactory serverEngineFactory) throws IOException {
        //Add the default port if the address is missing it
        super(b, registry, ei, getAddressValue(ei, true).getAddress(), true);
        loader = bus.getExtension(ClassLoader.class);
        this.serverEngineFactory = serverEngineFactory;
        nurl = new URL(getAddress(endpointInfo));
    }


    @Override
    protected Logger getLogger() {
        return LOG;
    }

    protected void retrieveEngine()
        throws IOException {

        engine = serverEngineFactory.retrieveNettyHttpServerEngine(nurl.getPort());
        if (engine == null) {
            engine =
                serverEngineFactory.createNettyHttpServerEngine(nurl.getHost(), nurl.getPort(), nurl.getProtocol());
        }

        assert engine != null;
    }

    public void finalizeConfig() {
        assert !configFinalized;

        try {
            retrieveEngine();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        configFinalized = true;
    }

    private String getAddress(EndpointInfo endpointInfo) {
        String address = endpointInfo.getAddress();
        if (address.startsWith("netty://")) {
            address = address.substring(8);
        }
        return address;
    }

    protected String getBasePath(String contextPath) throws IOException {
        if (StringUtils.isEmpty(endpointInfo.getAddress())) {
            return "";
        }
        return new URL(getAddress(endpointInfo)).getPath();
    }

    /**
     * Activate receipt of incoming messages.
     */
    protected void activate() {
        super.activate();
        LOG.log(Level.FINE, "Activating receipt of incoming messages");
        final URL url;

        try {
            url = new URL(getAddress(endpointInfo));
        } catch (Exception e) {
            throw new Fault(e);
        }
        // setup the path for it

        engine.addServant(url,
                new NettyHttpHandler(this, contextMatchOnExact()));
    }

    /**
     * Deactivate receipt of incoming messages.
     */
    protected void deactivate() {
        super.deactivate();
        LOG.log(Level.FINE, "Deactivating receipt of incoming messages");
        engine.removeServant(nurl);
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

        if (getServer().isSetRedirectURL()) {
            resp.sendRedirect(getServer().getRedirectURL());
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

    public ServerEngine getEngine() {
        return engine;
    }

    protected Message retrieveFromContinuation(HttpServletRequest req) {
        return (Message) req.getAttribute(CXF_CONTINUATION_MESSAGE);
    }


    protected void setupContinuation(Message inMessage,
                                     final HttpServletRequest req,
                                     final HttpServletResponse resp) {
        // Here we don't support the Continuation
    }

    protected String getBasePathForFullAddress(String addr) {
        try {
            return new URL(addr).getPath();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }


}
