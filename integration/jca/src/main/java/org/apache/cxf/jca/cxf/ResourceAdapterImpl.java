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
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.transaction.xa.XAResource;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.core.resourceadapter.ResourceBean;

import org.apache.cxf.jca.inbound.InboundEndpoint;
import org.apache.cxf.jca.inbound.MDBActivationSpec;
import org.apache.cxf.jca.inbound.MDBActivationWork;

public class ResourceAdapterImpl extends ResourceBean implements ResourceAdapter {

    private static final Logger LOG = LogUtils.getL7dLogger(ResourceAdapterImpl.class);
    private BootstrapContext ctx;
    private Set <Bus> busCache = new HashSet<Bus>();
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

    protected Set getBusCache() {
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
            Iterator busIterator = busCache.iterator();
            Bus bus = null;
            while (busIterator.hasNext()) {
                bus = (Bus)busIterator.next();
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

    public XAResource[] getXAResources(ActivationSpec as[])
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

















