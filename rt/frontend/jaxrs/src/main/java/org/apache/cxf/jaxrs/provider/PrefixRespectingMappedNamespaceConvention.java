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

package org.apache.cxf.jaxrs.provider;

import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;

/**
 * The MappedNamespaceConvention throws for any namespace that does not have a pre-configured
 * JSON prefix, even when the caller is supplying a perfectly usable prefix via XML.
 * This class overrides that behavior. 
 */
public class PrefixRespectingMappedNamespaceConvention extends MappedNamespaceConvention {

    public PrefixRespectingMappedNamespaceConvention(Configuration c) {
        super(c);
    }

    @Override
    public String createKey(String prefix, String ns, String local) {
        try {
            return super.createKey(prefix, ns, local);
        } catch (IllegalStateException ise) {
            StringBuilder builder = new StringBuilder();
            if (prefix != null && prefix.length() != 0) {
                builder.append(prefix).append('.');
            }
            return builder.append(local).toString();
        }
    }
}
