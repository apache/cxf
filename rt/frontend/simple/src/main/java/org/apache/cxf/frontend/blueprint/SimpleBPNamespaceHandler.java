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

package org.apache.cxf.frontend.blueprint;

import java.net.URL;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.aries.blueprint.Namespaces;
import org.apache.aries.blueprint.ParserContext;
import org.apache.cxf.helpers.BaseNamespaceHandler;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

@Namespaces("http://cxf.apache.org/blueprint/simple")
public class SimpleBPNamespaceHandler extends BaseNamespaceHandler {
    private BlueprintContainer blueprintContainer;

    public SimpleBPNamespaceHandler() {
    }

    public URL getSchemaLocation(String namespace) {
        if ("http://cxf.apache.org/blueprint/simple".equals(namespace)) {
            return getClass().getClassLoader().getResource("schemas/blueprint/simple.xsd");
        }
        return super.findCoreSchemaLocation(namespace);
    }


    public Metadata parse(Element element, ParserContext context) {
        String s = element.getLocalName();
        if ("server".equals(s)) {
            return new ServerFactoryBeanDefinitionParser().parse(element, context);
        } else if ("client".equals(s)) {
            return new ClientProxyFactoryBeanDefinitionParser().parse(element, context);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Set<Class> getManagedClasses() {
        return null;
    }
    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        return null;
    }


    public BlueprintContainer getBlueprintContainer() {
        return blueprintContainer;
    }

    public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

}
