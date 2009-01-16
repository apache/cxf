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



import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.configuration.jsse.spring.TLSServerParametersConfig;
import org.apache.cxf.configuration.security.TLSServerParametersType;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.http_jetty.ThreadingParameters;
import org.apache.cxf.transports.http_jetty.configuration.TLSServerParametersIdentifiedType;
import org.apache.cxf.transports.http_jetty.configuration.ThreadingParametersIdentifiedType;
import org.apache.cxf.transports.http_jetty.configuration.ThreadingParametersType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;





public class JettyHTTPServerEngineBeanDefinitionParser extends AbstractBeanDefinitionParser {

    public void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        
        String portStr = element.getAttribute("port");
        bean.addPropertyValue("port", portStr);
               
        String continuationsStr = element.getAttribute("continuationsEnabled");
        if (continuationsStr != null && continuationsStr.length() > 0) {
            bean.addPropertyValue("continuationsEnabled", continuationsStr);
        }
        ValueHolder busValue = ctx.getContainingBeanDefinition()
            .getConstructorArgumentValues().getArgumentValue(0, Bus.class);
        bean.addPropertyValue("bus", busValue.getValue());
        try {
            Element elem = DOMUtils.getFirstElement(element);
            while (elem != null) {
                String name = elem.getLocalName();
                if ("tlsServerParameters".equals(name)) {
                    mapElementToJaxbPropertyFactory(elem,
                                                    bean,
                                                    "tlsServerParameters",
                                                    JettyHTTPServerEngineBeanDefinitionParser.class,
                                                    "createTLSServerParametersConfig");
                } else if ("threadingParameters".equals(name)) {
                    mapElementToJaxbPropertyFactory(elem,
                                                    bean,
                                                    "threadingParameters",
                                                    JettyHTTPServerEngineBeanDefinitionParser.class,
                                                    "createThreadingParameters");
                } else if ("tlsServerParametersRef".equals(name)) {
                    mapElementToJaxbPropertyFactory(elem,
                                                    bean,
                                                    "tlsServerParametersRef",
                                                    JettyHTTPServerEngineBeanDefinitionParser.class,
                                                    "createTLSServerParametersConfigRef");
                } else if ("threadingParametersRef".equals(name)) {
                    mapElementToJaxbPropertyFactory(elem,
                                                    bean,
                                                    "threadingParametersRef",
                                                    JettyHTTPServerEngineBeanDefinitionParser.class,
                                                    "createThreadingParametersRef"
                                                    );
                } else if ("connector".equals(name)) { 
                    // only deal with the one connector here
                    List list = 
                        ctx.getDelegate().parseListElement(elem, bean.getBeanDefinition());
                    bean.addPropertyValue("connector", list.get(0));
                } else if ("handlers".equals(name)) {
                    List handlers = 
                        ctx.getDelegate().parseListElement(elem, bean.getBeanDefinition());
                    bean.addPropertyValue("handlers", handlers);
                } else if ("sessionSupport".equals(name) || "reuseAddress".equals(name)) {
                    String text = elem.getTextContent();                        
                    bean.addPropertyValue(name, text);
                }                         

                elem = org.apache.cxf.helpers.DOMUtils.getNextElement(elem);          
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not process configuration.", e);
        }

        bean.setLazyInit(false);
    }
    
    private static ThreadingParameters toThreadingParameters(
                                    ThreadingParametersType paramtype) {
        ThreadingParameters params = new ThreadingParameters();
        params.setMaxThreads(paramtype.getMaxThreads());
        params.setMinThreads(paramtype.getMinThreads());
        return params;
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
        return SpringJettyHTTPServerEngine.class;
    }
    
    public static class SpringJettyHTTPServerEngine extends JettyHTTPServerEngine
        implements ApplicationContextAware {
        
        String threadingRef;
        String tlsRef;
        
        public SpringJettyHTTPServerEngine(
            JettyHTTPServerEngineFactory fac, 
            Bus bus,
            int port) {
            super(fac, bus, port);
        }
        
        public SpringJettyHTTPServerEngine() {
            super();
        }
        
        
        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            if (getBus() == null) {
                Bus bus = BusFactory.getThreadDefaultBus();
                BusWiringBeanFactoryPostProcessor.updateBusReferencesInContext(bus, ctx);
                setBus(bus);
            }
        }
        
        public void setThreadingParametersRef(String s) {
            threadingRef = s;
        }
        public void setTlsServerParametersRef(String s) {
            tlsRef = s;
        }
        
        @PostConstruct
        public void finalizeConfig() 
            throws GeneralSecurityException,
                   IOException {
            if (tlsRef != null || threadingRef != null) {
                retrieveEngineFactory();
                if (threadingRef != null) {
                    setThreadingParameters(factory.getThreadingParametersMap().get(threadingRef));
                }
                if (tlsRef != null) {
                    setTlsServerParameters(factory.getTlsServerParametersMap().get(tlsRef));
                }
            }
            super.finalizeConfig();
        }

    }
        

    
    public static TLSServerParametersConfig createTLSServerParametersConfig(String s) 
        throws GeneralSecurityException, IOException {
        
        TLSServerParametersType parametersType = unmarshalFactoryString(s, 
                                                                        TLSServerParametersType.class);
        
        return new TLSServerParametersConfig(parametersType);
    }
    public static String createTLSServerParametersConfigRef(String s)
    
        throws GeneralSecurityException, IOException {
        
        TLSServerParametersIdentifiedType parameterTypeRef 
            = unmarshalFactoryString(s, TLSServerParametersIdentifiedType.class);
        
        return parameterTypeRef.getId(); 
    } 
    
    public static ThreadingParameters createThreadingParameters(String s) {
        
        ThreadingParametersType parametersType = unmarshalFactoryString(s, 
                                                                        ThreadingParametersType.class);
        
        return toThreadingParameters(parametersType);
    }
    public static String createThreadingParametersRef(String s) {

        ThreadingParametersIdentifiedType parametersType 
            = unmarshalFactoryString(s, ThreadingParametersIdentifiedType.class);
        return parametersType.getId();
    }
}
