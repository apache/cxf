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

package org.apache.cxf.bus.managers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.AbstractBindingFactory;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.helpers.CastUtils;

@NoJSR250Annotations(unlessNull = "bus")
public final class BindingFactoryManagerImpl implements BindingFactoryManager {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(BindingFactoryManagerImpl.class);

    Map<String, BindingFactory> bindingFactories;
    Set<String> failed = new CopyOnWriteArraySet<>();
    Set<String> loaded = new CopyOnWriteArraySet<>();
    Bus bus;

    public BindingFactoryManagerImpl() {
        bindingFactories = new ConcurrentHashMap<>(8, 0.75f, 4);
    }
    public BindingFactoryManagerImpl(Bus b) {
        bindingFactories = new ConcurrentHashMap<>(8, 0.75f, 4);
        setBus(b);
    }

    @Resource
    public void setBus(Bus b) {
        bus = b;
        if (null != bus) {
            bus.setExtension(this, BindingFactoryManager.class);
        }
    }

    public void registerBindingFactory(String name,
                                       BindingFactory factory) {
        bindingFactories.put(name, factory);
    }

    public void unregisterBindingFactory(String name) {
        bindingFactories.remove(name);
    }

    public BindingFactory getBindingFactory(final String namespace) throws BusException {
        BindingFactory factory = bindingFactories.get(namespace);
        if (null == factory) {
            if (!failed.contains(namespace)) {
                factory = loadActivationNamespace(namespace);
                if (factory == null) {
                    factory = loadDefaultNamespace(namespace);
                }
                if (factory == null) {
                    factory = loadAll(namespace);
                }
            }
            if (factory == null) {
                failed.add(namespace);
                throw new BusException(new Message("NO_BINDING_FACTORY_EXC", BUNDLE, namespace));
            }
        }
        return factory;
    }

    private BindingFactory loadAll(final String namespace) {
        //Try old method of having activationNamespaces configured in.
        //It activates all the factories in the list until one matches, thus
        //it activates stuff that really aren't needed.
        ConfiguredBeanLocator.BeanLoaderListener<BindingFactory> listener
            = new ConfiguredBeanLocator.BeanLoaderListener<BindingFactory>() {
                public boolean beanLoaded(String name, BindingFactory bean) {
                    loaded.add(name);
                    if (!bindingFactories.containsKey(namespace)) {
                        if (bean instanceof AbstractBindingFactory) {
                            for (String ns
                                 : ((AbstractBindingFactory)bean).getActivationNamespaces()) {
                                registerBindingFactory(ns, bean);
                            }
                        } else {
                            try {
                                Method m = bean.getClass().getMethod("getActivationNamespace");
                                Collection<String> c = CastUtils.cast((Collection<?>)m.invoke(bean));
                                for (String s : c) {
                                    registerBindingFactory(s, bean);
                                }
                            } catch (Exception ex) {
                                //ignore
                            }
                        }
                    }
                    return bindingFactories.containsKey(namespace);
                }

                public boolean loadBean(String name, Class<? extends BindingFactory> type) {
                    return !bindingFactories.containsKey(namespace) && !loaded.contains(name);
                }
            };
        bus.getExtension(ConfiguredBeanLocator.class)
            .loadBeansOfType(BindingFactory.class,
                             listener);
        return bindingFactories.get(namespace);
    }
    private BindingFactory loadDefaultNamespace(final String namespace) {
        //First attempt will be to examine the factory class
        //for a DEFAULT_NAMESPACES field and use it
        ConfiguredBeanLocator.BeanLoaderListener<BindingFactory> listener
            = new ConfiguredBeanLocator.BeanLoaderListener<BindingFactory>() {
                public boolean beanLoaded(String name, BindingFactory bean) {
                    loaded.add(name);
                    return bindingFactories.containsKey(namespace);
                }

                public boolean loadBean(String name, Class<? extends BindingFactory> type) {
                    if (!loaded.contains(name)) {
                        try {
                            Field f = type.getField("DEFAULT_NAMESPACES");
                            Object o = f.get(null);
                            if (o instanceof Collection) {
                                Collection<String> c = CastUtils.cast((Collection<?>)o);
                                return c.contains(namespace);
                            }
                        } catch (Exception ex) {
                            //ignore
                        }
                    }
                    return false;
                }
            };
        bus.getExtension(ConfiguredBeanLocator.class)
            .loadBeansOfType(BindingFactory.class,
                             listener);

        return bindingFactories.get(namespace);
    }
    private BindingFactory loadActivationNamespace(final String namespace) {
        final ConfiguredBeanLocator locator = bus.getExtension(ConfiguredBeanLocator.class);

        //Second attempt will be to examine the factory class
        //for a DEFAULT_NAMESPACES field and if it doesn't exist, try
        //using the older activation ns things.  This will then load most
        //of the "older" things
        ConfiguredBeanLocator.BeanLoaderListener<BindingFactory> listener
            = new ConfiguredBeanLocator.BeanLoaderListener<BindingFactory>() {
                public boolean beanLoaded(String name, BindingFactory bean) {
                    loaded.add(name);
                    return bindingFactories.containsKey(namespace);
                }

                public boolean loadBean(String name, Class<? extends BindingFactory> type) {
                    if (loaded.contains(name)) {
                        return false;
                    }
                    try {
                        type.getField("DEFAULT_NAMESPACES");
                        return false;
                    } catch (Exception ex) {
                        //ignore
                    }
                    return locator.hasConfiguredPropertyValue(name, "activationNamespaces", namespace);
                }
            };
        locator.loadBeansOfType(BindingFactory.class,
                                listener);

        return bindingFactories.get(namespace);
    }
}
