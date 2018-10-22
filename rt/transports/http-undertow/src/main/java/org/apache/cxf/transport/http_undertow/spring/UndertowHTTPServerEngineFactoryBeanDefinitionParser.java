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
package org.apache.cxf.transport.http_undertow.spring;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.configuration.spring.BusWiringType;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.transport.http_undertow.ThreadingParameters;
import org.apache.cxf.transport.http_undertow.UndertowHTTPServerEngineFactory;
import org.apache.cxf.transports.http_undertow.configuration.TLSServerParametersIdentifiedType;
import org.apache.cxf.transports.http_undertow.configuration.ThreadingParametersIdentifiedType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class UndertowHTTPServerEngineFactoryBeanDefinitionParser
        extends AbstractBeanDefinitionParser {
    static final String HTTP_UNDERTOW_NS = "http://cxf.apache.org/transports/http-undertow/configuration";

    protected String resolveId(Element elem, AbstractBeanDefinition definition,
                               ParserContext ctx) throws BeanDefinitionStoreException {
        String id = this.getIdOrName(elem);
        if (StringUtils.isEmpty(id)) {
            return UndertowHTTPServerEngineFactory.class.getName();
        }
        id = super.resolveId(elem, definition, ctx);
        if (!ctx.getRegistry().containsBeanDefinition(UndertowHTTPServerEngineFactory.class.getName())) {
            ctx.getRegistry().registerAlias(id, UndertowHTTPServerEngineFactory.class.getName());
        }
        return id;
    }


    @Override
    public void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {

        String bus = element.getAttribute("bus");

        BeanDefinitionBuilder factbean
            = BeanDefinitionBuilder
                .rootBeanDefinition(UndertowSpringTypesFactory.class);

        ctx.getRegistry()
            .registerBeanDefinition(UndertowSpringTypesFactory.class.getName(),
                                    factbean.getBeanDefinition());
        try {
            if (StringUtils.isEmpty(bus)) {
                addBusWiringAttribute(bean, BusWiringType.CONSTRUCTOR);
            } else {
                bean.addConstructorArgReference(bus);
            }

            bean.addConstructorArgValue(mapElementToJaxbBean(element,
                                                        TLSServerParametersIdentifiedType.class,
                                                        UndertowSpringTypesFactory.class,
                                                        "createTLSServerParametersMap"));
            bean.addConstructorArgValue(mapElementToJaxbBean(element,
                                                        ThreadingParametersIdentifiedType.class,
                                                        UndertowSpringTypesFactory.class,
                                "createThreadingParametersMap"));

            // parser the engine list
            List<Object> list =
                getRequiredElementsList(element, ctx, new QName(HTTP_UNDERTOW_NS, "engine"), bean);
            if (!list.isEmpty()) {
                bean.addPropertyValue("enginesList", list);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not process configuration.", e);
        }
    }

    private List<Object> getRequiredElementsList(Element parent, ParserContext ctx, QName name,
                                         BeanDefinitionBuilder bean) {

        List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(parent,
                                                                     name.getNamespaceURI(),
                                                                     name.getLocalPart());
        ManagedList<Object> list = new ManagedList<>(elemList.size());
        list.setSource(ctx.extractSource(parent));

        for (Element elem : elemList) {
            list.add(ctx.getDelegate().parsePropertySubElement(elem, bean.getBeanDefinition()));
        }
        return list;
    }



    /*
     * We do not require an id from the configuration.
     *
     * (non-Javadoc)
     * @see org.springframework.beans.factory.xml.AbstractBeanDefinitionParser#shouldGenerateId()
     */
    @Override
    protected boolean shouldGenerateId() {
        return true;
    }

    @Override
    protected Class<?> getBeanClass(Element arg0) {
        return SpringUndertowHTTPServerEngineFactory.class;
    }

    @NoJSR250Annotations(unlessNull = "bus")
    public static class SpringUndertowHTTPServerEngineFactory extends UndertowHTTPServerEngineFactory
        implements ApplicationContextAware {

        public SpringUndertowHTTPServerEngineFactory() {
            super();
        }
        public SpringUndertowHTTPServerEngineFactory(Bus bus,
                                                  Map<String, TLSServerParameters> tls,
                                                  Map<String, ThreadingParameters> threading) {
            super(bus, tls, threading);
        }

        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            if (getBus() == null) {
                setBus(BusWiringBeanFactoryPostProcessor.addDefaultBus(ctx));
            }
        }
    }
}
