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

import org.apache.aries.blueprint.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.container.BeanRecipe;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.osgi.service.blueprint.container.BlueprintContainer;

/**
 * 
 */
public class BlueprintBeanLocator implements ConfiguredBeanLocator {
    ConfiguredBeanLocator orig;
    ExtendedBlueprintContainer container;

    public BlueprintBeanLocator(ConfiguredBeanLocator orig, BlueprintContainer cont) {
        this.orig = orig;
        this.container = (ExtendedBlueprintContainer)cont;
        
        if (orig instanceof ExtensionManagerImpl) {
            List<String> names = new ArrayList<String>(container.getComponentIds());
            ((ExtensionManagerImpl)orig).removeBeansOfNames(names);
        }
    }
    
    
    /** {@inheritDoc}*/
    public List<String> getBeanNamesOfType(Class<?> type) {
        Set<String> names = new LinkedHashSet<String>();
        for (String s : container.getComponentIds()) {
            Recipe r = container.getRepository().getRecipe(s);
            if (r instanceof BeanRecipe 
                && type.isAssignableFrom(((BeanRecipe)r).getType())) {
                names.add(s);
            }
        }
        names.addAll(orig.getBeanNamesOfType(type));
        return new ArrayList<String>(names);
    }

    /** {@inheritDoc}*/
    public <T> Collection<? extends T> getBeansOfType(Class<T> type) {
        List<T> list = new ArrayList<T>();
        for (String s : container.getComponentIds()) {
            Recipe r = container.getRepository().getRecipe(s);
            if (r instanceof BeanRecipe 
                && type.isAssignableFrom(((BeanRecipe)r).getType())) {
                
                list.add(type.cast(container.getComponentInstance(s)));
            }
        }
        list.addAll(orig.getBeansOfType(type));
        return list;
    }

    /** {@inheritDoc}*/
    public <T> boolean loadBeansOfType(Class<T> type, BeanLoaderListener<T> listener) {
        List<String> names = new ArrayList<String>();
        for (String s : container.getComponentIds()) {
            Recipe r = container.getRepository().getRecipe(s);
            if (r instanceof BeanRecipe 
                && type.isAssignableFrom(((BeanRecipe)r).getType())) {
                names.add(s);
            }
        }
        Collections.reverse(names);
        boolean loaded = false;
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
        return loaded || orig.loadBeansOfType(type, listener);
    }

    public boolean hasConfiguredPropertyValue(String beanName, String propertyName, String value) {
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
        return orig.hasConfiguredPropertyValue(beanName, propertyName, value);
    }

}
