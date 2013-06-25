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
package org.apache.cxf.transport.http.netty.server.spring;

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
import org.apache.cxf.configuration.jsse.TLSServerParametersConfig;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.transports.http_netty_server.configuration.TLSServerParametersIdentifiedType;


@NoJSR250Annotations
public final class NettySpringTypesFactory {
    public NettySpringTypesFactory() {
        
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
   
    public Map<String, TLSServerParameters> createTLSServerParametersMap(String s,
                                                                         JAXBContext ctx) 
        throws Exception {
        Document doc = DOMUtils.readXml(new StringReader(s));
        
        List <TLSServerParametersIdentifiedType> tlsServerParameters =
            NettySpringTypesFactory
                .parseListElement(doc.getDocumentElement(), 
                                  new QName(NettyHttpServerEngineFactoryBeanDefinitionParser.HTTP_NETTY_SERVER_NS,
                                            "identifiedTLSServerParameters"),
                                  TLSServerParametersIdentifiedType.class,
                                  ctx);
        Map<String, TLSServerParameters> tlsServerParametersMap =
            toTLSServerParamenters(tlsServerParameters);
        return tlsServerParametersMap;
    }
    
    @SuppressWarnings("unchecked")
    public static <V> List<V> parseListElement(Element parent, 
                                           QName name, 
                                           Class<?> c,
                                           JAXBContext context) throws JAXBException {
        List<V> list = new ArrayList<V>();
        Node data = null;
           
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
