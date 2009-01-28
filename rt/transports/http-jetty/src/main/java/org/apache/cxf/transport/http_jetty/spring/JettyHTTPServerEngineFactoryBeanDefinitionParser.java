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
package org.apache.cxf.transport.http_jetty.spring;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.configuration.spring.BusWiringType;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.http_jetty.ThreadingParameters;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class JettyHTTPServerEngineFactoryBeanDefinitionParser
        extends AbstractBeanDefinitionParser {
    static final String HTTP_JETTY_NS = "http://cxf.apache.org/transports/http-jetty/configuration";

    protected String resolveId(Element elem, AbstractBeanDefinition definition, 
                               ParserContext ctx) throws BeanDefinitionStoreException {
        String id = this.getIdOrName(elem);
        if (StringUtils.isEmpty(id)) {
            return JettyHTTPServerEngineFactory.class.getName();            
        }
        id = super.resolveId(elem, definition, ctx);
        if (!ctx.getRegistry().containsBeanDefinition(JettyHTTPServerEngineFactory.class.getName())) {
            ctx.getRegistry().registerAlias(id, JettyHTTPServerEngineFactory.class.getName());
        }
        return id;
    }
    

    @Override
    public void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        //bean.setAbstract(true);        
        String bus = element.getAttribute("bus");
        
        BeanDefinitionBuilder factbean 
            = BeanDefinitionBuilder
                .rootBeanDefinition(JettySpringTypesFactory.class);

        ctx.getRegistry()
            .registerBeanDefinition(JettySpringTypesFactory.class.getName(),
                                    factbean.getBeanDefinition());
        try {
            if (StringUtils.isEmpty(bus)) {
                addBusWiringAttribute(bean, BusWiringType.CONSTRUCTOR);
            } else {
                bean.addConstructorArgReference(bus);
            }

            bean.addConstructorArg(mapElementToJaxbBean(element,
                                                        Map.class,
                                                        JettySpringTypesFactory.class,
                                "createTLSServerParametersMap"));
            bean.addConstructorArg(mapElementToJaxbBean(element,
                                                        Map.class,
                                                        JettySpringTypesFactory.class,
                                "createThreadingParametersMap"));
            
            // parser the engine list
            List list = 
                getRequiredElementsList(element, ctx, new QName(HTTP_JETTY_NS, "engine"), bean);
            if (list.size() > 0) {
                bean.addPropertyValue("enginesList", list);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not process configuration.", e);
        }
    }    
    
    @SuppressWarnings("unchecked")
    private List getRequiredElementsList(Element parent, ParserContext ctx, QName name,
                                         BeanDefinitionBuilder bean) {
       
        List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(parent, 
                                                                     name.getNamespaceURI(), 
                                                                     name.getLocalPart());
        ManagedList list = new ManagedList(elemList.size());
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
    protected Class getBeanClass(Element arg0) {
        return SpringJettyHTTPServerEngineFactory.class;
    }
    
    public static class SpringJettyHTTPServerEngineFactory extends JettyHTTPServerEngineFactory 
        implements ApplicationContextAware {

        public SpringJettyHTTPServerEngineFactory() {
            super();
        }
        public SpringJettyHTTPServerEngineFactory(Bus bus,
                                                  Map<String, TLSServerParameters> tls,
                                                  Map<String, ThreadingParameters> threading) {
            super(bus, tls, threading);
        }    
        
        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            if (getBus() == null) {
                Bus bus = BusFactory.getThreadDefaultBus();
                BusWiringBeanFactoryPostProcessor.updateBusReferencesInContext(bus, ctx);
                setBus(bus);
                registerWithBus();
            }
        }
    }
    

}
