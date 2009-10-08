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
package org.apache.cxf.transport.http.spring;

import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.configuration.jsse.spring.TLSClientParametersConfig;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HttpAuthSupplier;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;


public class HttpConduitBeanDefinitionParser 
    extends AbstractBeanDefinitionParser {

    private static final String HTTP_NS =
        "http://cxf.apache.org/transports/http/configuration";

    @Override
    public void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        bean.setAbstract(true);
        mapElementToJaxbProperty(element, bean, 
                new QName(HTTP_NS, "client"), "client", 
                HTTPClientPolicy.class);
        mapElementToJaxbProperty(element, bean, 
                new QName(HTTP_NS, "proxyAuthorization"), "proxyAuthorization", 
                ProxyAuthorizationPolicy.class);
        mapElementToJaxbProperty(element, bean, 
                new QName(HTTP_NS, "authorization"), "authorization", 
                AuthorizationPolicy.class);
        
        mapSpecificElements(element, bean);
    }

    /**
     * This method specifically maps the "trustDecider" and "basicAuthSupplier"
     * elements to properties on the HttpConduit.
     * 
     * @param parent This should represent "conduit".
     * @param bean   The bean parser.
     */
    private void mapSpecificElements(
        Element               parent, 
        BeanDefinitionBuilder bean
    ) { 
        Node n = parent.getFirstChild();
        while (n != null) {
            if (Node.ELEMENT_NODE != n.getNodeType() 
                || !HTTP_NS.equals(n.getNamespaceURI())) {
                n = n.getNextSibling();
                continue;
            }
            String elementName = n.getLocalName();
            // Schema should require that no more than one each of these exist.
            if ("trustDecider".equals(elementName)) {                
                mapBeanOrClassElement((Element)n, bean, MessageTrustDecider.class);
            } else if ("authSupplier".equals(elementName)) {
                mapBeanOrClassElement((Element)n, bean, HttpAuthSupplier.class);
            } else if ("basicAuthSupplier".equals(elementName)) {
                mapBeanOrClassElement((Element)n, bean, HttpAuthSupplier.class);
            } else if ("tlsClientParameters".equals(elementName)) {
                mapTLSClientParameters((Element)n, bean);
            }          
            n = n.getNextSibling();
        }
        
    }
    
    /**
     * Inject the "setTlsClientParameters" method with
     * a TLSClientParametersConfig object initialized with the JAXB
     * generated type unmarshalled from the selected node.
     */
    public void mapTLSClientParameters(Element n, BeanDefinitionBuilder bean) {
        StringWriter writer = new StringWriter();
        XMLStreamWriter xmlWriter = StaxUtils.createXMLStreamWriter(writer);
        try {
            StaxUtils.copy(n, xmlWriter);
            xmlWriter.flush();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        BeanDefinitionBuilder jaxbbean 
            = BeanDefinitionBuilder.rootBeanDefinition(TLSClientParametersConfig.class);
        jaxbbean.getRawBeanDefinition().setFactoryMethodName("createTLSClientParameters");
        jaxbbean.addConstructorArg(writer.toString());
        bean.addPropertyValue("tlsClientParameters", jaxbbean.getBeanDefinition());
    }
    
    /**
     * This method finds the class or bean associated with the named element
     * and sets the bean property that is associated with the same name as
     * the element.
     * <p>
     * The element has either a "class" attribute or "bean" attribute, but 
     * not both.
     * 
     * @param element      The element.
     * @param bean         The Bean Definition Parser.
     * @param elementClass The Class a bean or class is supposed to be.
     */
    protected void mapBeanOrClassElement(
        Element               element, 
        BeanDefinitionBuilder bean,
        Class                 elementClass
    ) {
        String elementName = element.getLocalName();
    
        String classProperty = element.getAttribute("class");
        if (classProperty != null && !classProperty.equals("")) {
            try {
                Object obj = 
                    ClassLoaderUtils.loadClass(
                            classProperty, getClass()).newInstance();
                if (!elementClass.isInstance(obj)) {
                    throw new IllegalArgumentException(
                        "Element '" + elementName + "' must be of type " 
                        + elementClass.getName() + ".");
                }
                bean.addPropertyValue(elementName, obj);
            } catch (IllegalAccessException ex) {
                throw new IllegalArgumentException(
                    "Element '" + elementName + "' could not load " 
                    + classProperty
                    + " - " + ex);
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException(
                    "Element '" + elementName + "' could not load " 
                    + classProperty
                    + " - " + ex);
            } catch (InstantiationException ex) {
                throw new IllegalArgumentException(
                    "Element '" + elementName + "' could not load " 
                    + classProperty
                    + " - " + ex);
            }
        }
        String beanref = element.getAttribute("bean");
        if (beanref != null && !beanref.equals("")) {
            if (classProperty != null && !classProperty.equals("")) {
                throw new IllegalArgumentException(
                        "Element '" + elementName + "' cannot have both "
                        + "\"bean\" and \"class\" attributes.");
                                
            }
            bean.addPropertyReference(elementName, beanref);
        } else if (classProperty == null || classProperty.equals("")) {
            throw new IllegalArgumentException(
                    "Element '" + elementName 
                    + "' requires at least one of the "
                    + "\"bean\" or \"class\" attributes.");
        }
    }

    @Override
    protected Class getBeanClass(Element arg0) {
        return HTTPConduit.class;
    }

}
