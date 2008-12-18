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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

public final class JAXBHelper {
    private JAXBHelper() {
        
    }
    
    @SuppressWarnings("unchecked")
    public static <V> List<V> parseListElement(Element parent, 
                                           BeanDefinitionBuilder bean, 
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
          
        Node node = parent.getFirstChild();           
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && name.getLocalPart().equals(node.getLocalName())
                && name.getNamespaceURI().equals(node.getNamespaceURI())) {
                data = node;
                Object obj = unmarshal(context, data, c);                
                if (obj != null) {                    
                    list.add((V) obj);
                }
            }
            node = node.getNextSibling();
        }
        return list;
    }
    
    
    public static <T> T parseElement(Element element, 
                               BeanDefinitionBuilder bean, 
                               Class<T> c) throws JAXBException {
        if (null == element) {
            return null;
        }
        JAXBContext context = null;
        String pkg = "";
        if (null != c && c.getPackage() != null) {
            pkg = c.getPackage().getName();
            context = JAXBContext.newInstance(pkg, c.getClassLoader());
        } else {
            context = JAXBContext.newInstance(pkg);
        }
        Object obj = unmarshal(context, element, c);
        
        return c.cast(obj);
    }
    
    
    private static Object unmarshal(JAXBContext context,
                                     Node data, Class<?> c) {
        if (context == null) {
            return null;
        }
        
        Object obj = null;
        
        try {
            
            Unmarshaller u = context.createUnmarshaller();
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
