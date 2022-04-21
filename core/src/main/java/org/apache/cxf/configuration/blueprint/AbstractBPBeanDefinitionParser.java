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
package org.apache.cxf.configuration.blueprint;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.apache.cxf.bus.blueprint.BlueprintBus;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBContextCache.CachedContextAndSchemas;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

public abstract class AbstractBPBeanDefinitionParser {

    private static final String XMLNS_BLUEPRINT = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
    private static final String COMPONENT_ID = "component-id";

    private JAXBContext jaxbContext;
    private Set<Class<?>> jaxbClasses;

    protected boolean hasBusProperty() {
        return false;
    }

    public Metadata createValue(ParserContext context, QName qName) {
        MutableBeanMetadata v = context.createMetadata(MutableBeanMetadata.class);
        v.setRuntimeClass(QName.class);
        v.addArgument(createValue(context, qName.getNamespaceURI()), null, 0);
        v.addArgument(createValue(context, qName.getLocalPart()), null, 1);
        return v;
    }

    protected Metadata parseListData(ParserContext context,
                                     ComponentMetadata enclosingComponent,
                                     Element element) {
        MutableCollectionMetadata m
            = (MutableCollectionMetadata) context.parseElement(CollectionMetadata.class,
                                                               enclosingComponent, element);
        m.setCollectionClass(List.class);
        return m;
    }

    protected Metadata parseMapData(ParserContext context,
                                    ComponentMetadata enclosingComponent,
                                    Element element) {
        return context.parseElement(MapMetadata.class, enclosingComponent, element);
    }

    protected void setFirstChildAsProperty(Element element,
                                           ParserContext ctx,
                                           MutableBeanMetadata bean,
                                           String propertyName) {

        Element first = DOMUtils.getFirstElement(element);

        if (first == null) {
            throw new IllegalStateException(propertyName + " property must have child elements!");
        }

        String id;
        if (XMLNS_BLUEPRINT.equals(first.getNamespaceURI())) {
            String name = first.getLocalName();
            if ("ref".equals(name)) {
                id = first.getAttribute(COMPONENT_ID);
                if (id == null) {
                    throw new IllegalStateException("<ref> elements must have a \"component-id\" attribute!");
                }
                bean.addProperty(propertyName, createRef(ctx, id));
            } else {
                //Rely on BP to handle these ones.
                bean.addProperty(propertyName, ctx.parseElement(Metadata.class, bean, first));
            }
        } else {
            bean.addProperty(propertyName, ctx.parseElement(Metadata.class, bean, first));
        }
    }

    public QName parseQName(Element element, String t) {
        String t1 = t;

        if (t1.startsWith("{")) {
            int i = t1.indexOf('}');
            if (i == -1) {
                throw new RuntimeException("Namespace bracket '{' must having a closing bracket '}'.");
            }

            t1 = t1.substring(i + 1);
        }

        final String local;
        final String pre;
        final String ns;
        int colIdx = t1.indexOf(':');
        if (colIdx == -1) {
            local = t1;
            pre = "";

            ns = DOMUtils.getNamespace(element, "");
        } else {
            pre = t1.substring(0, colIdx);
            local = t1.substring(colIdx + 1);

            ns = DOMUtils.getNamespace(element, pre);
        }

        return new QName(ns, local, pre);
    }

    protected boolean parseAttributes(Element element, ParserContext ctx, MutableBeanMetadata bean) {
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

            if ("createdFromAPI".equals(name) || "abstract".equals(name)) {
                bean.setScope(BeanMetadata.SCOPE_PROTOTYPE);
            } else {
                if ("depends-on".equals(name)) {
                    bean.addDependsOn(val);
                } else if ("name".equals(name)) {
                    processNameAttribute(element, ctx, bean, val);
                } else if ("bus".equals(name)) {
                    processBusAttribute(element, ctx, bean, val);
                } else if (!"id".equals(name) && isAttribute(pre, name)) {
                    mapAttribute(bean, element, name, val, ctx);
                }
            }
        }
        return setBus; // 'setBus' is always false
    }
    protected void processBusAttribute(Element element, ParserContext ctx,
                                       MutableBeanMetadata bean, String val) {
        if (this.hasBusProperty()) {
            bean.addProperty("bus", getBusRef(ctx, val));
        } else {
            bean.addArgument(getBusRef(ctx, val), null, 0);
        }
    }

    protected void processNameAttribute(Element element,
                                        ParserContext ctx,
                                        MutableBeanMetadata bean,
                                        String val) {
        //nothing
    }
    protected void mapAttribute(MutableBeanMetadata bean, Element e,
                                String name, String val, ParserContext context) {
        mapToProperty(bean, name, val, context);
    }

    protected boolean isAttribute(String pre, String name) {
        return !"xmlns".equals(name) && (pre == null || !"xmlns".equals(pre))
            && !"abstract".equals(name) && !"lazy-init".equals(name)
            && !"id".equals(name);
    }

    protected boolean isNamespace(String name, String prefix) {
        return "xmlns".equals(prefix) || prefix == null && "xmlns".equals(name);
    }

    protected void mapElement(ParserContext ctx, MutableBeanMetadata bean, Element el, String name) {
    }

    protected void mapToProperty(MutableBeanMetadata bean,
                                 String propertyName,
                                 String val,
                                 ParserContext context) {
        if ("id".equals(propertyName)) {
            return;
        }

        if (!StringUtils.isEmpty(val)) {
            if (val.startsWith("#")) {
                bean.addProperty(propertyName, createRef(context, val.substring(1)));
            } else {
                bean.addProperty(propertyName, createValue(context, val));
            }
        }
    }

    public static ValueMetadata createValue(ParserContext context, String value) {
        MutableValueMetadata v = context.createMetadata(MutableValueMetadata.class);
        v.setStringValue(value);
        return v;
    }

    public static RefMetadata createRef(ParserContext context, String value) {
        MutableRefMetadata r = context.createMetadata(MutableRefMetadata.class);
        r.setComponentId(value);
        return r;
    }

    public static PassThroughMetadata createPassThrough(ParserContext context, Object value) {
        MutablePassThroughMetadata v = context.createMetadata(MutablePassThroughMetadata.class);
        v.setObject(value);
        return v;
    }

    public static MutableBeanMetadata createObjectOfClass(ParserContext context, String value) {
        MutableBeanMetadata v = context.createMetadata(MutableBeanMetadata.class);
        v.setClassName(value);
        return v;
    }

    protected MutableBeanMetadata getBus(ParserContext context, String name) {
        ComponentDefinitionRegistry cdr = context.getComponentDefinitionRegistry();
        ComponentMetadata meta = cdr.getComponentDefinition("blueprintBundle");

        if (!cdr.containsComponentDefinition(InterceptorTypeConverter.class.getName())) {
            MutablePassThroughMetadata md = context.createMetadata(MutablePassThroughMetadata.class);
            md.setObject(new InterceptorTypeConverter());

            md.setId(InterceptorTypeConverter.class.getName());
            context.getComponentDefinitionRegistry().registerTypeConverter(md);
        }
        if (!cdr.containsComponentDefinition(name)) {
            //Create a bus

            MutableBeanMetadata bus = context.createMetadata(MutableBeanMetadata.class);
            bus.setId(name);
            bus.setRuntimeClass(BlueprintBus.class);
            if (meta != null) {
                //blueprint-no-osgi does not provide a bundleContext
                bus.addProperty("bundleContext", createRef(context, "blueprintBundleContext"));
            }
            bus.addProperty("blueprintContainer", createRef(context, "blueprintContainer"));
            bus.setDestroyMethod("shutdown");
            bus.setInitMethod("initialize");

            context.getComponentDefinitionRegistry().registerComponentDefinition(bus);

            return bus;
        }
        return (MutableBeanMetadata) cdr.getComponentDefinition(name);
    }

    protected RefMetadata getBusRef(ParserContext context, String name) {
        if ("cxf".equals(name)) {
            getBus(context, name);
        }
        return createRef(context, name);
    }

    protected void parseChildElements(Element element, ParserContext ctx, MutableBeanMetadata bean) {
        Element el = DOMUtils.getFirstElement(element);
        while (el != null) {
            String name = el.getLocalName();
            mapElement(ctx, bean, el, name);
            el = DOMUtils.getNextElement(el);
        }
    }

    protected void mapElementToJaxbProperty(ParserContext ctx,
                                            MutableBeanMetadata bean, Element parent,
                                            QName name,
                                            String propertyName,
                                            Class<?> c) {
        Element data = DOMUtils.getFirstChildWithName(parent, name);
        if (data == null) {
            return;
        }

        mapElementToJaxbProperty(ctx, bean, data, propertyName, c);
    }

    public static class JAXBBeanFactory {
        final JAXBContext ctx;
        final Class<?> cls;
        public JAXBBeanFactory(JAXBContext c, Class<?> c2) {
            ctx = c;
            cls = c2;
        }


        public Object createJAXBBean(String v) {
            XMLStreamReader reader = StaxUtils.createXMLStreamReader(new StringReader(v));
            try {
                JAXBElement<?> el = JAXBUtils.unmarshall(ctx, reader, cls);
                if (el != null) {
                    return el.getValue();
                }
                return null;
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    //ignore
                }
            }
        }
    }

    protected void mapElementToJaxbProperty(ParserContext ctx,
                                            MutableBeanMetadata bean,
                                            Element data,
                                            String propertyName,
                                            Class<?> c) {
        try {
            Unmarshaller u = null;
            try {
                final StringWriter writer = new StringWriter();
                StaxUtils.writeTo(data, writer);

                MutableBeanMetadata factory = ctx.createMetadata(MutableBeanMetadata.class);
                factory.setClassName(c.getName());
                factory.setFactoryComponent(createPassThrough(ctx, new JAXBBeanFactory(getContext(c), c)));
                factory.setFactoryMethod("createJAXBBean");
                factory.addArgument(createValue(ctx, writer.toString()), String.class.getName(), 0);
                bean.addProperty(propertyName, factory);

            } catch (Exception ex) {
                u = getContext(c).createUnmarshaller();
                u.setEventHandler(null);
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
                    MutablePassThroughMetadata value = ctx.createMetadata(MutablePassThroughMetadata.class);
                    value.setObject(obj);
                    bean.addProperty(propertyName, value);
                }
            } finally {
                JAXBUtils.closeUnmarshaller(u);
            }
        } catch (JAXBException e) {
            throw new RuntimeException("Could not parse configuration.", e);
        }
    }


    protected synchronized JAXBContext getContext(Class<?> cls) {
        if (jaxbContext == null || jaxbClasses == null || !jaxbClasses.contains(cls)) {
            try {
                Set<Class<?>> tmp = new HashSet<>();
                if (jaxbClasses != null) {
                    tmp.addAll(jaxbClasses);
                }
                JAXBContextCache.addPackage(tmp, PackageUtils.getPackageName(cls),
                                            cls == null
                                            ? getClass().getClassLoader()
                                                : cls.getClassLoader());
                if (cls != null) {
                    boolean hasOf = false;
                    for (Class<?> c : tmp) {
                        if (c.getPackage() == cls.getPackage()
                            && "ObjectFactory".equals(c.getSimpleName())) {
                            hasOf = true;
                        }
                    }
                    if (!hasOf) {
                        tmp.add(cls);
                    }
                }
                JAXBContextCache.scanPackages(tmp);
                CachedContextAndSchemas ccs
                    = JAXBContextCache.getCachedContextAndSchemas(tmp, null, null, null, false);
                jaxbClasses = ccs.getClasses();
                jaxbContext = ccs.getContext();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return jaxbContext;
    }
}
