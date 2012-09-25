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

package org.apache.cxf.wsn.services;

import javax.management.MBeanServer;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.blueprint.BlueprintBus;
import org.apache.cxf.wsn.EndpointRegistrationException;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

/**
 * 
 */
public class OSGiJaxwsEndpointManager extends JaxwsEndpointManager {
    
    private BundleContext bundleContext;
    private BlueprintContainer container;
    private Object cxfBus;
    private boolean hasCXF = true;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.mbeanServer = (MBeanServer)bundleContext
            .getService(bundleContext.getServiceReference(MBeanServer.class.getName()));
    }
    public void setBlueprintContainer(BlueprintContainer c) {
        this.container = c;
    }

    public Endpoint register(String address, Object service) throws EndpointRegistrationException {
        Object o = setCXFBus();
        try {
            return super.register(address, service);
        } finally {
            restoreCXFBus(o);
        }
    }
    private void restoreCXFBus(Object o) {
        if (hasCXF) {
            restoreCXFBusInternal(o);
        }
    }
    private Object setCXFBus() {
        if (cxfBus == null && hasCXF) {
            try {
                createCXFBus();
            } catch (Throwable t) {
                hasCXF = false;
            }
        }
        if (hasCXF) {
            return setCXFBusInternal();
        }
        return null;
    }
    
    public void destroy() {
        if (cxfBus != null) {
            destroyBus();
        }
    }
    
    
    private void destroyBus() {
        ((Bus)cxfBus).shutdown(true);
        cxfBus = null;
    }
    private void restoreCXFBusInternal(Object o) {
        BusFactory.setThreadDefaultBus((Bus)o);
    }

    private Object setCXFBusInternal() {
        return BusFactory.getAndSetThreadDefaultBus((Bus)cxfBus);
    }
    private void createCXFBus() {
        BlueprintBus bp = new BlueprintBus();
        bp.setBundleContext(bundleContext);
        bp.setBlueprintContainer(container);
        bp.setId("WS-Notification");
        bp.initialize();
        cxfBus = bp;
    }
    
}
