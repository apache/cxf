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
import java.util.TreeMap;


import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.jsse.spring.TLSServerParametersConfig;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.http_jetty.ThreadingParameters;

import org.apache.cxf.transports.http_jetty.configuration.TLSServerParametersIdentifiedType;
import org.apache.cxf.transports.http_jetty.configuration.ThreadingParametersIdentifiedType;
import org.apache.cxf.transports.http_jetty.configuration.ThreadingParametersType;


import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;

public class JettyHTTPServerEngineFactoryBeanDefinitionParser
        extends AbstractBeanDefinitionParser {
    private static final String HTTP_JETTY_NS = "http://cxf.apache.org/transports/http-jetty/configuration";

    @Override
    public void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        //bean.setAbstract(true);        
        String bus = element.getAttribute("bus");
         
        try {
            List <ThreadingParametersIdentifiedType> threadingParametersIdentifiedTypes = 
                JAXBHelper.parseListElement(element, bean, 
                                            new QName(HTTP_JETTY_NS, "identifiedThreadingParameters"), 
                                            ThreadingParametersIdentifiedType.class);
            Map<String, ThreadingParameters> threadingParametersMap =
                toThreadingParameters(threadingParametersIdentifiedTypes);
            List <TLSServerParametersIdentifiedType> tlsServerParameters =
                JAXBHelper.parseListElement(element, bean, 
                                            new QName(HTTP_JETTY_NS, "identifiedTLSServerParameters"),
                                            TLSServerParametersIdentifiedType.class);
            Map<String, TLSServerParameters> tlsServerParametersMap =
                toTLSServerParamenters(tlsServerParameters);
                                    
            bean.addPropertyValue("threadingParametersMap", threadingParametersMap);
            bean.addPropertyValue("tlsServerParametersMap", tlsServerParametersMap);
            
            
            if (StringUtils.isEmpty(bus)) {
                if (ctx.getRegistry().containsBeanDefinition("cxf")) {
                    bean.addPropertyReference("bus", "cxf");
                }
            } else {
                bean.addPropertyReference("bus", bus);
            }
            
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
       
        NodeList nl = parent.getChildNodes();
        ManagedList list = new ManagedList(nl.getLength());
        list.setSource(ctx.extractSource(parent));
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && name.getLocalPart().equals(n.getLocalName())
                && name.getNamespaceURI().equals(n.getNamespaceURI())) {
                list.add(ctx.getDelegate().
                         parsePropertySubElement((Element)n, bean.getBeanDefinition()));

            }
        }
        return list;
    }
    
    private Map<String, ThreadingParameters> toThreadingParameters(
        List <ThreadingParametersIdentifiedType> list) {
        Map<String, ThreadingParameters> map = new TreeMap<String, ThreadingParameters>();
        for (ThreadingParametersIdentifiedType t : list) {
            ThreadingParameters parameter = 
                toThreadingParameters(t.getThreadingParameters());
            map.put(t.getId(), parameter);
        } 
        return map;
    }
    
    private ThreadingParameters toThreadingParameters(ThreadingParametersType paramtype) {
        ThreadingParameters params = new ThreadingParameters();
        params.setMaxThreads(paramtype.getMaxThreads());
        params.setMinThreads(paramtype.getMinThreads());
        return params;
    }
        
    private Map<String, TLSServerParameters> toTLSServerParamenters(
        List <TLSServerParametersIdentifiedType> list) {
        Map<String, TLSServerParameters> map = new TreeMap<String, TLSServerParameters>();
        for (TLSServerParametersIdentifiedType t : list) {
            try {             
                TLSServerParameters parameter = new TLSServerParametersConfig(t.getTlsServerParameters());
                map.put(t.getId(), parameter);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Could not configure TLS for id " + t.getId(), e);
            }
            
        }
        return map;
        
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
        return JettyHTTPServerEngineFactory.class;
    }

}
