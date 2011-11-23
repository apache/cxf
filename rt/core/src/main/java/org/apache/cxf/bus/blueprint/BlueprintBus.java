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


import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.configuration.Configurer;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

/**
 * 
 */
public class BlueprintBus extends ExtensionManagerBus {

    BundleContext context;
    BlueprintContainer container;
    
    public BlueprintBus() {
        // Using the BlueprintBus Classloader to load the extensions
        super(null, null, BlueprintBus.class.getClassLoader());
    }
    
    public void setBundleContext(BundleContext c) {
        context = c;
        super.setExtension(new BundleDelegatingClassLoader(c.getBundle(), 
                                                           this.getClass().getClassLoader()),
                           ClassLoader.class);
        super.setExtension(c, BundleContext.class);
    }
    public void setBlueprintContainer(BlueprintContainer con) {
        container = con;
        setExtension(new ConfigurerImpl(con), Configurer.class);
        setExtension(new BlueprintBeanLocator(getExtension(ConfiguredBeanLocator.class), container, context),
                           ConfiguredBeanLocator.class);
    }
    public String getId() {
        if (id == null) {
            id = context.getBundle().getSymbolicName() + "-" 
                + DEFAULT_BUS_ID + Integer.toString(this.hashCode());
        }
        return id;
    }

}
