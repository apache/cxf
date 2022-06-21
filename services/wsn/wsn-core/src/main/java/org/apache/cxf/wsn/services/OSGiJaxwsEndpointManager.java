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

import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.blueprint.BlueprintBus;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.wsn.EndpointRegistrationException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.blueprint.container.BlueprintContainer;

import static org.apache.cxf.bus.osgi.OSGIBusListener.CONTEXT_NAME_PROPERTY;
import static org.apache.cxf.bus.osgi.OSGIBusListener.CONTEXT_SYMBOLIC_NAME_PROPERTY;
import static org.apache.cxf.bus.osgi.OSGIBusListener.CONTEXT_VERSION_PROPERTY;

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

    public Endpoint register(String address, Object service, URL wsdlLocation) throws EndpointRegistrationException {
        Object o = setCXFBus();
        try {
            return super.register(address, service, wsdlLocation);
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
        if (null != bundleContext) {
            Map<String, Object> props = new HashMap<>();
            props.put(CONTEXT_SYMBOLIC_NAME_PROPERTY, bundleContext.getBundle().getSymbolicName());
            props.put(CONTEXT_VERSION_PROPERTY, getBundleVersion(bundleContext.getBundle()));
            props.put(CONTEXT_NAME_PROPERTY, bp.getId());
            bundleContext.registerService(Bus.class.getName(), bp, CollectionUtils.toDictionary(props));
        }
        cxfBus = bp;
    }
    private Version getBundleVersion(Bundle bundle) {
        Dictionary<?, ?> headers = bundle.getHeaders();
        String version = (String) headers.get(Constants.BUNDLE_VERSION);
        return (version != null) ? Version.parseVersion(version) : Version.emptyVersion;
    }

}
