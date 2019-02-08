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
package org.apache.cxf.ws.transfer.dialect.fragment.language;

import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType;

/**
 * Interface for FragmentDialect languages.
 */
public interface FragmentDialectLanguage {

    /**
     * Returns fragment of resource, which is described by expression.
     * @param representation Resource, from which is fragment computed.
     * @param expression Expression written in the language.
     * @return It can return org.w3c.dom.Node | org.w3c.dom.NodeList | String
     */
    Object getResourceFragment(Representation representation, ExpressionType expression);

}
