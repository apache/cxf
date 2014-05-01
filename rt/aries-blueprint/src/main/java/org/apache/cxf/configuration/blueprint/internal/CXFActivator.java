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

package org.apache.cxf.configuration.blueprint.internal;

import org.apache.cxf.bus.blueprint.BlueprintNameSpaceHandlerFactory;
import org.apache.cxf.bus.blueprint.NamespaceHandlerRegisterer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Is called in OSGi on start and stop of the cxf bundle.
 * Manages 
 * - CXFBundleListener
 * - Attaching ManagedWorkqueues to config admin service
 * - OsgiBusListener
 * - Blueprint namespaces
 */
public class CXFActivator implements BundleActivator {

    /** {@inheritDoc}*/
    public void start(BundleContext context) throws Exception {

        BlueprintNameSpaceHandlerFactory factory = new BlueprintNameSpaceHandlerFactory() {
                
            @Override
            public Object createNamespaceHandler() {
                return new CXFAPINamespaceHandler();
            }
        };
        NamespaceHandlerRegisterer.register(context, factory,
                                            "http://cxf.apache.org/blueprint/core",
                                            "http://cxf.apache.org/configuration/beans",
                                            "http://cxf.apache.org/configuration/parameterized-types",
                                            "http://cxf.apache.org/configuration/security",
                                            "http://schemas.xmlsoap.org/wsdl/",
                                            "http://www.w3.org/2005/08/addressing",
                                            "http://schemas.xmlsoap.org/ws/2004/08/addressing");

    }

    /** {@inheritDoc}*/
    public void stop(BundleContext context) throws Exception {
    }

}
