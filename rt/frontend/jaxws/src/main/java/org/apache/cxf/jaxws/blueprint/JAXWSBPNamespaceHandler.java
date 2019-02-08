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

import java.net.URL;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.aries.blueprint.Namespaces;
import org.apache.aries.blueprint.ParserContext;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.blueprint.ClientProxyFactoryBeanDefinitionParser;
import org.apache.cxf.frontend.blueprint.ServerFactoryBeanDefinitionParser;
import org.apache.cxf.helpers.BaseNamespaceHandler;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

@Namespaces("http://cxf.apache.org/blueprint/jaxws")
public class JAXWSBPNamespaceHandler extends BaseNamespaceHandler {
    private BlueprintContainer blueprintContainer;

    public JAXWSBPNamespaceHandler() {
    }

    public URL getSchemaLocation(String namespace) {
        if ("http://cxf.apache.org/blueprint/jaxws".equals(namespace)) {
            return getClass().getClassLoader().getResource("schemas/blueprint/jaxws.xsd");
        }
        return super.findCoreSchemaLocation(namespace);
    }


    public Metadata parse(Element element, ParserContext context) {
        String s = element.getLocalName();
        if ("endpoint".equals(s)) {
            return new EndpointDefinitionParser().parse(element, context);
        } else if ("server".equals(s)) {
            return new ServerFactoryBeanDefinitionParser(BPJaxWsServerFactoryBean.class)
                .parse(element, context);
        } else if ("client".equals(s)) {
            return new ClientProxyFactoryBeanDefinitionParser(JaxWsProxyFactoryBean.class)
                .parse(element, context);
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



    @NoJSR250Annotations
    public static class BPJaxWsServerFactoryBean extends JaxWsServerFactoryBean {

        private Server server;

        public BPJaxWsServerFactoryBean() {
            super();
        }
        public BPJaxWsServerFactoryBean(JaxWsServiceFactoryBean fact) {
            super(fact);
        }
        public Server getServer() {
            return server;
        }

        public void init() {
            create();
        }
        @Override
        public Server create() {
            if (server == null) {
                server = super.create();
            }
            return server;
        }
        public void destroy() {
            if (server != null) {
                server.destroy();
                server = null;
            }
        }
    }


}
