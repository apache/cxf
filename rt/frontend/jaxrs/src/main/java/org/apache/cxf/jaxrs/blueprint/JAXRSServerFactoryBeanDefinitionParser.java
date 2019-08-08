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
package org.apache.cxf.jaxrs.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.blueprint.SimpleBPBeanDefinitionParser;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;



public class JAXRSServerFactoryBeanDefinitionParser extends SimpleBPBeanDefinitionParser {

    public JAXRSServerFactoryBeanDefinitionParser() {
        this(BPJAXRSServerFactoryBean.class);
    }
    
    public JAXRSServerFactoryBeanDefinitionParser(Class<?> cls) {
        super(cls);
    }
    @Override
    protected void mapAttribute(MutableBeanMetadata bean,
                                Element e, String name,
                                String val, ParserContext context) {
        if ("beanNames".equals(name)) {
            String[] values = val.split(" ");
            MutableCollectionMetadata tempFactories = context.createMetadata(MutableCollectionMetadata.class);
            for (String v : values) {
                String theValue = v.trim();
                if (theValue.length() > 0) {

                    MutablePassThroughMetadata factory
                        = context.createMetadata(MutablePassThroughMetadata.class);
                    factory.setObject(new PassThroughCallable<Object>(new BlueprintResourceFactory(v)));

                    MutableBeanMetadata resourceBean = context.createMetadata(MutableBeanMetadata.class);
                    resourceBean.setRuntimeClass(BlueprintResourceFactory.class);
                    resourceBean.setFactoryComponent(factory);
                    resourceBean.setFactoryMethod("call");
                    resourceBean.setInitMethod("init");

                    tempFactories.addValue(resourceBean);
                }
            }
            bean.addProperty("tempFactories", tempFactories);
        } else if ("serviceName".equals(name)) {
            QName q = parseQName(e, val);
            bean.addProperty(name, createValue(context, q));
        } else if ("publish".equals(name)) {
            mapToProperty(bean, "start", val, context);
        } else {
            mapToProperty(bean, name, val, context);
        }
    }

    @Override
    protected void mapElement(ParserContext ctx, MutableBeanMetadata bean, Element el, String name) {
        if ("properties".equals(name)
            || "extensionMappings".equals(name)
            || "languageMappings".equals(name)) {
            bean.addProperty(name, this.parseMapData(ctx, bean, el));
        } else if ("executor".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "serviceFactory.executor");
        } else if ("invoker".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "serviceFactory.invoker");
        } else if ("binding".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "bindingConfig");
        } else if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
            || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)) {
            bean.addProperty(name, this.parseListData(ctx, bean, el));
        } else if ("features".equals(name) || "schemaLocations".equals(name)
            || "providers".equals(name) || "serviceBeans".equals(name)
            || "modelBeans".equals(name)) {
            bean.addProperty(name, this.parseListData(ctx, bean, el));
        }  else if ("serviceFactories".equals(name)) {
            bean.addProperty("resourceProviders", this.parseListData(ctx, bean, el));
        } else if ("resourceClasses".equals(name)) {
            List<String> resources = getResourceClassesFromElement(el);
            MutableCollectionMetadata list = ctx.createMetadata(MutableCollectionMetadata.class);
            list.setCollectionClass(List.class);
            for (String res : resources) {
                MutableBeanMetadata objectOfClass = createObjectOfClass(ctx, res);
                list.addValue(objectOfClass);
            }
            bean.addProperty("serviceBeans", list);
        } else if ("model".equals(name)) {
            List<UserResource> resources = ResourceUtils.getResourcesFromElement(el);
            MutableCollectionMetadata list = ctx.createMetadata(MutableCollectionMetadata.class);
            list.setCollectionClass(List.class);
            for (UserResource res : resources) {
                MutablePassThroughMetadata factory = ctx.createMetadata(MutablePassThroughMetadata.class);
                factory.setObject(new PassThroughCallable<Object>(res));

                MutableBeanMetadata resourceBean = ctx.createMetadata(MutableBeanMetadata.class);
                resourceBean.setFactoryComponent(factory);
                resourceBean.setFactoryMethod("call");
                list.addValue(resourceBean);
            }
            bean.addProperty("modelBeans", list);
        } else {
            setFirstChildAsProperty(el, ctx, bean, name);
        }
    }


    @Override
    public Metadata parse(Element element, ParserContext context) {
        MutableBeanMetadata bean = (MutableBeanMetadata)super.parse(element, context);
       
        bean.setInitMethod("init");
        bean.setDestroyMethod("destroy");
        // We don't really want to delay the registration of our Server
        bean.setActivation(ComponentMetadata.ACTIVATION_EAGER);
        return bean;
    }


    @Override
    protected boolean hasBusProperty() {
        return true;
    }

    public static class PassThroughCallable<T> implements Callable<T> {

        private T value;

        public PassThroughCallable(T value) {
            this.value = value;
        }

        public T call() throws Exception {
            return value;
        }
    }

    private static List<String> getResourceClassesFromElement(Element modelEl) {
        List<String> resources = new ArrayList<>();
        List<Element> resourceEls =
            DOMUtils.findAllElementsByTagName(modelEl, "class");
        for (Element e : resourceEls) {
            resources.add(getResourceClassFromElement(e));
        }
        return resources;
    }

    private static String getResourceClassFromElement(Element e) {
        return e.getAttribute("name");
    }
    
    @NoJSR250Annotations
    public static class BPJAXRSServerFactoryBean extends JAXRSServerFactoryBean {

        private Server server;

        public BPJAXRSServerFactoryBean() {
            super();
        }
        public BPJAXRSServerFactoryBean(JAXRSServiceFactoryBean fact) {
            super(fact);
        }
        public Server getServer() {
            return server;
        }

        public void init() {
            create();
        }
        @Override
        public Server create() {
            if (server == null) {
                server = super.create();
            }
            return server;
        }
        public void destroy() {
            if (server != null) {
                server.destroy();
                server = null;
            }
        }
    }

}
