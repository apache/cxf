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
package org.apache.cxf.frontend.spring;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.configuration.spring.AbstractFactoryBeanDefinitionParser;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ClientProxyFactoryBeanDefinitionParser 
    extends AbstractFactoryBeanDefinitionParser {

    public ClientProxyFactoryBeanDefinitionParser() {
        super();
        setBeanClass(SpringClientProxyFactoryBean.class);
    }
    @Override
    protected Class getFactoryClass() {
        return SpringClientProxyFactoryBean.class;
    }
    protected Class getRawFactoryClass() {
        return ClientProxyFactoryBean.class;
    }

    @Override
    protected String getFactoryIdSuffix() {
        return ".proxyFactory";
    }

    @Override
    protected String getSuffix() {
        return ".simple-client";
    }

    @Override
    protected void mapAttribute(BeanDefinitionBuilder bean, Element e, String name, String val) {
        if ("endpointName".equals(name) || "serviceName".equals(name)) {
            QName q = parseQName(e, val);
            bean.addPropertyValue(name, q);
        } else {
            mapToProperty(bean, name, val);
        }
    }

    @Override
    protected void mapElement(ParserContext ctx, BeanDefinitionBuilder bean, Element e, String name) {
        if ("properties".equals(name)) {
            Map map = ctx.getDelegate().parseMapElement(e, bean.getBeanDefinition());
            bean.addPropertyValue("properties", map);
        } else if ("binding".equals(name)) {
            setFirstChildAsProperty(e, ctx, bean, "bindingConfig");
        } else if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
            || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)
            || "features".equals(name) || "handlers".equals(name)) {
            List list = ctx.getDelegate().parseListElement(e, bean.getBeanDefinition());
            bean.addPropertyValue(name, list);
        } else {
            setFirstChildAsProperty(e, ctx, bean, name);
        }
    }
    
    public static class SpringClientProxyFactoryBean extends ClientProxyFactoryBean
        implements ApplicationContextAware, FactoryBean {

        public SpringClientProxyFactoryBean() {
            super();
        }
        public SpringClientProxyFactoryBean(ClientFactoryBean fact) {
            super(fact);
        }
        
        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            if (getBus() == null) {
                Bus bus = BusFactory.getThreadDefaultBus();
                BusWiringBeanFactoryPostProcessor.updateBusReferencesInContext(bus, ctx);
                setBus(bus);
            }
        }
        public Object getObject() throws Exception {
            return create();
        }
        public Class getObjectType() {
            return this.getServiceClass();
        }
        public boolean isSingleton() {
            return false;
        }
    }
}
