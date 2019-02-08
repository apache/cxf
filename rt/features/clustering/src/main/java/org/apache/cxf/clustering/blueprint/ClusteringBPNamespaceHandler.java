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
package org.apache.cxf.clustering.blueprint;

import java.net.URL;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.aries.blueprint.Namespaces;
import org.apache.aries.blueprint.ParserContext;
import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.LoadDistributorFeature;
import org.apache.cxf.clustering.circuitbreaker.CircuitBreakerFailoverFeature;
import org.apache.cxf.helpers.BaseNamespaceHandler;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

@Namespaces("http://cxf.apache.org/clustering")
public class ClusteringBPNamespaceHandler extends BaseNamespaceHandler {
    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        return null;
    }

    public Metadata parse(Element element, ParserContext context) {
        String s = element.getLocalName();
        if ("failover".equals(s)) {
            return new ClusteringBPBeanDefinitionParser(FailoverFeature.class).parse(element, context);
        } else if ("loadDistributor".equals(s)) {
            return new ClusteringBPBeanDefinitionParser(LoadDistributorFeature.class).parse(element, context);
        } else if ("circuit-breaker-failover".equals(s)) {
            return new ClusteringBPBeanDefinitionParser(CircuitBreakerFailoverFeature.class).parse(element, context);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Set<Class> getManagedClasses() {
        return null;
    }

    public URL getSchemaLocation(String namespace) {
        if ("http://cxf.apache.org/clustering".equals(namespace)) {
            return getClass().getClassLoader().getResource("schemas/clustering.xsd");
        }
        // delegate to cxf-core
        return super.findCoreSchemaLocation(namespace);
    }

}
