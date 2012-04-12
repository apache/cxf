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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.configuration.spring.BusWiringType;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class BusDefinitionParser extends AbstractBeanDefinitionParser {
    private static AtomicInteger counter = new AtomicInteger(0);

    public BusDefinitionParser() {
        super();
        setBeanClass(BusConfig.class);
    }

    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        String bus = element.getAttribute("bus");        
        if (StringUtils.isEmpty(bus)) {
            bus = element.getAttribute("name");
            if (StringUtils.isEmpty(bus)) {
                element.setAttribute("bus", bus);
            }
        }
        element.removeAttribute("name");
        if (StringUtils.isEmpty(bus)) {
            bus = "cxf";
        }
        String id = element.getAttribute("id");
        if (!StringUtils.isEmpty(id)) {
            bean.addPropertyValue("id", id);
        }

        super.doParse(element, ctx, bean);
        
        if (ctx.getRegistry().containsBeanDefinition(bus)) {
            BeanDefinition def = ctx.getRegistry().getBeanDefinition(bus);
            copyProps(bean, def);
            bean.addConstructorArgValue(bus);
        } else if (!"cxf".equals(bus)) {
            bean.getRawBeanDefinition().setBeanClass(SpringBus.class);
            bean.getRawBeanDefinition().getPropertyValues().removePropertyValue("bus");
            bean.setDestroyMethodName("shutdown");
            element.setUserData("ID", bus, null);
        } else {
            addBusWiringAttribute(bean, BusWiringType.PROPERTY, bus, ctx);
            bean.getRawBeanDefinition().setAttribute(WIRE_BUS_CREATE, 
                                                     resolveId(element, null, ctx));
            bean.addConstructorArgValue(bus);
        }
    }
    protected boolean processBusAttribute(Element element, ParserContext ctx, 
                                          BeanDefinitionBuilder bean,
                                          String val) {
        return false;
    }
    private void copyProps(BeanDefinitionBuilder src, BeanDefinition def) {
        for (PropertyValue v : src.getBeanDefinition().getPropertyValues().getPropertyValues()) {
            if (!"bus".equals(v.getName())) {
                def.getPropertyValues().addPropertyValue(v.getName(), v.getValue());
            }
            src.getBeanDefinition().getPropertyValues().removePropertyValue(v);
        }
        
    }

    @Override
    protected void mapElement(ParserContext ctx, 
                              BeanDefinitionBuilder bean, 
                              Element e, 
                              String name) {
        if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
            || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)
            || "features".equals(name)) {
            List list = ctx.getDelegate().parseListElement(e, bean.getBeanDefinition());
            bean.addPropertyValue(name, list);
        } else if ("properties".equals(name)) {
            Map map = ctx.getDelegate().parseMapElement(e, bean.getBeanDefinition());
            bean.addPropertyValue("properties", map);
        }
    }
    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, 
                               ParserContext ctx) {
        String bus = (String)element.getUserData("ID");
        if (bus == null) {
            bus = element.getAttribute("bus");        
            if (StringUtils.isEmpty(bus)) {
                bus = element.getAttribute("name");
            }
            if (StringUtils.isEmpty(bus)) {
                bus = Bus.DEFAULT_BUS_ID + ".config" + counter.getAndIncrement();
            } else {
                bus = bus + ".config";
            }
            element.setUserData("ID", bus, null);
        }
        return bus;
    }
    
    @NoJSR250Annotations
    public static class BusConfig extends AbstractBasicInterceptorProvider
        implements ApplicationContextAware {
        
        CXFBusImpl bus;
        String busName;
        String id;
        Collection<AbstractFeature> features;
        Map<String, Object> properties;
        
        public BusConfig(String busName) {
            this.busName = busName;
        }
        
        public void setBus(Bus bb) {
            if (bus == bb) {
                return;
            }
            CXFBusImpl b = (CXFBusImpl)bb;
            if (properties != null) {
                b.setProperties(properties);
                properties = null;
            }
            if (!getInInterceptors().isEmpty()) {
                b.getInInterceptors().addAll(getInInterceptors());
            }
            if (!getOutInterceptors().isEmpty()) {
                b.getOutInterceptors().addAll(getOutInterceptors());
            }
            if (!getInFaultInterceptors().isEmpty()) {
                b.getInFaultInterceptors().addAll(getInFaultInterceptors());
            }
            if (!getOutFaultInterceptors().isEmpty()) {
                b.getOutFaultInterceptors().addAll(getOutFaultInterceptors());
            }
            if (!StringUtils.isEmpty(id)) {
                b.setId(id);
            }
            if (features != null) {
                b.setFeatures(features);
                features = null;
            }
            bus = b;
        }

        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            if (bus != null) {
                return;
            }
            /*
            bus = (CXFBusImpl)BusWiringBeanFactoryPostProcessor
                    .addBus(applicationContext, busName);
                    */                
        }
        
        public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
            if (bus != null) {
                return bus.getOutFaultInterceptors();
            }
            return super.getOutFaultInterceptors();
        }

        public List<Interceptor<? extends Message>> getInFaultInterceptors() {
            if (bus != null) {
                return bus.getInFaultInterceptors();
            }
            return super.getInFaultInterceptors();
        }

        public List<Interceptor<? extends Message>> getInInterceptors() {
            if (bus != null) {
                return bus.getInInterceptors();
            }
            return super.getInInterceptors();
        }

        public List<Interceptor<? extends Message>> getOutInterceptors() {
            if (bus != null) {
                return bus.getOutInterceptors();
            }
            return super.getOutInterceptors();
        }

        public void setInInterceptors(List<Interceptor<? extends Message>> interceptors) {
            if (bus != null) {
                bus.getInInterceptors().addAll(interceptors);
            } else {
                super.setInInterceptors(interceptors);
            }
        }

        public void setInFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
            if (bus != null) {
                bus.getInFaultInterceptors().addAll(interceptors);
            } else {
                super.setInFaultInterceptors(interceptors);
            }
        }

        public void setOutInterceptors(List<Interceptor<? extends Message>> interceptors) {
            if (bus != null) {
                bus.getOutInterceptors().addAll(interceptors);
            } else {
                super.setOutInterceptors(interceptors);
            }
        }

        public void setOutFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
            if (bus != null) {
                bus.setOutFaultInterceptors(interceptors);
            } else {
                super.setOutFaultInterceptors(interceptors);
            }
        }
        
        public Collection<AbstractFeature> getFeatures() {
            if (bus != null) {
                return bus.getFeatures();
            }
            return features;
        }

        public void setFeatures(Collection<AbstractFeature> features) {
            if (bus != null) {
                bus.setFeatures(features);
            } else {
                this.features = features;
            }
            
        }
        
        public Map<String, Object> getProperties() {
            if (bus != null) {
                return bus.getProperties();
            } 
            return properties;
        }
        public void setProperties(Map<String, Object> s) {
            if (bus != null) {
                bus.setProperties(s);
            } else {
                this.properties = s;
            }
        }
        
        public void setId(String s) {
            id = s;
        }


    }
}
