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

package org.apache.cxf.binding;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.configuration.spring.MapProvider;

public final class BindingFactoryManagerImpl implements BindingFactoryManager {
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(BindingFactoryManagerImpl.class);
    
    Map<String, BindingFactory> bindingFactories; 
    Bus bus;
     
    public BindingFactoryManagerImpl() throws BusException {
        bindingFactories = new ConcurrentHashMap<String, BindingFactory>();
    }
    
    public BindingFactoryManagerImpl(Map<String, BindingFactory> bindingFactories) {
        super();
        if (!(bindingFactories instanceof ConcurrentHashMap)) {
            bindingFactories = new ConcurrentHashMap<String, BindingFactory>(bindingFactories);
        }
        this.bindingFactories = bindingFactories;
    }
    public BindingFactoryManagerImpl(MapProvider<String, BindingFactory> bindingFactories) {
        super();
        this.bindingFactories = bindingFactories.createMap();
    }

    /**
     * Spring configuration via constructor is slow.
     * This accessor allows initialization via a property.
     * @param bindingFactoriesMapProvider
     */
    public void setMapProvider(MapProvider<String, BindingFactory> bindingFactoriesMapProvider) {
        this.bindingFactories = bindingFactoriesMapProvider.createMap();
    }

    @Resource
    public void setBus(Bus b) {
        bus = b;
    }
    
    @PostConstruct
    public void register() {
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
    
    public BindingFactory getBindingFactory(String namespace) throws BusException {
        BindingFactory factory = bindingFactories.get(namespace);
        if (null == factory) {
            throw new BusException(new Message("NO_BINDING_FACTORY_EXC", BUNDLE, namespace));
        }
        return factory;
    }
    
}
