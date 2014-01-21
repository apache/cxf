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
import org.apache.aries.blueprint.ParserContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

/**
 * 
 */
public class CXFAPINamespaceHandler implements NamespaceHandler {    
    
    public URL getSchemaLocation(String namespace) {
        String location = null;

        if ("http://cxf.apache.org/configuration/beans".equals(namespace)) {
            location = "schemas/configuration/cxf-beans.xsd";
        } else if ("http://cxf.apache.org/configuration/parameterized-types".equals(namespace)) {
            location = "schemas/configuration/parameterized-types.xsd";
        } else if ("http://cxf.apache.org/configuration/security".equals(namespace)) {
            location = "schemas/configuration/security.xsd";
        } else if ("http://schemas.xmlsoap.org/wsdl/".equals(namespace)) {
            location = "schemas/wsdl/wsdl.xsd";
        } else if ("http://www.w3.org/2005/08/addressing".equals(namespace)) {
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


    public Metadata parse(Element element, ParserContext context) {
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
