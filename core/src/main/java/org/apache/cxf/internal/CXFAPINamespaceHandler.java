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

package org.apache.cxf.internal;

import java.net.URL;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.Namespaces;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.cxf.bus.blueprint.BusDefinitionParser;
import org.apache.cxf.configuration.blueprint.SimpleBPBeanDefinitionParser;
import org.apache.cxf.feature.FastInfosetFeature;
import org.apache.cxf.workqueue.AutomaticWorkQueueImpl;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

@Namespaces({"http://cxf.apache.org/blueprint/core",
             "http://cxf.apache.org/configuration/beans",
             "http://cxf.apache.org/configuration/parameterized-types",
             "http://cxf.apache.org/configuration/security",
             "http://schemas.xmlsoap.org/wsdl/",
             "http://www.w3.org/2005/08/addressing",
             "http://schemas.xmlsoap.org/ws/2004/08/addressing"})
public class CXFAPINamespaceHandler implements NamespaceHandler {

    public URL getSchemaLocation(String namespace) {
        String location = null;

        // when schema is being resolved for custom namespace elements, "namespace" is real namespace
        // (from xmlns:prefix="<namespace>"
        // but when namespace is <xsd:import>ed, aries/xerces uses systemID (schemaLocation)

        if ("http://cxf.apache.org/configuration/beans".equals(namespace)
                || "http://cxf.apache.org/schemas/configuration/cxf-beans.xsd".equals(namespace)) {
            location = "schemas/configuration/cxf-beans.xsd";
        } else if ("http://cxf.apache.org/configuration/parameterized-types".equals(namespace)
                || "http://cxf.apache.org/schemas/configuration/parameterized-types.xsd".equals(namespace)) {
            location = "schemas/configuration/parameterized-types.xsd";
        } else if ("http://cxf.apache.org/configuration/security".equals(namespace)
                || "http://cxf.apache.org/schemas/configuration/security.xsd".equals(namespace)) {
            location = "schemas/configuration/security.xsd";
        } else if ("http://schemas.xmlsoap.org/wsdl/".equals(namespace)
                || "http://schemas.xmlsoap.org/wsdl/2003-02-11.xsd".equals(namespace)) {
            location = "schemas/wsdl/wsdl.xsd";
        } else if ("http://www.w3.org/2005/08/addressing".equals(namespace)
                || "http://www.w3.org/2006/03/addressing/ws-addr.xsd".equals(namespace)) {
            location = "schemas/wsdl/ws-addr.xsd";
        } else if ("http://schemas.xmlsoap.org/ws/2004/08/addressing".equals(namespace)) {
            location = "schemas/wsdl/addressing.xsd";
        } else if ("http://cxf.apache.org/blueprint/core".equals(namespace)) {
            location = "schemas/blueprint/core.xsd";
        }
        if (location != null) {
            return getClass().getClassLoader().getResource(location);
        }
        return null;
    }


    @SuppressWarnings("deprecation")
    public Metadata parse(Element element, ParserContext context) {
        String s = element.getLocalName();
        if ("bus".equals(s)) {
            //parse bus
            return new BusDefinitionParser().parse(element, context);
        } else if ("logging".equals(s)) {
            //logging feature
            return new SimpleBPBeanDefinitionParser(org.apache.cxf.feature.LoggingFeature.class)
                .parse(element, context);
        } else if ("fastinfoset".equals(s)) {
            //fastinfosetfeature
            return new SimpleBPBeanDefinitionParser(FastInfosetFeature.class).parse(element, context);
        } else if ("workqueue".equals(s)) {
            return new SimpleBPBeanDefinitionParser(AutomaticWorkQueueImpl.class) {
                
                @Override
                public String getId(Element element, ParserContext context) {
                    String id = element.hasAttribute("id") ? element.getAttribute("id") : null;
                    if (id == null) {
                        id = "cxf.workqueue.";
                        id += element.hasAttribute("name") ? element.getAttribute("name") : "def";
                    }
                    return id;
                }

                @Override
                protected void processNameAttribute(Element element, ParserContext ctx,
                                                    MutableBeanMetadata bean, String val) {
                    bean.addProperty("name", createValue(ctx, val));
                }
            } .parse(element, context);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Set<Class> getManagedClasses() {
        //probably should have the various stuff in cxf-api in here?
        return null;
    }
    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        return null;
    }

}
