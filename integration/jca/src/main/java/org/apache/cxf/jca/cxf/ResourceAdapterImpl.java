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
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.core.resourceadapter.ResourceBean;

public class ResourceAdapterImpl extends ResourceBean implements ResourceAdapter {

    private static final Logger LOG = LogUtils.getL7dLogger(ResourceAdapterImpl.class);
    private BootstrapContext ctx;
    private Set <Bus> busCache = new HashSet<Bus>();
   
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
    }

    public XAResource[] getXAResources(ActivationSpec as[])
        throws ResourceException {
        throw new NotSupportedException();
    }

    public void endpointActivation(MessageEndpointFactory mef, ActivationSpec as)
        throws ResourceException {
        throw new NotSupportedException();
    }

    public void endpointDeactivation(MessageEndpointFactory mef, ActivationSpec as) {
    }

    public BootstrapContext getBootstrapContext() {
        return ctx;
    }
}
















