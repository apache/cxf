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

package org.apache.cxf.bus.spring;

import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.spring.ConfigurerImpl;
import org.apache.cxf.resource.ResourceManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * 
 */
public class SpringBus extends ExtensionManagerBus 
    implements ApplicationContextAware, ApplicationListener {

    AbstractApplicationContext ctx;

    
    public SpringBus() {
    }
    
    /** {@inheritDoc}*/
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = (AbstractApplicationContext)applicationContext;
        ctx.addApplicationListener(this);
        ApplicationContext ac = applicationContext.getParent();
        while (ac != null) {
            if (ac instanceof AbstractApplicationContext) {
                ((AbstractApplicationContext)ac).addApplicationListener(this);
            }
            ac = ac.getParent();
        }
        
        // set the classLoader extension with the application context classLoader
        setExtension(applicationContext.getClassLoader(), ClassLoader.class);
        
        setExtension(new ConfigurerImpl(applicationContext), Configurer.class);
        
        ResourceManager m = getExtension(ResourceManager.class);
        m.addResourceResolver(new BusApplicationContextResourceResolver(applicationContext));
        
        setExtension(applicationContext, ApplicationContext.class);
        ConfiguredBeanLocator loc = getExtension(ConfiguredBeanLocator.class);
        if (!(loc instanceof SpringBeanLocator)) {
            setExtension(new SpringBeanLocator(applicationContext, this), ConfiguredBeanLocator.class);
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (ctx == null) {
            return;
        }
        boolean doIt = false;
        ApplicationContext ac = ctx;
        while (ac != null && !doIt) {
            if (event.getSource() == ac) {
                doIt = true;
                break;
            }
            ac = ac.getParent();
        }
        
        if (doIt) {
            if (event instanceof ContextRefreshedEvent) {
                initialize();
            } else if (event instanceof ContextClosedEvent) {
                getExtension(BusLifeCycleManager.class).postShutdown();
            }
        }
    }
    
    public void destroyBeans() {
        ctx.close();
        super.destroyBeans();
    }
    
    public String getId() {
        if (id == null) {
            try {
                Class<?> clsbc = Class.forName("org.osgi.framework.BundleContext");
                Class<?> clsb = Class.forName("org.osgi.framework.Bundle");
                Object o = getExtension(clsbc);
                Object o2 = clsbc.getMethod("getBundle").invoke(o);
                String s = (String)clsb.getMethod("getSymbolicName").invoke(o2);
                id = s + "-" + DEFAULT_BUS_ID + Integer.toString(this.hashCode());
            } catch (Throwable t) {
                id = super.getId();
            }
        }
        return id;
    }

}
