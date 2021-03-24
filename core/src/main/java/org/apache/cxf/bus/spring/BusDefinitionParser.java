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
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.configuration.spring.BusWiringType;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
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

    @Override
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
            bean.setDestroyMethodName("shutdown");
            try {
                element.setUserData("ID", bus, null);
                bean.getRawBeanDefinition().getPropertyValues().removePropertyValue("bus");
            } catch (Throwable t) {
                //likely not DOM level 3, ignore
            }
        } else {
            addBusWiringAttribute(bean, BusWiringType.PROPERTY, bus, ctx);
            bean.getRawBeanDefinition().setAttribute(WIRE_BUS_CREATE,
                                                     resolveId(element, null, ctx));
            bean.addConstructorArgValue(bus);
        }
    }
    
    @Override
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
            List<?> list = ctx.getDelegate().parseListElement(e, bean.getBeanDefinition());
            bean.addPropertyValue(name, list);
        } else if ("properties".equals(name)) {
            Map<?, ?> map = ctx.getDelegate().parseMapElement(e, bean.getBeanDefinition());
            bean.addPropertyValue("properties", map);
        }
    }
    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition,
                               ParserContext ctx) {
        String bus = null;
        try {
            bus = (String)element.getUserData("ID");
        } catch (Throwable t) {
            //ignore
        }
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
            try {
                element.setUserData("ID", bus, null);
            } catch (Throwable t) {
                //maybe no DOM level 3, ignore, but, may have issues with the counter
            }
        }
        return bus;
    }

    @NoJSR250Annotations
    public static class BusConfig extends AbstractBasicInterceptorProvider
        implements ApplicationContextAware {

        Bus bus;
        String busName;
        String id;
        Collection<Feature> features;
        Map<String, Object> properties;

        public BusConfig(String busName) {
            this.busName = busName;
        }

        public void setBus(Bus bb) {
            if (bus == bb) {
                return;
            }
            if (properties != null) {
                bb.setProperties(properties);
                properties = null;
            }
            if (!getInInterceptors().isEmpty()) {
                bb.getInInterceptors().addAll(getInInterceptors());
            }
            if (!getOutInterceptors().isEmpty()) {
                bb.getOutInterceptors().addAll(getOutInterceptors());
            }
            if (!getInFaultInterceptors().isEmpty()) {
                bb.getInFaultInterceptors().addAll(getInFaultInterceptors());
            }
            if (!getOutFaultInterceptors().isEmpty()) {
                bb.getOutFaultInterceptors().addAll(getOutFaultInterceptors());
            }
            if (!StringUtils.isEmpty(id)) {
                bb.setId(id);
            }
            if (features != null) {
                bb.setFeatures(features);
                features = null;
            }
            bus = bb;
        }

        public void setApplicationContext(ApplicationContext applicationContext) {
            if (bus != null) {
                return;
            }
        }

        @Override
        public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
            if (bus != null) {
                return bus.getOutFaultInterceptors();
            }
            return super.getOutFaultInterceptors();
        }

        @Override
        public List<Interceptor<? extends Message>> getInFaultInterceptors() {
            if (bus != null) {
                return bus.getInFaultInterceptors();
            }
            return super.getInFaultInterceptors();
        }

        @Override
        public List<Interceptor<? extends Message>> getInInterceptors() {
            if (bus != null) {
                return bus.getInInterceptors();
            }
            return super.getInInterceptors();
        }

        @Override
        public List<Interceptor<? extends Message>> getOutInterceptors() {
            if (bus != null) {
                return bus.getOutInterceptors();
            }
            return super.getOutInterceptors();
        }

        @Override
        public void setInInterceptors(List<Interceptor<? extends Message>> interceptors) {
            if (bus != null) {
                bus.getInInterceptors().addAll(interceptors);
            } else {
                super.setInInterceptors(interceptors);
            }
        }

        @Override
        public void setInFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
            if (bus != null) {
                bus.getInFaultInterceptors().addAll(interceptors);
            } else {
                super.setInFaultInterceptors(interceptors);
            }
        }

        @Override
        public void setOutInterceptors(List<Interceptor<? extends Message>> interceptors) {
            if (bus != null) {
                bus.getOutInterceptors().addAll(interceptors);
            } else {
                super.setOutInterceptors(interceptors);
            }
        }

        @Override
        public void setOutFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
            if (bus != null) {
                bus.getOutFaultInterceptors().addAll(interceptors);
            } else {
                super.setOutFaultInterceptors(interceptors);
            }
        }

        public Collection<Feature> getFeatures() {
            if (bus != null) {
                return bus.getFeatures();
            }
            return features;
        }

        public void setFeatures(Collection<? extends Feature> features) {
            if (bus != null) {
                bus.setFeatures(features);
            } else {
                this.features = CastUtils.cast(features);
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
