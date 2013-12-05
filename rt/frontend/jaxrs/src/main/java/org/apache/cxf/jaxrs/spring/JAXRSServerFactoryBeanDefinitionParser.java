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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ext.Provider;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;





public class JAXRSServerFactoryBeanDefinitionParser extends AbstractBeanDefinitionParser {
    
    
    public JAXRSServerFactoryBeanDefinitionParser() {
        super();
        setBeanClass(SpringJAXRSServerFactoryBean.class);
    }
    
    @Override
    protected void mapAttribute(BeanDefinitionBuilder bean, Element e, String name, String val) {
        if ("beanNames".equals(name)) {
            String[] values = StringUtils.split(val, " ");
            List<SpringResourceFactory> tempFactories = new ArrayList<SpringResourceFactory>(values.length);
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
        } else if ("base-packages".equals(name)) {
            final String[] values = StringUtils.split(val, ",");
            final Set<String> basePackages = new HashSet<String>(values.length);
            for (final String value : values) {
                final String trimmed = value.trim();
                if (trimmed.equals(SpringJAXRSServerFactoryBean.ALL_PACKAGES)) {
                    basePackages.clear();
                    basePackages.add(trimmed);
                    break;
                } else if (trimmed.length() > 0) {
                    basePackages.add(trimmed);
                }
            }
            bean.addPropertyValue("basePackages", basePackages);
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
        
        private static final String ALL_CLASS_FILES = "**/*.class";
        private static final String ALL_PACKAGES = "*";
        
        private List<SpringResourceFactory> tempFactories;
        private List<String> basePackages;

        public SpringJAXRSServerFactoryBean() {
            super();
        }

        public SpringJAXRSServerFactoryBean(JAXRSServiceFactoryBean sf) {
            super(sf);
        }
        
        public void setBasePackages(List<String> basePackages) {
            this.basePackages = basePackages;
        }
        
        public void setTempResourceProviders(List<SpringResourceFactory> providers) {
            tempFactories = providers;
        }
        
        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            if (tempFactories != null) {
                List<ResourceProvider> factories = new ArrayList<ResourceProvider>(
                    tempFactories.size());
                for (int i = 0; i < tempFactories.size(); i++) {
                    SpringResourceFactory factory = tempFactories.get(i);
                    factory.setApplicationContext(ctx);
                    factories.add(factory);
                }
                tempFactories.clear();
                super.setResourceProviders(factories);
            }
            
            try {
                if (basePackages != null && !basePackages.isEmpty()) {
                    final List< Object > providers = new ArrayList< Object >();
                    
                    // Reusing Spring's approach to classpath scanning. Because Java packages are
                    // open, it's impossible to get all classes belonging to specific package.
                    // Instead, the classpath is looked for *.class files under package's
                    // path (f.e., package 'com.example' becomes a classpath 'com/example/**/*.class'). 
                    final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                    final MetadataReaderFactory factory = new CachingMetadataReaderFactory(resolver);

                    for (final String basePackage: basePackages) {
                        final boolean scanAllPackages = basePackage.equals(ALL_PACKAGES);
                        
                        final String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX 
                            + (scanAllPackages ? "" : ClassUtils.convertClassNameToResourcePath(basePackage)) 
                            + ALL_CLASS_FILES;
                        
                        final Resource[] resources = resolver.getResources(packageSearchPath);                        
                        for (final Resource resource: resources) {
                            final MetadataReader reader = factory.getMetadataReader(resource);
                            final AnnotationMetadata metadata = reader.getAnnotationMetadata();
                            
                            if (scanAllPackages && shouldSkip(metadata.getClassName())) {
                                continue;
                            }
                            
                            // Create a bean only if it's a provider (annotated)
                            if (metadata.isAnnotated(Provider.class.getName())) {                                
                                final Class<?> clazz = ClassLoaderUtils.loadClass(metadata.getClassName(), getClass());
                                providers.add(ctx.getAutowireCapableBeanFactory().createBean(clazz));
                            }
                        }                        
                    }
                    
                    if (!providers.isEmpty()) {                        
                        this.setProviders(providers);
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
        
        private boolean shouldSkip(final String classname) {
            return classname.startsWith("org.apache.cxf");
        }
    }
}
