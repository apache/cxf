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
package org.apache.cxf.ws.rm.blueprint;

import java.net.URL;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.aries.blueprint.ParserContext;
import org.apache.cxf.helpers.BaseNamespaceHandler;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.feature.RMFeature;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

public class RMBPHandler extends BaseNamespaceHandler {

    public URL getSchemaLocation(String s) {
        if ("http://cxf.apache.org/ws/rm/manager".equals(s)) {
            return getClass().getClassLoader().
                getResource("schemas/configuration/wsrm-manager.xsd");
        } else if ("http://schemas.xmlsoap.org/ws/2005/02/rm/policy".equals(s)) {
            return getClass().getClassLoader().
                getResource("schemas/configuration/wsrm-policy.xsd");
        } else if ("http://docs.oasis-open.org/ws-rx/wsrmp/200702".equals(s)) {
            return getClass().getClassLoader().
                getResource("schemas/configuration/wsrmp-1.1-schema-200702.xsd");
        }
        return super.findCoreSchemaLocation(s);
    }

    @SuppressWarnings("rawtypes")
    public Set<Class> getManagedClasses() {
        return null;
    }

    public Metadata parse(Element element, ParserContext context) {
        String s = element.getLocalName();
        if ("reliableMessaging".equals(s)) {
            return new RMBPBeanDefinitionParser(RMFeature.class).parse(element, context);
        } else if ("rmManager".equals(s)) {
            return new RMBPBeanDefinitionParser(RMManager.class).parse(element, context);
        } else if ("jdbcStore".equals(s)) {
            return new RMBPTxStoreBeanDefinitionParser().parse(element, context);
        }

        return null;
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata componentMetadata, ParserContext context) {
        return null;
    }
}
