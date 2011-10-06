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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.aries.blueprint.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.container.BeanRecipe;
import org.apache.aries.blueprint.di.ExecutionContext;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;

/**
 * 
 */
public class BlueprintBeanLocator implements ConfiguredBeanLocator {
    private static final Logger LOG = LogUtils.getL7dLogger(BlueprintBeanLocator.class);
    ConfiguredBeanLocator orig;
    ExtendedBlueprintContainer container;
    BundleContext context;
    
    public BlueprintBeanLocator(ConfiguredBeanLocator orig, 
                                BlueprintContainer cont, 
                                BundleContext context) {
        this.orig = orig;
        this.container = (ExtendedBlueprintContainer)cont;
        this.context = context;
        if (orig instanceof ExtensionManagerImpl) {
            List<String> names = new ArrayList<String>(container.getComponentIds());
            ((ExtensionManagerImpl)orig).removeBeansOfNames(names);
        }
    }
    
    public <T> T getBeanOfType(String name, Class<T> type) {
        ExecutionContext origContext 
            = ExecutionContext.Holder.setContext((ExecutionContext)container.getRepository());
        try {
            Recipe r = container.getRepository().getRecipe(name);
            if (r instanceof BeanRecipe && ((BeanRecipe)r).getType() != null
                && type.isAssignableFrom(((BeanRecipe)r).getType())) {
                return type.cast(container.getComponentInstance(name));
            }
        } finally {
            ExecutionContext.Holder.setContext(origContext);
        }
        return orig.getBeanOfType(name, type);
    }
    /** {@inheritDoc}*/
    public List<String> getBeanNamesOfType(Class<?> type) {
        Set<String> names = new LinkedHashSet<String>();
        ExecutionContext origContext 
            = ExecutionContext.Holder.setContext((ExecutionContext)container.getRepository());
        try {
            for (String s : container.getComponentIds()) {
                Recipe r = container.getRepository().getRecipe(s);
                if (r instanceof BeanRecipe && ((BeanRecipe)r).getType() != null
                    && type.isAssignableFrom(((BeanRecipe)r).getType())) {
                    names.add(s);
                }
            }
        } finally {
            ExecutionContext.Holder.setContext(origContext);
        }
        names.addAll(orig.getBeanNamesOfType(type));
        return new ArrayList<String>(names);
    }

    /** {@inheritDoc}*/
    public <T> Collection<? extends T> getBeansOfType(Class<T> type) {
        List<T> list = new ArrayList<T>();
        
        ExecutionContext origContext 
            = ExecutionContext.Holder.setContext((ExecutionContext)container.getRepository());
        try {
            for (String s : container.getComponentIds()) {
                Recipe r = container.getRepository().getRecipe(s);
                if (r instanceof BeanRecipe && ((BeanRecipe)r).getType() != null
                    && type.isAssignableFrom(((BeanRecipe)r).getType())) {
                    
                    list.add(type.cast(container.getComponentInstance(s)));
                } 
            }
        } finally {
            ExecutionContext.Holder.setContext(origContext);
        }
        list.addAll(orig.getBeansOfType(type));
        if (list.isEmpty()) {
            try {
                ServiceReference refs[] = context.getServiceReferences(type.getName(), null);
                if (refs != null) {
                    for (ServiceReference r : refs) {
                        list.add(type.cast(context.getService(r)));
                    }
                }
            } catch (Exception ex) {
                //ignore, just don't support the OSGi services
                LOG.info("Try to find the Bean with type:" + type 
                    + " from OSGi services and get error: " + ex);  
            }
        }
        
        return list;
    }

    /** {@inheritDoc}*/
    public <T> boolean loadBeansOfType(Class<T> type, BeanLoaderListener<T> listener) {
        List<String> names = new ArrayList<String>();
        boolean loaded = false;
        ExecutionContext origContext 
            = ExecutionContext.Holder.setContext((ExecutionContext)container.getRepository());
        try {
            for (String s : container.getComponentIds()) {
                Recipe r = container.getRepository().getRecipe(s);
                if (r instanceof BeanRecipe && ((BeanRecipe)r).getType() != null
                    && type.isAssignableFrom(((BeanRecipe)r).getType())) {
                    names.add(s);
                }
            }
            Collections.reverse(names);
            for (String s : names) {
                BeanRecipe r = (BeanRecipe)container.getRepository().getRecipe(s);
                Class<?> beanType = r.getType();
                Class<? extends T> t = beanType.asSubclass(type);
                if (listener.loadBean(s, t)) {
                    Object o = container.getComponentInstance(s);
                    if (listener.beanLoaded(s, type.cast(o))) {
                        return true;
                    }
                    loaded = true;
                }
            }
        } finally {
            ExecutionContext.Holder.setContext(origContext);
        }
        return loaded || orig.loadBeansOfType(type, listener);
    }

    public boolean hasConfiguredPropertyValue(String beanName, String propertyName, String value) {
        ExecutionContext origContext 
            = ExecutionContext.Holder.setContext((ExecutionContext)container.getRepository());
        try {
            Recipe r = container.getRepository().getRecipe(beanName);
            if (r instanceof BeanRecipe) {
                BeanRecipe br = (BeanRecipe)r;
                Object o = br.getProperty(propertyName);
                if (o == null) {
                    return false;
                }
                //TODO - need to check the values of the property
                return false;
            }
        } finally {
            ExecutionContext.Holder.setContext(origContext);
        }
        return orig.hasConfiguredPropertyValue(beanName, propertyName, value);
    }

}
