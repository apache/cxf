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
import org.apache.cxf.ws.security.policy.SP12Constants;

/**
 * 
 */
public class WSSecurityPolicyInterceptorProvider extends AbstractPolicyInterceptorProvider {
    private static final Collection<QName> ASSERTION_TYPES;
    static {
        ASSERTION_TYPES = new ArrayList<QName>();
        ASSERTION_TYPES.add(SP12Constants.LAYOUT);
        ASSERTION_TYPES.add(SP12Constants.INCLUDE_TIMESTAMP);
        ASSERTION_TYPES.add(SP12Constants.ALGORITHM_SUITE);
        ASSERTION_TYPES.add(SP12Constants.WSS10);
        ASSERTION_TYPES.add(SP12Constants.WSS11);
        ASSERTION_TYPES.add(SP11Constants.TRUST_10);
        ASSERTION_TYPES.add(SP12Constants.TRUST_13);
        ASSERTION_TYPES.add(SP12Constants.PROTECTION_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.X509_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.ENCRYPTION_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.SIGNATURE_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.USERNAME_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.TRANSPORT_TOKEN);            
        ASSERTION_TYPES.add(SP12Constants.SIGNED_PARTS);
        ASSERTION_TYPES.add(SP12Constants.ENCRYPTED_PARTS);
        ASSERTION_TYPES.add(SP12Constants.INSTANCE.getSupportingTokens());
        ASSERTION_TYPES.add(SP12Constants.INSTANCE.getSignedSupportingTokens());
        ASSERTION_TYPES.add(SP12Constants.INSTANCE.getEndorsingSupportingTokens());
        ASSERTION_TYPES.add(SP12Constants.INSTANCE.getSignedEndorsingSupportingTokens());
        ASSERTION_TYPES.add(SP12Constants.ENCRYPTED_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP12Constants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP12Constants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP12Constants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
    }

    public WSSecurityPolicyInterceptorProvider() {
        super(ASSERTION_TYPES);
    }
}
