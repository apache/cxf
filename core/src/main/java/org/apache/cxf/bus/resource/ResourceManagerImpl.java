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

package org.apache.cxf.bus.resource;

import java.util.List;

import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.extension.BusExtension;
import org.apache.cxf.resource.DefaultResourceManager;
import org.apache.cxf.resource.ObjectTypeResolver;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;

@NoJSR250Annotations(unlessNull = "bus")
public class ResourceManagerImpl extends DefaultResourceManager implements BusExtension {

    private Bus bus;

    public ResourceManagerImpl() {
    }

    public ResourceManagerImpl(Bus b) {
        super();
        setBus(b);
    }
    protected void onFirstResolve() {
        super.onFirstResolve();
        if (bus != null) {
            ConfiguredBeanLocator locator = bus.getExtension(ConfiguredBeanLocator.class);
            if (locator != null) {
                this.addResourceResolvers(locator.getBeansOfType(ResourceResolver.class));
            }
        }
    }

    /**
     * Set the list of resolvers for this resource manager.
     * @param resolvers
     */
    public final void setResolvers(List<? extends ResourceResolver> resolvers) {
        registeredResolvers.clear();
        registeredResolvers.addAll(resolvers);
    }

    @Resource
    public final void setBus(Bus b) {
        if (bus != b) {
            bus = b;
            firstCalled = false;
            super.addResourceResolver(new ObjectTypeResolver(bus));
            if (null != bus) {
                bus.setExtension(this, ResourceManager.class);
            }
        }
    }

    public Class<?> getRegistrationType() {
        return ResourceManager.class;
    }


}
