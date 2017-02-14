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
package org.apache.cxf.jaxrs.client.blueprint;

import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.cxf.configuration.blueprint.SimpleBPBeanDefinitionParser;
import org.apache.cxf.jaxrs.blueprint.JAXRSServerFactoryBeanDefinitionParser.PassThroughCallable;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.utils.ResourceUtils;



public class JAXRSClientFactoryBeanDefinitionParser extends SimpleBPBeanDefinitionParser {

    public JAXRSClientFactoryBeanDefinitionParser() {
        super(JAXRSClientFactoryBean.class);
    }
    @Override
    public String getFactorySuffix() {
        return ".proxyFactory";
    }
    public String getFactoryCreateType(Element element) {
        return Client.class.getName();
    }

    @Override
    protected boolean hasBusProperty() {
        return true;
    }


    @Override
    protected void mapAttribute(MutableBeanMetadata bean,
                                Element e, String name,
                                String val, ParserContext context) {
        if ("serviceName".equals(name)) {
            QName q = parseQName(e, val);
            bean.addProperty(name, this.createValue(context, q));
        } else {
            mapToProperty(bean, name, val, context);
        }
    }

    @Override
    protected void mapElement(ParserContext ctx, MutableBeanMetadata bean, Element el, String name) {
        if ("properties".equals(name) || "headers".equals(name)) {
            bean.addProperty(name, this.parseMapData(ctx, bean, el));
        } else if ("executor".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "serviceFactory.executor");
        } else if ("binding".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, "bindingConfig");
        } else if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
            || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)) {
            bean.addProperty(name, parseListData(ctx, bean, el));
        } else if ("features".equals(name) || "providers".equals(name)
                   || "schemaLocations".equals(name) || "modelBeans".equals(name)) {
            bean.addProperty(name, parseListData(ctx, bean, el));
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

}
