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
package org.apache.cxf.jaxws.spring;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.bus.spring.Jsr250BeanPostProcessor;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.configuration.spring.BusWiringType;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.spi.ProviderImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


public class EndpointDefinitionParser extends AbstractBeanDefinitionParser {
    private static final Class<?> EP_CLASS;
    static {
        Class<?> cls = SpringEndpointImpl.class;
        try {
            if (ProviderImpl.isJaxWs22()) {
                cls = ClassLoaderUtils
                    .loadClass("org.apache.cxf.jaxws22.spring.JAXWS22SpringEndpointImpl", 
                           EndpointDefinitionParser.class);
            }
        } catch (Throwable t) {
            cls = SpringEndpointImpl.class;
        }
        EP_CLASS = cls;
    }
    
    
    private static final String IMPLEMENTOR = "implementor";

    public EndpointDefinitionParser() {
        super();
        setBeanClass(EP_CLASS);
    }

    @Override
    protected String getSuffix() {
        return ".jaxws-endpoint";
    }

    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        boolean isAbstract = false;
        boolean publish = true;
        NamedNodeMap atts = element.getAttributes();
        String bus = element.getAttribute("bus");
        if (StringUtils.isEmpty(bus)) {
            addBusWiringAttribute(bean, BusWiringType.CONSTRUCTOR);
        } else {
            bean.addConstructorArgReference(bus);
        }
        for (int i = 0; i < atts.getLength(); i++) {
            Attr node = (Attr) atts.item(i);
            String val = node.getValue();
            String pre = node.getPrefix();
            String name = node.getLocalName();

            if ("createdFromAPI".equals(name)) {
                bean.setAbstract(true);
                isAbstract = true;
            } else if (isAttribute(pre, name) && !"publish".equals(name) && !"bus".equals(name)) {
                if ("endpointName".equals(name) || "serviceName".equals(name)) {
                    QName q = parseQName(element, val);
                    bean.addPropertyValue(name, q);
                } else if ("depends-on".equals(name)) {
                    bean.addDependsOn(val);
                } else if (IMPLEMENTOR.equals(name)) {
                    loadImplementor(bean, val);
                } else if (!"name".equals(name)) {
                    mapToProperty(bean, name, val);
                }
            } else if ("abstract".equals(name)) {
                bean.setAbstract(true);
                isAbstract = true;
            } else if ("publish".equals(name)) {
                publish = "true".equals(val);
            }
        }
        
        Element elem = DOMUtils.getFirstElement(element);
        while (elem != null) {
            String name = elem.getLocalName();
            if ("properties".equals(name)) {
                Map map = ctx.getDelegate().parseMapElement(elem, bean.getBeanDefinition());
                bean.addPropertyValue("properties", map);
            } else if ("binding".equals(name)) {
                setFirstChildAsProperty(elem, ctx, bean, "bindingConfig");
            } else if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
                || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)
                || "features".equals(name) || "schemaLocations".equals(name)
                || "handlers".equals(name)) {
                List list = ctx.getDelegate().parseListElement(elem, bean.getBeanDefinition());
                bean.addPropertyValue(name, list);
            } else if (IMPLEMENTOR.equals(name)) {
                ctx.getDelegate()
                    .parseConstructorArgElement(elem, bean.getBeanDefinition());
            } else {
                setFirstChildAsProperty(elem, ctx, bean, name);
            }
            elem = DOMUtils.getNextElement(elem);
        }
        if (!isAbstract) {
            if (publish) {
                bean.setInitMethodName("publish");
            }
            bean.setDestroyMethodName("stop");
        }
        // We don't want to delay the registration of our Server
        bean.setLazyInit(false);
    }

    private void loadImplementor(BeanDefinitionBuilder bean, String val) {
        if (!StringUtils.isEmpty(val)) {
            bean.addPropertyValue("checkBlockConstruct", Boolean.TRUE);
            if (val.startsWith("#")) {
                bean.addConstructorArgReference(val.substring(1));
            } else {
                bean.addConstructorArgValue(BeanDefinitionBuilder
                                            .genericBeanDefinition(val).getBeanDefinition());
            }
        }
    }
    @Override
    protected String resolveId(Element elem, 
                               AbstractBeanDefinition definition, 
                               ParserContext ctx) 
        throws BeanDefinitionStoreException {
        String id = super.resolveId(elem, definition, ctx);
        if (StringUtils.isEmpty(id)) {
            id = EndpointImpl.class.getName() + "--" + definition.hashCode();
        }
        
        return id;
    }
    
    public static final void setBlocking(ApplicationContext ctx, EndpointImpl impl) {
        try {
            Class<?> cls = Class
                .forName("org.springframework.context.annotation.CommonAnnotationBeanPostProcessor");
            if (ctx.getBeanNamesForType(cls, true, false).length != 0) {
                //Spring will handle the postconstruct, but won't inject the 
                // WebServiceContext so we do need to do that.
                impl.getServerFactory().setBlockPostConstruct(true);
            } else if (ctx.containsBean(Jsr250BeanPostProcessor.class.getName())) {
                impl.getServerFactory().setBlockInjection(true);
            }
        } catch (ClassNotFoundException e) {
            //ignore
        }

    }
    
    @NoJSR250Annotations
    public static class SpringEndpointImpl extends EndpointImpl
        implements ApplicationContextAware {
    
        boolean checkBlockConstruct;
        
        public SpringEndpointImpl(Object o) {
            super(o instanceof Bus ? (Bus)o : null,
                o instanceof Bus ? null : o);
        }

        public SpringEndpointImpl(Bus bus, Object implementor) {
            super(bus, implementor);
        }
        
        public void setCheckBlockConstruct(Boolean b) {
            checkBlockConstruct = b;
        }
        
        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            if (checkBlockConstruct) {
                setBlocking(ctx, this);
            }
            if (getBus() == null) {
                setBus(BusWiringBeanFactoryPostProcessor.addDefaultBus(ctx));
            }
        }
    }

}
