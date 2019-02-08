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

package org.apache.cxf.karaf.commands.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;


/**
 */
public class CXFController {
    private static final Logger LOG = LogUtils.getL7dLogger(CXFController.class);

    @Reference
    private BundleContext bundleContext;

    @Reference
    private ConfigurationAdmin configAdmin;

    public List<Bus> getBusses() {
        List<Bus> busses = new ArrayList<>();
        try {
            Collection<ServiceReference<Bus>> references = bundleContext.getServiceReferences(Bus.class, null);
            if (references != null) {
                for (ServiceReference<Bus> reference : references) {
                    if (reference != null) {
                        Bus bus = bundleContext.getService(reference);
                        if (bus != null) {
                            busses.add(bus);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Cannot retrieve the list of CXF Busses.", e);
        }
        return busses;
    }

    public Bus getBus(String name) {
        try {
            Collection<ServiceReference<Bus>> references = bundleContext.getServiceReferences(Bus.class, null);
            if (references != null) {
                for (ServiceReference<Bus> reference : references) {
                    if (reference != null
                        && name.equals(reference.getProperty("cxf.bus.id"))) {
                        return bundleContext.getService(reference);
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Cannot retrieve the CXF Bus.", e);
            return null;
        }
        LOG.log(Level.INFO, "Cannot retrieve the CXF Bus " + name + ".");
        return null;
    }

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

}
