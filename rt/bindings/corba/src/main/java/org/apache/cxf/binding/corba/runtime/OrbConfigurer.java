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
package org.apache.cxf.binding.corba.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.corba.CorbaBindingFactory;

public class OrbConfigurer  {

    private CorbaBindingFactory factory;
    private List<String> orbArgs = new ArrayList<>();
    private Properties orbProperties = new Properties();

    public OrbConfigurer() {
    }

    public void setOrbArgs(List<String> args) {
        orbArgs = args;
    }

    public List<String> getOrbArgs() {
        return orbArgs;
    }

    public Properties getOrbProperties() {
        return orbProperties;
    }

    public void setOrbProperties(Properties props) {
        orbProperties = props;
    }

    public void setFactory(CorbaBindingFactory cFactory) {
        this.factory = cFactory;
    }

    public CorbaBindingFactory getFactory() {
        return factory;
    }

    @Resource
    public void setBus(Bus b) {
        if (factory == null) {
            factory = b.getExtension(CorbaBindingFactory.class);
        }
    }

    @PostConstruct
    public void register() {
        if (factory != null) {
            factory.getOrbConfig().setOrbArgs(orbArgs);
            factory.getOrbConfig().setOrbProperties(orbProperties);
        }
    }
}
