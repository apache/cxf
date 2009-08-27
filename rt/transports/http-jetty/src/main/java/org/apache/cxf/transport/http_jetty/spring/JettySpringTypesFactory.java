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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.jsse.spring.TLSServerParametersConfig;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.transport.http_jetty.ThreadingParameters;
import org.apache.cxf.transports.http_jetty.configuration.TLSServerParametersIdentifiedType;
import org.apache.cxf.transports.http_jetty.configuration.ThreadingParametersIdentifiedType;
import org.apache.cxf.transports.http_jetty.configuration.ThreadingParametersType;

@NoJSR250Annotations
public final class JettySpringTypesFactory {
    public JettySpringTypesFactory() {
        
    }
    private static Map<String, ThreadingParameters> toThreadingParameters(
        List <ThreadingParametersIdentifiedType> list) {
        Map<String, ThreadingParameters> map = new TreeMap<String, ThreadingParameters>();
        for (ThreadingParametersIdentifiedType t : list) {
            ThreadingParameters parameter = 
                toThreadingParameters(t.getThreadingParameters());
            map.put(t.getId(), parameter);
        } 
        return map;
    }
    private static ThreadingParameters toThreadingParameters(ThreadingParametersType paramtype) {
        ThreadingParameters params = new ThreadingParameters();
        params.setMaxThreads(paramtype.getMaxThreads());
        params.setMinThreads(paramtype.getMinThreads());
        return params;
    }
        
    private static Map<String, TLSServerParameters> toTLSServerParamenters(
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
    public Map<String, ThreadingParameters> createThreadingParametersMap(String s) 
        throws Exception {
        Document doc = DOMUtils.readXml(new StringReader(s));
        List <ThreadingParametersIdentifiedType> threadingParametersIdentifiedTypes = 
            JettySpringTypesFactory
                .parseListElement(doc.getDocumentElement(), 
                                  new QName(JettyHTTPServerEngineFactoryBeanDefinitionParser.HTTP_JETTY_NS,
                                            "identifiedThreadingParameters"), 
                                  ThreadingParametersIdentifiedType.class);
        Map<String, ThreadingParameters> threadingParametersMap =
            toThreadingParameters(threadingParametersIdentifiedTypes);
        return threadingParametersMap;
    }
    
    public Map<String, TLSServerParameters> createTLSServerParametersMap(String s) 
        throws Exception {
        Document doc = DOMUtils.readXml(new StringReader(s));
        
        List <TLSServerParametersIdentifiedType> tlsServerParameters =
            JettySpringTypesFactory
                .parseListElement(doc.getDocumentElement(), 
                                  new QName(JettyHTTPServerEngineFactoryBeanDefinitionParser.HTTP_JETTY_NS,
                                            "identifiedTLSServerParameters"),
                                  TLSServerParametersIdentifiedType.class);
        Map<String, TLSServerParameters> tlsServerParametersMap =
            toTLSServerParamenters(tlsServerParameters);
        return tlsServerParametersMap;
    }
    
    @SuppressWarnings("unchecked")
    public static <V> List<V> parseListElement(Element parent, 
                                           QName name, 
                                           Class<?> c) throws JAXBException {                                
        List<V> list = new ArrayList<V>();
        Node data = null;
           
        JAXBContext context = null;
        String pkg = "";
        if (null != c && c.getPackage() != null) {
            pkg = c.getPackage().getName();
            context = JAXBContext.newInstance(pkg, c.getClassLoader());
        } else {
            context = JAXBContext.newInstance(pkg);
        }
        Unmarshaller u = context.createUnmarshaller();
        Node node = parent.getFirstChild();           
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && name.getLocalPart().equals(node.getLocalName())
                && name.getNamespaceURI().equals(node.getNamespaceURI())) {
                data = node;
                Object obj = unmarshal(u, data, c);                
                if (obj != null) {                    
                    list.add((V) obj);
                }
            }
            node = node.getNextSibling();
        }
        return list;
    }
    
    

    
    
    private static Object unmarshal(Unmarshaller u,
                                     Node data, Class<?> c) {
        if (u == null) {
            return null;
        }
        
        Object obj = null;        
        try {
            if (c != null) {
                obj = u.unmarshal(data, c);
            } else {
                obj = u.unmarshal(data);
            }

            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> el = (JAXBElement<?>)obj;
                obj = el.getValue();
            }
           
        } catch (JAXBException e) {
            throw new RuntimeException("Could not parse configuration.", e);
        }
        
        return obj; 
        
    }
               

}
