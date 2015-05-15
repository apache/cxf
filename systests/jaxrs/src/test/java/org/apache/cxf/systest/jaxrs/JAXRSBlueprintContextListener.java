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
package org.apache.cxf.systest.jaxrs;

import java.net.URI;

import org.apache.aries.blueprint.container.SimpleNamespaceHandlerSet;
import org.apache.aries.blueprint.parser.NamespaceHandlerSet;
import org.apache.aries.blueprint.web.BlueprintContextListener;
import org.apache.cxf.internal.CXFAPINamespaceHandler;
import org.apache.cxf.jaxrs.blueprint.JAXRSBPNamespaceHandler;

public class JAXRSBlueprintContextListener extends BlueprintContextListener {
    @Override
    protected NamespaceHandlerSet getNamespaceHandlerSet(ClassLoader tccl) {
        SimpleNamespaceHandlerSet set = new SimpleNamespaceHandlerSet();
        
        set.addNamespace(URI.create("http://cxf.apache.org/blueprint/core"),
                         getClass().getResource("/schemas/blueprint/core.xsd"),
                         new CXFAPINamespaceHandler());
        set.addNamespace(URI.create("http://cxf.apache.org/blueprint/jaxrs"),
                         getClass().getResource("/schemas/blueprint/jaxrs.xsd"),
                         new JAXRSBPNamespaceHandler());
        return set;
    }
}
