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
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurer;
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
 * 
 * @author Ian Roberts
 */
public class BusWiringBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private static final Logger LOG = LogUtils.getL7dLogger(BusWiringBeanFactoryPostProcessor.class);
    Bus bus;
    
    public BusWiringBeanFactoryPostProcessor() {
    }

    public BusWiringBeanFactoryPostProcessor(Bus b) {
        bus = b;
    }
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) throws BeansException {
        Object inject = bus;
        if (factory.containsBeanDefinition(Bus.DEFAULT_BUS_ID)) {
            inject = new RuntimeBeanReference(Bus.DEFAULT_BUS_ID);
        }
        for (String beanName : factory.getBeanDefinitionNames()) {
            LOG.fine("Checking bean " + beanName);
            BeanDefinition beanDefinition = factory.getBeanDefinition(beanName);
            if (BusWiringType.PROPERTY == beanDefinition
                .getAttribute(AbstractBeanDefinitionParser.WIRE_BUS_ATTRIBUTE)) {
                LOG.fine("Found " + AbstractBeanDefinitionParser.WIRE_BUS_ATTRIBUTE + " attribute "
                         + BusWiringType.PROPERTY + " on bean " + beanName);
                beanDefinition.getPropertyValues()
                    .addPropertyValue("bus", inject);
            } else if (BusWiringType.CONSTRUCTOR == beanDefinition
                .getAttribute(AbstractBeanDefinitionParser.WIRE_BUS_ATTRIBUTE)) {
                LOG.fine("Found " + AbstractBeanDefinitionParser.WIRE_BUS_ATTRIBUTE + " attribute "
                         + BusWiringType.CONSTRUCTOR + " on bean " + beanName);
                ConstructorArgumentValues constructorArgs = beanDefinition.getConstructorArgumentValues();
                insertConstructorArg(constructorArgs, inject);
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
    
    public static void updateBusReferencesInContext(Bus bus, ApplicationContext ctx) {
        Configurer conf = bus.getExtension(Configurer.class);
        if (conf instanceof ConfigurerImpl) {
            ((ConfigurerImpl)conf).addApplicationContext(ctx);
        }
        if (ctx instanceof ConfigurableApplicationContext) {
            ConfigurableApplicationContext cctx = (ConfigurableApplicationContext)ctx;
            new BusWiringBeanFactoryPostProcessor(bus).postProcessBeanFactory(cctx.getBeanFactory());
        }
    }
}
