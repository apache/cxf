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
package org.apache.cxf.bus.blueprint;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.cxf.common.logging.LogUtils;
import org.osgi.framework.BundleContext;

public final class NamespaceHandlerRegisterer {
    private static final Logger LOG = LogUtils.getL7dLogger(NamespaceHandlerRegisterer.class);

    private NamespaceHandlerRegisterer() {
    }

    public static void register(BundleContext bc, BlueprintNameSpaceHandlerFactory factory,
                                String... namespaces) {
        try {
            Object handler = factory.createNamespaceHandler();
            for (String namespace : namespaces) {
                Dictionary<String, String> properties = new Hashtable<>();
                properties.put("osgi.service.blueprint.namespace", namespace);
                bc.registerService(NamespaceHandler.class.getName(), handler, properties);
                LOG.fine("Registered blueprint namespace handler for " + namespace);
            }
        } catch (NoClassDefFoundError e) {
            LOG.log(Level.INFO, "Aries Blueprint packages not available. So namespaces will not be registered");
        } catch (Throwable e) {
            LOG.log(Level.WARNING, "Unexpected exception when trying to install Aries Blueprint namespaces", e);
        }
    }

}
