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

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.jsse.spring.TLSServerParametersConfig;
import org.apache.cxf.configuration.security.TLSServerParametersType;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.configuration.spring.BusWiringType;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.http_jetty.ThreadingParameters;
import org.apache.cxf.transports.http_jetty.configuration.TLSServerParametersIdentifiedType;
import org.apache.cxf.transports.http_jetty.configuration.ThreadingParametersIdentifiedType;
import org.apache.cxf.transports.http_jetty.configuration.ThreadingParametersType;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;





public class JettyHTTPServerEngineBeanDefinitionParser extends AbstractBeanDefinitionParser {

    
    public void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        
        String portStr = element.getAttribute("port");
        int port = Integer.valueOf(portStr);
        bean.addPropertyValue("port", port);
               
        String continuationsStr = element.getAttribute("continuationsEnabled");
        if (continuationsStr != null && continuationsStr.length() > 0) {
            bean.addPropertyValue("continuationsEnabled", Boolean.parseBoolean(continuationsStr));
        }
        
        MutablePropertyValues engineFactoryProperties = ctx.getContainingBeanDefinition().getPropertyValues();
        PropertyValue busValue = engineFactoryProperties.getPropertyValue("bus");
              
        // get the property value from parents
        
        try {
            Element elem = DOMUtils.getFirstElement(element);
            while (elem != null) {
                String name = elem.getLocalName();
                if ("tlsServerParameters".equals(name)) {
                    
                    TLSServerParametersType parametersType = 
                        JAXBHelper.parseElement(elem, bean, TLSServerParametersType.class);
                    
                    TLSServerParametersConfig param = 
                        new TLSServerParametersConfig(parametersType);
                    
                    bean.addPropertyValue("tlsServerParameters", param);
                    
                } else if ("tlsServerParametersRef".equals(name)) {
                    
                    TLSServerParametersIdentifiedType parameterTypeRef = 
                        JAXBHelper.parseElement(elem, bean, 
                                                TLSServerParametersIdentifiedType.class);
                    
                    TLSServerParameters param = 
                        getTlsServerParameters(engineFactoryProperties, parameterTypeRef.getId()); 
                    bean.addPropertyValue("tlsServerParameters", param);
                    
                } else if ("threadingParameters".equals(name)) {
                    ThreadingParametersType parametersType = 
                        JAXBHelper.parseElement(elem, bean, ThreadingParametersType.class);
                    
                    ThreadingParameters param = toThreadingParameters(parametersType);
                    bean.addPropertyValue("threadingParameters", param);  
                    
                } else if ("threadingParametersRef".equals(name)) {
                    ThreadingParametersIdentifiedType parametersTypeRef =
                        JAXBHelper.parseElement(elem, bean, 
                                                ThreadingParametersIdentifiedType.class);
                    ThreadingParameters param = 
                        getThreadingParameters(engineFactoryProperties, parametersTypeRef.getId());
                    bean.addPropertyValue("threadingParameters", param);
                    
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
                    bean.addPropertyValue(name, Boolean.valueOf(text));
                }                         

                elem = org.apache.cxf.helpers.DOMUtils.getNextElement(elem);          
            }
        /*
        try {
            
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    String name = n.getLocalName();
                    if ("tlsServerParameters".equals(name)) {
                        
                        TLSServerParametersType parametersType = 
                            JAXBHelper.parseElement((Element)n, bean, TLSServerParametersType.class);
                        
                        TLSServerParametersConfig param = 
                            new TLSServerParametersConfig(parametersType);
                        
                        bean.addPropertyValue("tlsServerParameters", param);
                        
                    } else if ("tlsServerParametersRef".equals(name)) {
                        
                        TLSServerParametersIdentifiedType parameterTypeRef = 
                            JAXBHelper.parseElement((Element)n, bean, 
                                                    TLSServerParametersIdentifiedType.class);
                        
                        TLSServerParameters param = 
                            getTlsServerParameters(engineFactoryProperties, parameterTypeRef.getId()); 
                        bean.addPropertyValue("tlsServerParameters", param);
                        
                    } else if ("threadingParameters".equals(name)) {
                        ThreadingParametersType parametersType = 
                            JAXBHelper.parseElement((Element)n, bean, ThreadingParametersType.class);
                        
                        ThreadingParameters param = toThreadingParameters(parametersType);
                        bean.addPropertyValue("threadingParameters", param);  
                        
                    } else if ("threadingParametersRef".equals(name)) {
                        ThreadingParametersIdentifiedType parametersTypeRef =
                            JAXBHelper.parseElement((Element)n, bean, 
                                                    ThreadingParametersIdentifiedType.class);
                        ThreadingParameters param = 
                            getThreadingParameters(engineFactoryProperties, parametersTypeRef.getId());
                        bean.addPropertyValue("threadingParameters", param);
                        
                    } else if ("connector".equals(name)) { 
                        // only deal with the one connector here
                        List list = 
                            ctx.getDelegate().parseListElement((Element) n, bean.getBeanDefinition());
                        bean.addPropertyValue("connector", list.get(0));
                    } else if ("handlers".equals(name)) {
                        List handlers = 
                            ctx.getDelegate().parseListElement((Element) n, bean.getBeanDefinition());
                        bean.addPropertyValue("handlers", handlers);
                    } else if ("sessionSupport".equals(name) || "reuseAddress".equals(name)) {
                        String text = n.getTextContent();                        
                        bean.addPropertyValue(name, Boolean.valueOf(text));
                    }                         
                }
            }
            */
        } catch (Exception e) {
            throw new RuntimeException("Could not process configuration.", e);
        }
        
        // if the containing bean is having the bus wired up by the post processor then we should too
        if (ctx.getContainingBeanDefinition().getAttribute(WIRE_BUS_ATTRIBUTE) == BusWiringType.PROPERTY) {
            addBusWiringAttribute(bean, BusWiringType.PROPERTY);
        } else {
            bean.addPropertyValue("bus", busValue.getValue());
        }
        
        bean.setLazyInit(false);
        
    }
    
    private TLSServerParameters getTlsServerParameters(
             MutablePropertyValues engineFactoryProperties,
             String reference) {
        TLSServerParameters result = null;
        PropertyValue tlsParameterMapValue  = 
            engineFactoryProperties.getPropertyValue("tlsServerParametersMap");
        if (null == tlsParameterMapValue) {
            throw new RuntimeException("Could not find the tlsServerParametersMap " 
                                       + "from the JettyHTTPServerEngineFactory!");
        } else {
            Map tlsServerParametersMap  = 
                (Map)tlsParameterMapValue.getValue();
            result = (TLSServerParameters)tlsServerParametersMap.get(reference);
            if (result == null) {
                throw new RuntimeException("Could not find the tlsServerParametersMap reference [" 
                                           + reference + "]'s mapping tlsParameter");
            }
        }
        return result;
    }
    
    private ThreadingParameters getThreadingParameters(
             MutablePropertyValues engineFactoryProperties,
             String reference) {
        ThreadingParameters result = null;
        PropertyValue threadingParametersMapValue = 
            engineFactoryProperties.getPropertyValue("threadingParametersMap");
        if (null == threadingParametersMapValue) {
            throw new RuntimeException("Could not find the threadingParametersMap " 
                                       + "from the JettyHTTPServerEngineFactory!");
        } else {
            Map threadingParametersMap  = (Map)threadingParametersMapValue.getValue();
            result = (ThreadingParameters)threadingParametersMap.get(reference);
            if (result == null) {
                throw new RuntimeException("Could not find the threadingParametersMap reference [" 
                          + reference + "]'s mapping threadingParameters");
            }
        }     
       
        return result;
    }    
                                            
    
    private ThreadingParameters toThreadingParameters(
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
        return JettyHTTPServerEngine.class;
    }
    
    public static class SpringJettyHTTPServerEngine extends JettyHTTPServerEngine
        implements ApplicationContextAware {
        
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
    }
        

}
