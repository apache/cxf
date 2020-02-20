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

package org.apache.cxf.jaxrs.blueprint;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.aries.blueprint.Namespaces;
import org.apache.aries.blueprint.ParserContext;
import org.apache.cxf.helpers.BaseNamespaceHandler;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.staxutils.transform.OutTransformWriter;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

@Namespaces("http://cxf.apache.org/blueprint/jaxrs")
public class JAXRSBPNamespaceHandler extends BaseNamespaceHandler {
    private BlueprintContainer blueprintContainer;

    public JAXRSBPNamespaceHandler() {
    }

    public URL getSchemaLocation(String namespace) {
        if ("http://cxf.apache.org/blueprint/jaxrs".equals(namespace)
                || "http://cxf.apache.org/schemas/jaxrs.xsd".equals(namespace)) {
            return getClass().getClassLoader().getResource("schemas/blueprint/jaxrs.xsd");
        } else if ("http://cxf.apache.org/schemas/jaxrs-common.xsd".equals(namespace)) {
            return getClass().getClassLoader().getResource("schemas/blueprint/jaxrs-common.xsd");
        }
        return super.findCoreSchemaLocation(namespace);
    }


    public Metadata parse(Element element, ParserContext context) {
        String s = element.getLocalName();
        if ("server".equals(s)) {
            return new JAXRSServerFactoryBeanDefinitionParser().parse(element, context);
        } else if ("client".equals(s)) {
            return context.parseElement(Metadata.class, null, transformElement(element));
        } else {
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    public Set<Class> getManagedClasses() {
        return null;
    }
    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        return null;
    }

    private Element transformElement(Element element) {
        final Map<String, String> transformMap =
            Collections.singletonMap("{" + element.getNamespaceURI() + "}*",
                                     "{http://cxf.apache.org/blueprint/jaxrs-client}*");


        W3CDOMStreamWriter domWriter = new W3CDOMStreamWriter();
        OutTransformWriter transformWriter = new OutTransformWriter(domWriter, transformMap);
        try {
            StaxUtils.copy(element, transformWriter);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return domWriter.getDocument().getDocumentElement();
    }
    public BlueprintContainer getBlueprintContainer() {
        return blueprintContainer;
    }

    public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

}
