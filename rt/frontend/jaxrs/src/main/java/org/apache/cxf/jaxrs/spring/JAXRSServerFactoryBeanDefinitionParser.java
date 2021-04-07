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
package org.apache.cxf.jaxrs.spring;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClasspathScanner;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;



public class JAXRSServerFactoryBeanDefinitionParser extends AbstractBeanDefinitionParser {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSServerFactoryBeanDefinitionParser.class);

    public JAXRSServerFactoryBeanDefinitionParser() {
        super();
        setBeanClass(SpringJAXRSServerFactoryBean.class);
    }

    @Override
    protected void mapAttribute(BeanDefinitionBuilder bean, Element e, String name, String val) {
        if ("beanNames".equals(name)) {
            String[] values = val.split(" ");
            List<SpringResourceFactory> tempFactories = new ArrayList<>(values.length);
            for (String v : values) {
                String theValue = v.trim();
                if (theValue.length() > 0) {
                    tempFactories.add(new SpringResourceFactory(theValue));
                }
            }
            bean.addPropertyValue("tempResourceProviders", tempFactories);
        } else if ("serviceName".equals(name)) {
            QName q = parseQName(e, val);
            bean.addPropertyValue(name, q);
        } else if ("basePackages".equals(name)) {
            bean.addPropertyValue("basePackages", ClasspathScanner.parsePackages(val));
        } else if ("serviceAnnotation".equals(name)) {
            bean.addPropertyValue("serviceAnnotation", val);
        } else if ("publish".equals(name)) {
            mapToProperty(bean, "start", val);
        } else {
            mapToProperty(bean, name, val);
        }
    }

    @Override
    protected void mapElement(ParserContext ctx, BeanDefinitionBuilder bean, Element el, String name) {
        if ("properties".equals(name)
            || "extensionMappings".equals(name)
            || "languageMappings".equals(name)) {
            Map<?, ?> map = ctx.getDelegate().parseMapElement(el, bean.getBeanDefinition());
            bean.addPropertyValue(name, map);
        } else if ("executor".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "serviceFactory.executor");
        } else if ("invoker".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "serviceFactory.invoker");
        } else if ("binding".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "bindingConfig");
        } else if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
            || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)) {
            List<?> list = ctx.getDelegate().parseListElement(el, bean.getBeanDefinition());
            bean.addPropertyValue(name, list);
        } else if ("features".equals(name) || "schemaLocations".equals(name)
            || "providers".equals(name) || "serviceBeans".equals(name)
            || "modelBeans".equals(name)) {
            List<?> list = ctx.getDelegate().parseListElement(el, bean.getBeanDefinition());
            bean.addPropertyValue(name, list);
        }  else if ("serviceFactories".equals(name)) {
            List<?> list = ctx.getDelegate().parseListElement(el, bean.getBeanDefinition());
            bean.addPropertyValue("resourceProviders", list);
        } else if ("model".equals(name)) {
            List<UserResource> resources = ResourceUtils.getResourcesFromElement(el);
            bean.addPropertyValue("modelBeans", resources);
        } else {
            setFirstChildAsProperty(el, ctx, bean, name);
        }
    }


    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        super.doParse(element, ctx, bean);

        bean.setInitMethodName("create");
        bean.setDestroyMethodName("destroy");

        // We don't really want to delay the registration of our Server
        bean.setLazyInit(false);
    }

    @Override
    protected String resolveId(Element elem,
                               AbstractBeanDefinition definition,
                               ParserContext ctx)
        throws BeanDefinitionStoreException {
        String id = super.resolveId(elem, definition, ctx);
        if (StringUtils.isEmpty(id)) {
            id = getBeanClass().getName() + "--" + definition.hashCode();
        }

        return id;
    }

    @Override
    protected boolean hasBusProperty() {
        return true;
    }

    public static class SpringJAXRSServerFactoryBean extends JAXRSServerFactoryBean implements
        ApplicationContextAware {

        private List<SpringResourceFactory> tempFactories;
        private List<String> basePackages;
        private String serviceAnnotation;
        private ApplicationContext context;
        private boolean serviceBeansAvailable;
        private boolean providerBeansAvailable;
        private boolean resourceProvidersAvailable;

        public SpringJAXRSServerFactoryBean() {
            super();
        }

        public SpringJAXRSServerFactoryBean(JAXRSServiceFactoryBean sf) {
            super(sf);
        }

        public void destroy() {
            Server server = super.getServer();
            if (server != null && server.isStarted()) {
                server.destroy();
            }
        }
        @Override
        public void setServiceBeans(List<Object> beans) {
            super.setServiceBeans(beans);
            this.serviceBeansAvailable = true;
        }
        @Override
        public void setProviders(List<? extends Object> beans) {
            super.setProviders(beans);
            this.providerBeansAvailable = true;
        }
        public void setResourceProviders(List<ResourceProvider> rps) {
            super.setResourceProviders(rps);
            this.resourceProvidersAvailable = true;
        }
        public void setBasePackages(List<String> basePackages) {
            this.basePackages = basePackages;
        }

        public void setServiceAnnotation(String serviceAnnotation) {
            this.serviceAnnotation = serviceAnnotation;
        }

        public void setTempResourceProviders(List<SpringResourceFactory> providers) {
            tempFactories = providers;
        }

        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            this.context = ctx;

            if (tempFactories != null) {
                List<ResourceProvider> factories = new ArrayList<>(
                    tempFactories.size());
                for (int i = 0; i < tempFactories.size(); i++) {
                    SpringResourceFactory factory = tempFactories.get(i);
                    factory.setApplicationContext(ctx);
                    factories.add(factory);
                }
                tempFactories.clear();
                setResourceProviders(factories);
            }
            Class<? extends Annotation> serviceAnnotationClass = loadServiceAnnotationClass();
            if (basePackages != null) {
                try {
                    final Map< Class< ? extends Annotation >, Collection< Class< ? > > > classes =
                        ClasspathScanner.findClasses(basePackages, Provider.class, Path.class);

                    this.setServiceBeans(createBeansFromDiscoveredClasses(context,
                                                                          classes.get(Path.class),
                                                                          serviceAnnotationClass));
                    this.setProviders(createBeansFromDiscoveredClasses(context,
                                                                       classes.get(Provider.class),
                                                                       serviceAnnotationClass));
                } catch (IOException ex) {
                    throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
                } catch (ClassNotFoundException ex) {
                    throw new BeanCreationException("Failed to create bean from classfile", ex);
                }
            } else if (serviceAnnotationClass != null
                && !serviceBeansAvailable && !providerBeansAvailable && !resourceProvidersAvailable) {
                discoverContextResources(serviceAnnotationClass);
            }
            if (bus == null) {
                setBus(BusWiringBeanFactoryPostProcessor.addDefaultBus(ctx));
            }
        }
        private void discoverContextResources(Class<? extends Annotation> serviceAnnotationClass) {
            AbstractSpringComponentScanServer scanServer =
                new AbstractSpringComponentScanServer(serviceAnnotationClass) { };
            scanServer.setApplicationContext(context);
            scanServer.setJaxrsResources(this);
        }
        @SuppressWarnings("unchecked")
        private Class<? extends Annotation> loadServiceAnnotationClass() {
            if (serviceAnnotation != null) {
                try {
                    return (Class<? extends Annotation>)ClassLoaderUtils.loadClass(serviceAnnotation, this.getClass());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            return null;
        }
    }
    static List<Object> createBeansFromDiscoveredClasses(ApplicationContext context,
                                                         Collection<Class<?>> classes,
                                                         Class<? extends Annotation> serviceClassAnnotation) {
        AutowireCapableBeanFactory beanFactory = context.getAutowireCapableBeanFactory();
        final List< Object > providers = new ArrayList<>();
        for (final Class< ? > clazz: classes) {
            if (serviceClassAnnotation != null && clazz.getAnnotation(serviceClassAnnotation) == null) {
                continue;
            }
            Object bean;
            try {
                bean = beanFactory.createBean(clazz, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
            } catch (Exception ex) {
                String stackTrace = ExceptionUtils.getStackTrace(ex);
                LOG.fine("Autowire failure for a " + clazz.getName() + " bean: " + stackTrace);
                bean = beanFactory.createBean(clazz);
            }
            providers.add(bean);
        }
        return providers;
    }
}
