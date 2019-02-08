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

package org.apache.cxf.jaxws.blueprint;

import java.util.StringTokenizer;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.cxf.Bus;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

class EndpointDefinitionParser extends AbstractBPBeanDefinitionParser {
    private static final Class<?> EP_CLASS = EndpointImpl.class;
    public static String getIdOrName(Element elem) {
        String id = elem.getAttribute("id");

        if (null == id || "".equals(id)) {
            String names = elem.getAttribute("name");
            if (null != names) {
                StringTokenizer st = new StringTokenizer(names, ",");
                if (st.countTokens() > 0) {
                    id = st.nextToken();
                }
            }
        }
        return id;
    }

    public Metadata parse(Element element, ParserContext context) {
        //Endpoint definition
        MutableBeanMetadata cxfBean = context.createMetadata(MutableBeanMetadata.class);

        if (!StringUtils.isEmpty(getIdOrName(element))) {
            cxfBean.setId(getIdOrName(element));
        } else {
            cxfBean.setId("cxf.endpoint." + UUID.randomUUID().toString());
        }
        cxfBean.setRuntimeClass(EP_CLASS);

        boolean isAbstract = false;
        boolean publish = true;
        NamedNodeMap atts = element.getAttributes();

        String bus = null;
        Metadata impl = null;

        for (int i = 0; i < atts.getLength(); i++) {
            Attr node = (Attr) atts.item(i);
            String val = node.getValue();
            String pre = node.getPrefix();
            String name = node.getLocalName();
            if ("createdFromAPI".equals(name) || "abstract".equals(name)) {
                cxfBean.setScope(BeanMetadata.SCOPE_PROTOTYPE);
                isAbstract = true;
            } else if ("publish".equals(name)) {
                publish = Boolean.parseBoolean(val);
            } else if ("bus".equals(name)) {
                bus = val;
            } else if (isAttribute(pre, name)) {
                if ("endpointName".equals(name) || "serviceName".equals(name)) {
                    QName q = parseQName(element, val);
                    cxfBean.addProperty(name, createValue(context, q));
                } else if ("depends-on".equals(name)) {
                    cxfBean.addDependsOn(val);
                } else if ("implementor".equals(name)) {
                    if (val.startsWith("#")) {
                        impl = createRef(context, val.substring(1));
                    } else {
                        impl = createObjectOfClass(context, val);
                    }
                } else if (!"name".equals(name)) {
                    cxfBean.addProperty(name, createValue(context, val));
                }
            }
        }

        Element elem = DOMUtils.getFirstElement(element);
        while (elem != null) {
            String name = elem.getLocalName();
            if ("properties".equals(name)) {
                Metadata map = parseMapData(context, cxfBean, elem);
                cxfBean.addProperty(name, map);
            } else if ("binding".equals(name)) {
                setFirstChildAsProperty(elem, context, cxfBean, "bindingConfig");
            } else if ("inInterceptors".equals(name)
                    || "inFaultInterceptors".equals(name)
                    || "outInterceptors".equals(name)
                    || "outFaultInterceptors".equals(name)
                    || "features".equals(name)
                    || "schemaLocations".equals(name)
                    || "handlers".equals(name)) {
                Metadata list = parseListData(context, cxfBean, elem);
                cxfBean.addProperty(name, list);
            } else if ("implementor".equals(name)) {
                impl = context.parseElement(Metadata.class, cxfBean, elem);
            } else {
                setFirstChildAsProperty(elem, context, cxfBean, name);
            }
            elem = DOMUtils.getNextElement(elem);
        }
        if (StringUtils.isEmpty(bus)) {
            bus = "cxf";
        }
        cxfBean.addArgument(this.getBusRef(context, bus), Bus.class.getName(), 0);
        cxfBean.addArgument(impl, Object.class.getName(), 1);
        if (!isAbstract) {
            if (publish) {
                cxfBean.setInitMethod("publish");
            }
            cxfBean.setDestroyMethod("stop");
        }
        // We don't want to delay the registration of our Server
        cxfBean.setActivation(ComponentMetadata.ACTIVATION_EAGER);
        return cxfBean;
    }
}
