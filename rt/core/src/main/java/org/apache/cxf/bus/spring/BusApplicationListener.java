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

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
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
public class BusApplicationListener implements ApplicationListener, ApplicationContextAware {
    AbstractApplicationContext ctx;
    
    /** {@inheritDoc}*/
    public void onApplicationEvent(ApplicationEvent event) {
        if (ctx == null) {
            return;
        }
        if (event instanceof ContextRefreshedEvent) {
            Bus bus = (Bus)ctx.getBean("cxf");
            ((CXFBusImpl)bus).initialize();
            BusLifeCycleManager lcm = (BusLifeCycleManager)
                ctx.getBean("org.apache.cxf.buslifecycle.BusLifeCycleManager",
                        BusLifeCycleManager.class);
            lcm.initComplete();
        } else if (event instanceof ContextClosedEvent) {
            BusLifeCycleManager lcm = (BusLifeCycleManager)
                ctx.getBean("org.apache.cxf.buslifecycle.BusLifeCycleManager",
                    BusLifeCycleManager.class);
            lcm.postShutdown();
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof AbstractApplicationContext) {
            ctx = (AbstractApplicationContext)applicationContext;
            ctx.addApplicationListener(this);
        }        
    }

}
