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

package org.apache.cxf.transport.http.policy;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;

/**
 * This no-op PolicyInterceptorProvider prevents the policy enforcement
 * logic making it impossible to assert the HTTPClientPolicy upfront
 * before the HTTPConduit becomes invoved (e.g. via a WSPolicyFeature
 * applied to the <jawx:client> bean).
 */
@NoJSR250Annotations
public class NoOpPolicyInterceptorProvider
    extends AbstractPolicyInterceptorProvider {

    private static final Collection<QName> ASSERTION_TYPES;
    private static final QName HTTP_CONF_NAME =
        new QName("http://cxf.apache.org/transports/http/configuration", "client");
    
    static {
        Collection<QName> types = new ArrayList<QName>();
        types.add(HTTP_CONF_NAME);
        ASSERTION_TYPES = types;
    }
    
    public NoOpPolicyInterceptorProvider() {
        super(ASSERTION_TYPES);
    }
}
