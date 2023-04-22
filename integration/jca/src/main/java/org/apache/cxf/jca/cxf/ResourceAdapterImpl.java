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
package org.apache.cxf.jca.cxf;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.xa.XAResource;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.Work;
import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.core.resourceadapter.ResourceBean;
import org.apache.cxf.jca.inbound.InboundEndpoint;
import org.apache.cxf.jca.inbound.MDBActivationSpec;
import org.apache.cxf.jca.inbound.MDBActivationWork;

public class ResourceAdapterImpl extends ResourceBean implements ResourceAdapter {

    private static final long serialVersionUID = 5318740621610762307L;
    private static final Logger LOG = LogUtils.getL7dLogger(ResourceAdapterImpl.class);
    private BootstrapContext ctx;
    private Set<Bus> busCache = new HashSet<>();
    private Map<String, InboundEndpoint> endpoints = new ConcurrentHashMap<String, InboundEndpoint>();

    public ResourceAdapterImpl() {
        super();
    }

    public ResourceAdapterImpl(Properties props) {
        super(props);
    }

    public void registerBus(Bus bus) {
        LOG.fine("Bus " + bus + " initialized and added to ResourceAdapter busCache");
        busCache.add(bus);
    }

    protected Set<Bus> getBusCache() {
        return busCache;
    }

    protected void setBusCache(Set<Bus> cache) {
        this.busCache = cache;
    }

    public void start(BootstrapContext aCtx) throws ResourceAdapterInternalException {
        LOG.fine("Resource Adapter is starting by appserver...");
        if (aCtx == null) {
            throw new ResourceAdapterInternalException("BootstrapContext can not be null");
        }
        this.ctx = aCtx;
    }

    public void stop() {
        LOG.fine("Resource Adapter is being stopped by appserver...");
        if (!busCache.isEmpty()) {
            for (Bus bus : busCache) {
                bus.shutdown(true);
            }
        }

        // shutdown all the inbound endpoints
        for (Map.Entry<String, InboundEndpoint> entry : endpoints.entrySet()) {
            try {
                entry.getValue().shutdown();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to stop endpoint " + entry.getKey(), e);
            }
        }
        endpoints.clear();
    }

    public XAResource[] getXAResources(ActivationSpec[] as)
        throws ResourceException {
        throw new NotSupportedException();
    }

    public void endpointActivation(MessageEndpointFactory mef, ActivationSpec as)
        throws ResourceException {

        if  (!(as instanceof MDBActivationSpec)) {
            LOG.fine("Ignored unknown activation spec " + as);
            return;
        }

        MDBActivationSpec spec = (MDBActivationSpec)as;
        LOG.info("CXF resource adapter is activating " + spec.getDisplayName());

        Work work = new MDBActivationWork(spec, mef, endpoints);
        ctx.getWorkManager().scheduleWork(work);

    }

    public void endpointDeactivation(MessageEndpointFactory mef, ActivationSpec as) {

        if  (!(as instanceof MDBActivationSpec)) {
            LOG.fine("Ignored unknown activation spec " + as);
            return;
        }

        MDBActivationSpec spec = (MDBActivationSpec)as;
        LOG.info("CXF resource adapter is deactivating " + spec.getDisplayName());

        InboundEndpoint endpoint = endpoints.remove(spec.getDisplayName());
        if (endpoint != null) {
            try {
                endpoint.shutdown();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to stop endpoint "
                        + spec.getDisplayName(), e);
            }
        }
    }

    public BootstrapContext getBootstrapContext() {
        return ctx;
    }
}

















