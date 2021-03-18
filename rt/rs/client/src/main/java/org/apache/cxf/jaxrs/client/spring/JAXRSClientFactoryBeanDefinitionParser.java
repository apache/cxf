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
package org.apache.cxf.jaxrs.client.spring;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.common.util.ClasspathScanner;
import org.apache.cxf.configuration.spring.AbstractFactoryBeanDefinitionParser;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;





public class JAXRSClientFactoryBeanDefinitionParser extends AbstractFactoryBeanDefinitionParser {

    public JAXRSClientFactoryBeanDefinitionParser() {
        super();
        setBeanClass(Object.class);
    }

    @Override
    protected Class<?> getFactoryClass() {
        return JAXRSSpringClientFactoryBean.class;
    }

    @Override
    protected String getFactoryIdSuffix() {
        return ".proxyFactory";
    }

    @Override
    protected String getSuffix() {
        return ".jaxrs-client";
    }

    @Override
    protected void mapAttribute(BeanDefinitionBuilder bean, Element e, String name, String val) {
        if ("serviceName".equals(name)) {
            QName q = parseQName(e, val);
            bean.addPropertyValue(name, q);
        } else if ("basePackages".equals(name)) {
            bean.addPropertyValue("basePackages", ClasspathScanner.parsePackages(val));
        } else {
            mapToProperty(bean, name, val);
        }
    }

    @Override
    protected void mapElement(ParserContext ctx, BeanDefinitionBuilder bean, Element el, String name) {
        if ("properties".equals(name) || "headers".equals(name)) {
            Map<?, ?> map = ctx.getDelegate().parseMapElement(el, bean.getBeanDefinition());
            bean.addPropertyValue(name, map);
        } else if ("executor".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "serviceFactory.executor");
        } else if ("binding".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "bindingConfig");
        } else if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
            || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)) {
            List<?> list = ctx.getDelegate().parseListElement(el, bean.getBeanDefinition());
            bean.addPropertyValue(name, list);
        } else if ("features".equals(name) || "providers".equals(name)
                   || "schemaLocations".equals(name) || "modelBeans".equals(name)) {
            List<?> list = ctx.getDelegate().parseListElement(el, bean.getBeanDefinition());
            bean.addPropertyValue(name, list);
        } else if ("model".equals(name)) {
            List<UserResource> resources = ResourceUtils.getResourcesFromElement(el);
            bean.addPropertyValue("modelBeans", resources);
        } else {
            setFirstChildAsProperty(el, ctx, bean, name);
        }
    }

    public static class JAXRSSpringClientFactoryBean extends JAXRSClientFactoryBean
        implements ApplicationContextAware {

        private List<String> basePackages;

        public JAXRSSpringClientFactoryBean() {
            super();
        }

        public void setBasePackages(List<String> basePackages) {
            this.basePackages = basePackages;
        }

        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            try {
                if (basePackages != null) {
                    final Map< Class< ? extends Annotation >, Collection< Class< ? > > > classes =
                        ClasspathScanner.findClasses(basePackages, Path.class, Provider.class);

                    if (classes.get(Path.class).size() > 1) {
                        throw new NoUniqueBeanDefinitionException(Path.class, classes.get(Path.class).size(),
                            "More than one service class (@Path) has been discovered");
                    }
                    AutowireCapableBeanFactory beanFactory = ctx.getAutowireCapableBeanFactory();
                    for (final Class< ? > providerClass: classes.get(Provider.class)) {
                        Object bean;
                        try {
                            bean = beanFactory.createBean(providerClass,
                                                   AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
                        } catch (Exception ex) {
                            bean = beanFactory.createBean(providerClass);
                        }
                        setProvider(bean);
                    }

                    for (final Class< ? > serviceClass: classes.get(Path.class)) {
                        setServiceClass(serviceClass);
                    }
                }
            } catch (IOException ex) {
                throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
            } catch (ClassNotFoundException ex) {
                throw new BeanCreationException("Failed to create bean from classfile", ex);
            }

            if (bus == null) {
                setBus(BusWiringBeanFactoryPostProcessor.addDefaultBus(ctx));
            }
        }
    }

    static Class<?> getServiceClass(Collection<Class<?>> rootClasses) {
        for (Class<?> cls : rootClasses) {
            if (cls.isInterface()) {
                return cls;
            }
        }
        return rootClasses.iterator().next();
    }
    static List<Object> getProviders(ApplicationContext context, Collection<Class<?>> providerClasses) {
        List<Object> providers = new LinkedList<>();
        AutowireCapableBeanFactory beanFactory = context.getAutowireCapableBeanFactory();
        for (final Class< ? > providerClass: providerClasses) {
            Object bean;
            try {
                bean = beanFactory.createBean(providerClass,
                                       AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
            } catch (Exception ex) {
                bean = beanFactory.createBean(providerClass);
            }
            providers.add(bean);
        }
        return providers;
    }
}
