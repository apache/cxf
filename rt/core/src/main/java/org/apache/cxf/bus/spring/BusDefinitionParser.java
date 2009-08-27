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

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.configuration.spring.BusWiringType;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.Interceptor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;

public class BusDefinitionParser extends AbstractBeanDefinitionParser {

    public BusDefinitionParser() {
        super();
        setBeanClass(BusConfig.class);
    }
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        String bus = element.getAttribute("bus");
        if (StringUtils.isEmpty(bus)) {
            addBusWiringAttribute(bean, BusWiringType.CONSTRUCTOR);
        } else {
            bean.addConstructorArgReference(bus);
        }
        super.doParse(element, ctx, bean);
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
    
    protected String getIdOrName(Element elem) {
        String id = super.getIdOrName(elem); 
        if (StringUtils.isEmpty(id)) {
            id = Bus.DEFAULT_BUS_ID + ".config";
        }
        return id;
    }
    @NoJSR250Annotations
    public static class BusConfig  {
        CXFBusImpl bus;
        
        public BusConfig(Bus b) {
            bus = (CXFBusImpl)b;
        }
        public List<Interceptor> getOutFaultInterceptors() {
            return bus.getOutFaultInterceptors();
        }

        public List<Interceptor> getInFaultInterceptors() {
            return bus.getInFaultInterceptors();
        }

        public List<Interceptor> getInInterceptors() {
            return bus.getInInterceptors();
        }

        public List<Interceptor> getOutInterceptors() {
            return bus.getOutInterceptors();
        }

        public void setInInterceptors(List<Interceptor> interceptors) {
            bus.setInInterceptors(interceptors);
        }

        public void setInFaultInterceptors(List<Interceptor> interceptors) {
            bus.setInFaultInterceptors(interceptors);
        }

        public void setOutInterceptors(List<Interceptor> interceptors) {
            bus.setOutInterceptors(interceptors);
        }

        public void setOutFaultInterceptors(List<Interceptor> interceptors) {
            bus.setOutFaultInterceptors(interceptors);
        }
        
        public Collection<AbstractFeature> getFeatures() {
            return bus.getFeatures();
        }

        public void setFeatures(Collection<AbstractFeature> features) {
            bus.setFeatures(features);
        }
        
        public Map<String, Object> getProperties() {
            return bus.getProperties();
        }
        public void setProperties(Map<String, Object> s) {
            bus.setProperties(s);
        }

    }
}
