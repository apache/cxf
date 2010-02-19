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
package org.apache.cxf.configuration.spring;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.CacheMap;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

public abstract class AbstractBeanDefinitionParser 
    extends org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser {
    public static final String WIRE_BUS_ATTRIBUTE = AbstractBeanDefinitionParser.class.getName() + ".wireBus";

    private static Map<String, JAXBContext> packageContextCache = new CacheMap<String, JAXBContext>(); 
    
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractBeanDefinitionParser.class);
    
    private Class beanClass;
    
    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        boolean setBus = parseAttributes(element, ctx, bean);        
        if (!setBus && hasBusProperty()) {
            addBusWiringAttribute(bean, BusWiringType.PROPERTY);
        }
        parseChildElements(element, ctx, bean);
    }
    
    protected boolean parseAttributes(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        NamedNodeMap atts = element.getAttributes();
        boolean setBus = false;
        for (int i = 0; i < atts.getLength(); i++) {
            Attr node = (Attr) atts.item(i);
            String val = node.getValue();
            String pre = node.getPrefix();
            String name = node.getLocalName();
            String prefix = node.getPrefix();
            
            // Don't process namespaces
            if (isNamespace(name, prefix)) {
                continue;
            }
            
            if ("createdFromAPI".equals(name)) {
                bean.setAbstract(true);
            } else if ("abstract".equals(name)) {
                bean.setAbstract(true);
            } else if ("depends-on".equals(name)) {
                bean.addDependsOn(val);
            } else if (!"id".equals(name) && !"name".equals(name) && isAttribute(pre, name)) {
                if ("bus".equals(name)) {                                     
                    if (val != null && val.trim().length() > 0 
                        && ctx.getRegistry().containsBeanDefinition(val)) {
                        bean.addPropertyReference(name, val);
                        setBus = true;                         
                    }
                } else {
                    mapAttribute(bean, element, name, val);
                }    
            }
        } 
        return setBus;
    }

    private boolean isNamespace(String name, String prefix) {
        return "xmlns".equals(prefix) || prefix == null && "xmlns".equals(name);
    }
    
    protected void parseChildElements(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        Element el = DOMUtils.getFirstElement(element);
        while (el != null) {
            String name = el.getLocalName();
            mapElement(ctx, bean, el, name);
            el = DOMUtils.getNextElement(el);     
        }
    }

    public Class getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    protected Class getBeanClass(Element e) {
        return beanClass;
    }

    protected void mapAttribute(BeanDefinitionBuilder bean, Element e, String name, String val) {
        mapAttribute(bean, name, val);
    }

    protected void mapAttribute(BeanDefinitionBuilder bean, String name, String val) {
        mapToProperty(bean, name, val);
    }
    
    protected void mapElement(ParserContext ctx, BeanDefinitionBuilder bean, Element e, String name) {
    }
    
    @Override
    protected String resolveId(Element elem, AbstractBeanDefinition definition, 
                               ParserContext ctx) throws BeanDefinitionStoreException {
        
        // REVISIT: use getAttributeNS instead
        
        String id = getIdOrName(elem);
        String createdFromAPI = elem.getAttribute(BeanConstants.CREATED_FROM_API_ATTR);
        
        if (null == id || "".equals(id)) {
            return super.resolveId(elem, definition, ctx);
        } 
        
        if (createdFromAPI != null && "true".equals(createdFromAPI.toLowerCase())) {
            return id + getSuffix();
        }
        return id;        
    }

    protected boolean hasBusProperty() {
        return false;
    }
    
    protected String getSuffix() {
        return "";
    }

    protected void setFirstChildAsProperty(Element element, ParserContext ctx, 
                                         BeanDefinitionBuilder bean, String propertyName) {

        Element first = getFirstChild(element);
        
        if (first == null) {
            throw new IllegalStateException(propertyName + " property must have child elements!");
        }
        
        String id;
        BeanDefinition child;
        if (first.getNamespaceURI().equals(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI)) {
            String name = first.getLocalName();
            if ("ref".equals(name)) {
                id = first.getAttribute("bean");
                if (id == null) {
                    throw new IllegalStateException("<ref> elements must have a \"bean\" attribute!");
                }
                bean.addPropertyReference(propertyName, id);
                return;
            } else if ("bean".equals(name)) {
                BeanDefinitionHolder bdh = ctx.getDelegate().parseBeanDefinitionElement(first);
                child = bdh.getBeanDefinition();
                bean.addPropertyValue(propertyName, child);
                return;
            } else {
                throw new UnsupportedOperationException("Elements with the name " + name  
                                                        + " are not currently "
                                                        + "supported as sub elements of " 
                                                        + element.getLocalName());
            }
        }
        child = ctx.getDelegate().parseCustomElement(first, bean.getBeanDefinition());
        bean.addPropertyValue(propertyName, child);
    }

    protected Element getFirstChild(Element element) {
        return DOMUtils.getFirstElement(element);
    }

    protected void addBusWiringAttribute(BeanDefinitionBuilder bean, BusWiringType type) {
        LOG.fine("Adding " + WIRE_BUS_ATTRIBUTE + " attribute " + type + " to bean " + bean);
        bean.getRawBeanDefinition().setAttribute(WIRE_BUS_ATTRIBUTE, type);
    }
    
    protected void mapElementToJaxbProperty(Element parent, 
                                            BeanDefinitionBuilder bean, 
                                            QName name,
                                            String propertyName) {
        mapElementToJaxbProperty(parent, bean, name, propertyName, null);
    }
   
    protected void mapElementToJaxbProperty(Element parent, 
                                            BeanDefinitionBuilder bean, 
                                            QName name,
                                            String propertyName, 
                                            Class<?> c) {
        Element data = null;
        
        Node node = parent.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && name.getLocalPart().equals(node.getLocalName())
                && name.getNamespaceURI().equals(node.getNamespaceURI())) {
                data = (Element)node;
                break;
            }
            node = node.getNextSibling();
        }

        if (data == null) {
            return;
        }
        mapElementToJaxbProperty(data, bean, propertyName, c);
    }
    
    @SuppressWarnings("deprecation")
    protected void mapElementToJaxbProperty(Element data, 
                                            BeanDefinitionBuilder bean, 
                                            String propertyName, 
                                            Class<?> c) {
        JAXBContext context = null;
        try {
            String pkg = getJaxbPackage();
            if (null != c) {
                pkg = PackageUtils.getPackageName(c);
            }
            context = packageContextCache.get(pkg);
            if (context == null) {
                context = JAXBContext.newInstance(pkg, getClass().getClassLoader());
                packageContextCache.put(pkg, context);
            }
            try {
                StringWriter writer = new StringWriter();
                XMLStreamWriter xmlWriter = StaxUtils.createXMLStreamWriter(writer);
                StaxUtils.copy(data, xmlWriter);
                xmlWriter.flush();
    
                BeanDefinitionBuilder jaxbbean 
                    = BeanDefinitionBuilder.rootBeanDefinition(JAXBBeanFactory.class);
                jaxbbean.getRawBeanDefinition().setFactoryMethodName("createJAXBBean");
                jaxbbean.addConstructorArg(context);
                jaxbbean.addConstructorArg(writer.toString());
                jaxbbean.addConstructorArg(c);
                bean.addPropertyValue(propertyName, jaxbbean.getBeanDefinition());
            } catch (Exception ex) {
                Unmarshaller u = context.createUnmarshaller();
                Object obj;
                if (c != null) {
                    obj = u.unmarshal(data, c);
                } else {
                    obj = u.unmarshal(data);
                }
                if (obj instanceof JAXBElement<?>) {
                    JAXBElement<?> el = (JAXBElement<?>)obj;
                    obj = el.getValue();
                }
                if (obj != null) {
                    bean.addPropertyValue(propertyName, obj);
                }
            }
        } catch (JAXBException e) {
            throw new RuntimeException("Could not parse configuration.", e);
        }
    }


    public void mapElementToJaxbPropertyFactory(Element data, 
                                                BeanDefinitionBuilder bean, 
                                                String propertyName, 
                                                Class<?> factory,
                                                String method,
                                                Object ... args) {
        bean.addPropertyValue(propertyName, mapElementToJaxbBean(data, factory,
                                                                 null, method, args));
    }
    
    @SuppressWarnings("deprecation")
    public AbstractBeanDefinition mapElementToJaxbBean(Element data, 
                                                       Class<?> cls,
                                                      Class<?> factory,
                                                      String method,
                                                      Object ... args) {
        StringWriter writer = new StringWriter();
        XMLStreamWriter xmlWriter = StaxUtils.createXMLStreamWriter(writer);
        try {
            StaxUtils.copy(data, xmlWriter);
            xmlWriter.flush();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        BeanDefinitionBuilder jaxbbean 
            = BeanDefinitionBuilder.rootBeanDefinition(cls);
        if (factory != null) {
            jaxbbean.getRawBeanDefinition().setFactoryBeanName(factory.getName());
        }
        jaxbbean.getRawBeanDefinition().setFactoryMethodName(method);
        jaxbbean.addConstructorArg(writer.toString());
        if (args != null) {
            for (Object o : args) {
                jaxbbean.addConstructorArg(o);
            }                
        }
        return jaxbbean.getBeanDefinition();
    }
    
    protected static <T> T unmarshalFactoryString(String s, Class<T> cls) {
        StringReader reader = new StringReader(s);
        XMLStreamReader data = StaxUtils.createXMLStreamReader(reader);
        try {
            String pkg = cls.getPackage().getName();
            JAXBContext context = packageContextCache.get(pkg);
            if (context == null) {
                context = JAXBContext.newInstance(pkg, cls.getClassLoader());
                packageContextCache.put(pkg, context);
            }
            
            Unmarshaller u = context.createUnmarshaller();
            JAXBElement<?> obj = u.unmarshal(data, cls);
            return cls.cast(obj.getValue());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected String getJaxbPackage() {
        return "";
    }

    protected void mapToProperty(BeanDefinitionBuilder bean, String propertyName, String val) {
        if (ID_ATTRIBUTE.equals(propertyName)) {
            return;
        }
        
        if (StringUtils.hasText(val)) {
            if (val.startsWith("#")) {
                bean.addPropertyReference(propertyName, val.substring(1));
            } else {
                bean.addPropertyValue(propertyName, val);
            }
        }
    }
    
    protected boolean isAttribute(String pre, String name) {
        return !"xmlns".equals(name) && (pre == null || !pre.equals("xmlns"))
            && !"abstract".equals(name) && !"lazy-init".equals(name) && !"id".equals(name);
    }

    protected QName parseQName(Element element, String t) {
        String ns = null;
        String pre = null;
        String local = null;

        if (t.startsWith("{")) {
            int i = t.indexOf('}');
            if (i == -1) {
                throw new RuntimeException("Namespace bracket '{' must having a closing bracket '}'.");
            }

            ns = t.substring(1, i);
            t = t.substring(i + 1);
        }

        int colIdx = t.indexOf(':');
        if (colIdx == -1) {
            local = t;
            pre = "";
            
            ns = DOMUtils.getNamespace(element, "");
        } else {
            pre = t.substring(0, colIdx);
            local = t.substring(colIdx + 1);
            
            ns = DOMUtils.getNamespace(element, pre);
        }

        return new QName(ns, local, pre);
    }

    /* This id-or-name resolution logic follows that in Spring's
     * org.springframework.beans.factory.xml.BeanDefinitionParserDelegate object
     * Intent is to have resolution of CXF custom beans follow that of Spring beans
     */    
    protected String getIdOrName(Element elem) {
        String id = elem.getAttribute(BeanDefinitionParserDelegate.ID_ATTRIBUTE);
        
        if (null == id || "".equals(id)) {
            String names = elem.getAttribute(BeanConstants.NAME_ATTR);
            if (null != names) {
                StringTokenizer st = 
                    new StringTokenizer(names, BeanDefinitionParserDelegate.BEAN_NAME_DELIMITERS);
                if (st.countTokens() > 0) {
                    id = st.nextToken();
                }
            }
        }
        return id;
    }

}
