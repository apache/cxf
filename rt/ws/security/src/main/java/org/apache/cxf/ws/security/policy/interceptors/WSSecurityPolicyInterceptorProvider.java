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

package org.apache.cxf.ws.security.policy.interceptors;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.security.policy.SP11Constants;

/**
 * 
 */
public class WSSecurityPolicyInterceptorProvider extends AbstractPolicyInterceptorProvider {
    private static final Collection<QName> ASSERTION_TYPES;
    static {
        ASSERTION_TYPES = new ArrayList<QName>();
        ASSERTION_TYPES.add(SP11Constants.TRANSPORT_BINDING);
        ASSERTION_TYPES.add(SP11Constants.TRANSPORT_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.LAYOUT);
        ASSERTION_TYPES.add(SP11Constants.INCLUDE_TIMESTAMP);
        ASSERTION_TYPES.add(SP11Constants.ALGORITHM_SUITE);
        ASSERTION_TYPES.add(SP11Constants.WSS10);
        ASSERTION_TYPES.add(SP11Constants.WSS11);
        ASSERTION_TYPES.add(SP11Constants.SIGNED_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP11Constants.USERNAME_TOKEN);
        
        
    }

    public WSSecurityPolicyInterceptorProvider() {
        super(ASSERTION_TYPES);
    }
}
