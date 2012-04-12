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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.NullConfigurer;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.configuration.spring.BusWiringType;
import org.apache.cxf.configuration.spring.ConfigurerImpl;
import org.apache.cxf.helpers.CastUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * BeanFactoryPostProcessor that looks for any bean definitions that have the
 * {@link AbstractBeanDefinitionParser#WIRE_BUS_ATTRIBUTE} attribute set. If the attribute has the value
 * {@link BusWiringType#PROPERTY} then it attaches their "bus" property to the bean called "cxf". If the
 * attribute has the value {@link BusWiringType#CONSTRUCTOR} then it shifts any existing indexed constructor
 * arguments one place to the right and adds a reference to "cxf" as the first constructor argument. This
 * processor is intended to operate on beans defined via Spring namespace support which require a reference to
 * the CXF bus.
 */
public class BusWiringBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    Bus bus;
    String busName;
    
    public BusWiringBeanFactoryPostProcessor() {
    }

    public BusWiringBeanFactoryPostProcessor(Bus b) {
        bus = b;
    }
    public BusWiringBeanFactoryPostProcessor(String n) {
        busName = n;
    }
    private static Bus getBusForName(String name,
                                     ApplicationContext context,
                                     boolean create) {
        if (!context.containsBean(name) && (create || Bus.DEFAULT_BUS_ID.equals(name))) {
            SpringBus b = new SpringBus();
            b.setApplicationContext(context);
            ConfigurableApplicationContext cctx = (ConfigurableApplicationContext)context;
            cctx.getBeanFactory().registerSingleton(name, b);
        }
        return (Bus)context.getBean(name, Bus.class);
    }
    private Object getBusForName(String name,
                                 ConfigurableListableBeanFactory factory,
                                 boolean create,
                                 String cn) {
        if (!factory.containsBeanDefinition(name) && (create || Bus.DEFAULT_BUS_ID.equals(name))) {
            DefaultListableBeanFactory df = (DefaultListableBeanFactory)factory;
            RootBeanDefinition rbd = new RootBeanDefinition(SpringBus.class);
            if (cn != null) {
                rbd.setAttribute("busConfig", new RuntimeBeanReference(cn));
            }
            df.registerBeanDefinition(name, rbd);
        } else if (cn != null) {
            BeanDefinition bd = factory.getBeanDefinition(name);
            bd.getPropertyValues().addPropertyValue("busConfig", new RuntimeBeanReference(cn));
        }
        return new RuntimeBeanReference(name);        
    }
    
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) throws BeansException {
        Object inject = bus;
        if (inject == null) {
            inject = getBusForName(Bus.DEFAULT_BUS_ID, factory, true, null);
        } else {
            if (!factory.containsBeanDefinition(Bus.DEFAULT_BUS_ID)
                && !factory.containsSingleton(Bus.DEFAULT_BUS_ID)) {
                factory.registerSingleton(Bus.DEFAULT_BUS_ID, bus);
            }
        }
        for (String beanName : factory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = factory.getBeanDefinition(beanName);
            BusWiringType type 
                = (BusWiringType)beanDefinition.getAttribute(AbstractBeanDefinitionParser.WIRE_BUS_ATTRIBUTE);
            if (type == null) {
                continue;
            }
            String busname = (String)beanDefinition.getAttribute(AbstractBeanDefinitionParser.WIRE_BUS_NAME);
            String create = (String)beanDefinition
                .getAttribute(AbstractBeanDefinitionParser.WIRE_BUS_CREATE);
            Object inj = inject;
            if (busname != null) {
                if (bus != null) {
                    continue;
                }
                inj = getBusForName(busname, factory, create != null, create);
            }
            beanDefinition.removeAttribute(AbstractBeanDefinitionParser.WIRE_BUS_NAME);
            beanDefinition.removeAttribute(AbstractBeanDefinitionParser.WIRE_BUS_ATTRIBUTE);
            beanDefinition.removeAttribute(AbstractBeanDefinitionParser.WIRE_BUS_CREATE);
            if (create == null) {
                if (BusWiringType.PROPERTY == type) {
                    beanDefinition.getPropertyValues()
                        .addPropertyValue("bus", inj);
                } else if (BusWiringType.CONSTRUCTOR == type) {
                    ConstructorArgumentValues constructorArgs = beanDefinition.getConstructorArgumentValues();
                    insertConstructorArg(constructorArgs, inj);
                }
            }
        }
    }

    /**
     * Insert the given value as the first constructor argument in the given set. To do this, we clear the
     * argument set, then re-insert all its generic arguments, then re-insert all its indexed arguments with
     * their indices incremented by 1, and finally set the first indexed argument (at index 0) to the given
     * value.
     * 
     * @param constructorArgs the argument definition to modify.
     * @param valueToInsert the value to insert as the first argument.
     */
    private void insertConstructorArg(ConstructorArgumentValues constructorArgs, Object valueToInsert) {
        List<ValueHolder> genericArgs = new ArrayList<ValueHolder>(CastUtils
            .<ValueHolder> cast(constructorArgs.getGenericArgumentValues()));
        Map<Integer, ValueHolder> indexedArgs = new HashMap<Integer, ValueHolder>(CastUtils
            .<Integer, ValueHolder> cast(constructorArgs.getIndexedArgumentValues()));

        constructorArgs.clear();
        for (ValueHolder genericValue : genericArgs) {
            constructorArgs.addGenericArgumentValue(genericValue);
        }
        for (Map.Entry<Integer, ValueHolder> entry : indexedArgs.entrySet()) {
            constructorArgs.addIndexedArgumentValue(entry.getKey() + 1, entry.getValue());
        }
        constructorArgs.addIndexedArgumentValue(0, valueToInsert);
    }
    
    
    /**
     * This is deprecated and is there to support 2.3.x compatibility.
     * Code should be updated to call addDefaultBus(ctx) instead to get the bus
     * associated with the context.
     * @param bus
     * @param ctx
     */
    @Deprecated
    public static void updateBusReferencesInContext(Bus bus, ApplicationContext ctx) {
        Configurer conf = bus.getExtension(Configurer.class);
        if (conf instanceof NullConfigurer) {
            bus.setExtension(new ConfigurerImpl(ctx), Configurer.class);
            conf = bus.getExtension(Configurer.class);
        } else if (conf instanceof ConfigurerImpl) {
            ((ConfigurerImpl)conf).addApplicationContext(ctx);
        }
        if (ctx instanceof ConfigurableApplicationContext) {
            ConfigurableApplicationContext cctx = (ConfigurableApplicationContext)ctx;
            new BusWiringBeanFactoryPostProcessor(bus).postProcessBeanFactory(cctx.getBeanFactory());
        }
    }

    public static Bus addDefaultBus(ApplicationContext ctx) {
        if (!ctx.containsBean(Bus.DEFAULT_BUS_ID)) {
            Bus b = getBusForName(Bus.DEFAULT_BUS_ID, ctx, true);
            if (ctx instanceof ConfigurableApplicationContext) {
                ConfigurableApplicationContext cctx = (ConfigurableApplicationContext)ctx;
                new BusWiringBeanFactoryPostProcessor(b).postProcessBeanFactory(cctx.getBeanFactory());
            }
        }
        return ctx.getBean(Bus.DEFAULT_BUS_ID, Bus.class);
    }
    public static Bus addBus(ApplicationContext ctx, String name) {
        return getBusForName(name, ctx, true);
    }
}
