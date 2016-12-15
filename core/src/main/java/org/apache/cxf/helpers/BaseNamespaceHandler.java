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

package org.apache.cxf.helpers;

import java.net.URL;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.cxf.internal.CXFAPINamespaceHandler;

public abstract class BaseNamespaceHandler implements NamespaceHandler {

    private NamespaceHandler cxfApiNamespaceHandler = new CXFAPINamespaceHandler();

    /**
     * If namespace handler's schema imports other schemas from cxf-core bundle, this method
     * may be used to delegate to <code>CXFAPINamespaceHandler</code> to resolve imported schema.
     * @param namespace
     * @return if namespace may be resolved by CXFAPINamespaceHandler valid URL is returned. Otherwise
     * returns <code>null</code>
     */
    protected URL findCoreSchemaLocation(String namespace) {
        return cxfApiNamespaceHandler.getSchemaLocation(namespace);
    }

}
