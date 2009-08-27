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

package org.apache.cxf.transport;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.helpers.CastUtils;

/**
 * 
 */
public class TransportFinder<T> {
    Map<String, T> map;
    Set<String> loaded;
    Class<T> cls;
    ConfiguredBeanLocator locator;
    
    public TransportFinder(Bus b,
                           Map<String, T> m,
                           Set<String> l,
                           Class<T> c) {
        map = m;
        cls = c;
        locator = b.getExtension(ConfiguredBeanLocator.class);
        loaded = l;
    }
    
    public T findTransportForNamespace(final String namespace) {
        if (locator == null) {
            return null;
        }
        T factory = loadDefaultNamespace(namespace);
        if (factory == null) {
            factory = loadActivationNamespaces(namespace);
        }
        if (factory == null) {
            factory = loadNoDefaultNamespace(namespace);
        }
        if (factory == null) {
            loadAll();
            factory = map.get(namespace);
        }
        return factory;
    }
    

    public T findTransportForURI(String uri) {
        if (locator == null) {
            return null;
        }
        //If the uri is related path or has no protocol prefix , we will set it to be http
        if (uri.startsWith("/") || uri.indexOf(":") < 0) {
            uri = "http://" + uri;
        }
        T factory = checkForURI(uri);
        if (factory == null) {
            //didn't find, now well need to search
            factory = loadDefaultURIs(uri);
            
            if (factory == null) {
                loadAll();
                factory = checkForURI(uri);
            }
        }
        return factory;
    }
    
    private static Set<String> getPrefixes(Object t) {
        Set<String> prefixes = null;
        if (t instanceof AbstractTransportFactory) {
            AbstractTransportFactory atf = (AbstractTransportFactory)t;
            prefixes = atf.getUriPrefixes();
        } else if (t instanceof DestinationFactory) {
            DestinationFactory atf = (DestinationFactory)t;
            prefixes = atf.getUriPrefixes();                
        } else if (t instanceof ConduitInitiator) {
            ConduitInitiator atf = (ConduitInitiator)t;
            prefixes = atf.getUriPrefixes();                
        }
        return prefixes;
    }
    private boolean hasPrefix(String uri, Collection<String> prefixes) {
        if (prefixes == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    public T checkForURI(String uri) {
        //first attempt the ones already registered
        for (T t : map.values()) {
            if (hasPrefix(uri, getPrefixes(t))) {
                return t;
            }
        }
        return null;
    }
    
    private void loadAll() {
        ConfiguredBeanLocator.BeanLoaderListener<T> listener 
            = new ConfiguredBeanLocator.BeanLoaderListener<T>() {
                public boolean beanLoaded(String name, T bean) {
                    loaded.add(name);
                    registerBean(bean);
                    return false;
                }
                public boolean loadBean(String name, Class<? extends T> type) {
                    return !loaded.contains(name);
                }
            };
        locator.loadBeansOfType(cls, listener);
    }

    private void registerBean(T bean) {
        if (bean instanceof AbstractTransportFactory) {
            for (String ns 
                 : ((AbstractTransportFactory)bean).getTransportIds()) {
                if (!map.containsKey(ns)) {
                    map.put(ns, bean);
                }
            }
        } else {
            try {
                Method m = bean.getClass().getMethod("getActivationNamespaces", new Class[0]);
                Collection<String> c = CastUtils.cast((Collection<?>)m.invoke(bean));
                for (String s : c) {
                    if (!map.containsKey(s)) {
                        map.put(s, bean);
                    }
                }
            } catch (Exception ex) {
                //ignore
            }
        }
    }

    
    private T loadActivationNamespaces(final String namespace) {
        //Try old method of having activationNamespaces configured in. 
        ConfiguredBeanLocator.BeanLoaderListener<T> listener 
            = new ConfiguredBeanLocator.BeanLoaderListener<T>() {
                public boolean beanLoaded(String name, T bean) {
                    loaded.add(name);
                    if (!map.containsKey(namespace)) {
                        registerBean(bean);
                    } 
                    return map.containsKey(namespace);
                }

                public boolean loadBean(String name, Class<? extends T> type) {
                    return locator.hasConfiguredPropertyValue(name,
                                                              "transportIds",
                                                              namespace);
                }
            };
        locator.loadBeansOfType(cls, listener);
        return map.get(namespace);
    }
    
    


    private T loadDefaultURIs(final String uri) {
        //First attempt will be to examine the factory class
        //for a DEFAULT_URIS field and use it
        URIBeanLoaderListener listener 
            = new URIBeanLoaderListener(uri) {

                public boolean loadBean(String name, Class<? extends T> type) {
                    try {
                        Field f = type.getField("DEFAULT_URIS");
                        Object o = f.get(null);
                        if (o instanceof Collection) {
                            Collection<String> c = CastUtils.cast((Collection<?>)o);
                            return hasPrefix(uri, c);
                        }
                    } catch (Exception ex) {
                        //ignore
                    }
                    return false;
                }
            };                
        locator.loadBeansOfType(cls, listener);
        return listener.getFactory();
    }
    
    abstract class URIBeanLoaderListener implements ConfiguredBeanLocator.BeanLoaderListener<T> {
        T factory;
        String uri;
        
        URIBeanLoaderListener(String u) {
            uri = u;
        }
        
        public T getFactory() {
            return factory;
        }
        public boolean beanLoaded(String name, T bean) {
            registerBean(bean);
            if (hasPrefix(uri, getPrefixes(bean))) {
                factory = bean;
                return true;
            }
            return false;
        }
        
    }
    
    private T loadDefaultNamespace(final String namespace) {
        //First attempt will be to examine the factory class
        //for a DEFAULT_NAMESPACES field and use it
        ConfiguredBeanLocator.BeanLoaderListener<T> listener 
            = new ConfiguredBeanLocator.BeanLoaderListener<T>() {
                public boolean beanLoaded(String name, T bean) {
                    loaded.add(name);
                    return map.containsKey(namespace);
                }

                public boolean loadBean(String name, Class<? extends T> type) {
                    if (loaded.contains(name)) {
                        return false;
                    }
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
                    return false;
                }
            };                
        locator.loadBeansOfType(cls, listener);
        
        return map.get(namespace);
    }
    private T loadNoDefaultNamespace(final String namespace) {
        //Second attempt will be to examine the factory class
        //for a DEFAULT_NAMESPACES field and if it doesn't exist, try 
        //loading.  This will then load most of the "older" things
        ConfiguredBeanLocator.BeanLoaderListener<T> listener 
            = new ConfiguredBeanLocator.BeanLoaderListener<T>() {
                public boolean beanLoaded(String name, T bean) {
                    loaded.add(name);
                    registerBean(bean);
                    return map.containsKey(namespace);
                }

                public boolean loadBean(String name, Class<? extends T> type) {
                    if (loaded.contains(name)) {
                        return false;
                    }
                    try {
                        type.getField("DEFAULT_NAMESPACES");
                        return false;
                    } catch (Exception ex) {
                        //ignore
                    }
                    return true;
                }
            };                
        locator.loadBeansOfType(cls, listener);
        
        return map.get(namespace);
    }


}
