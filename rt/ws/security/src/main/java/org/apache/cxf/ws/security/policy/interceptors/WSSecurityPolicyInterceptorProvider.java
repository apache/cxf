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
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;

/**
 *
 */
public class WSSecurityPolicyInterceptorProvider extends AbstractPolicyInterceptorProvider {
    private static final long serialVersionUID = 2092269997296804632L;
    private static final Collection<QName> ASSERTION_TYPES;
    static {
        ASSERTION_TYPES = new ArrayList<>();
        ASSERTION_TYPES.add(SP12Constants.LAYOUT);
        ASSERTION_TYPES.add(SP12Constants.INCLUDE_TIMESTAMP);
        ASSERTION_TYPES.add(SP12Constants.ALGORITHM_SUITE);
        ASSERTION_TYPES.add(SP12Constants.ENCRYPT_SIGNATURE);
        ASSERTION_TYPES.add(SP12Constants.PROTECT_TOKENS);
        ASSERTION_TYPES.add(SP12Constants.ENCRYPT_BEFORE_SIGNING);
        ASSERTION_TYPES.add(SP12Constants.SIGN_BEFORE_ENCRYPTING);
        ASSERTION_TYPES.add(SP12Constants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY);
        ASSERTION_TYPES.add(SP12Constants.WSS10);
        ASSERTION_TYPES.add(SP12Constants.WSS11);
        ASSERTION_TYPES.add(SP12Constants.TRUST_13);
        ASSERTION_TYPES.add(SP12Constants.PROTECTION_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.X509_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.ENCRYPTION_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.SIGNATURE_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.TRANSPORT_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.INITIATOR_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.INITIATOR_SIGNATURE_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.INITIATOR_ENCRYPTION_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.RECIPIENT_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.RECIPIENT_SIGNATURE_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.RECIPIENT_ENCRYPTION_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.SIGNED_PARTS);
        ASSERTION_TYPES.add(SP12Constants.REQUIRED_PARTS);
        ASSERTION_TYPES.add(SP12Constants.REQUIRED_ELEMENTS);
        ASSERTION_TYPES.add(SP12Constants.ENCRYPTED_PARTS);
        ASSERTION_TYPES.add(SP12Constants.ENCRYPTED_ELEMENTS);
        ASSERTION_TYPES.add(SP12Constants.SIGNED_ELEMENTS);
        ASSERTION_TYPES.add(SP12Constants.CONTENT_ENCRYPTED_ELEMENTS);
        ASSERTION_TYPES.add(SP12Constants.SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP12Constants.SIGNED_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP12Constants.ENDORSING_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP12Constants.ENCRYPTED_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP12Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP12Constants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP12Constants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP12Constants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);

        ASSERTION_TYPES.add(SP11Constants.LAYOUT);
        ASSERTION_TYPES.add(SP11Constants.INCLUDE_TIMESTAMP);
        ASSERTION_TYPES.add(SP11Constants.ALGORITHM_SUITE);
        ASSERTION_TYPES.add(SP11Constants.ENCRYPT_SIGNATURE);
        ASSERTION_TYPES.add(SP11Constants.PROTECT_TOKENS);
        ASSERTION_TYPES.add(SP11Constants.ENCRYPT_BEFORE_SIGNING);
        ASSERTION_TYPES.add(SP11Constants.SIGN_BEFORE_ENCRYPTING);
        ASSERTION_TYPES.add(SP11Constants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY);
        ASSERTION_TYPES.add(SP11Constants.WSS10);
        ASSERTION_TYPES.add(SP11Constants.WSS11);
        ASSERTION_TYPES.add(SP11Constants.TRUST_10);
        ASSERTION_TYPES.add(SP11Constants.PROTECTION_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.X509_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.ENCRYPTION_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.SIGNATURE_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.TRANSPORT_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.INITIATOR_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.INITIATOR_SIGNATURE_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.INITIATOR_ENCRYPTION_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.RECIPIENT_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.RECIPIENT_SIGNATURE_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.RECIPIENT_ENCRYPTION_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.SIGNED_PARTS);
        ASSERTION_TYPES.add(SP11Constants.REQUIRED_PARTS);
        ASSERTION_TYPES.add(SP11Constants.REQUIRED_ELEMENTS);
        ASSERTION_TYPES.add(SP11Constants.ENCRYPTED_PARTS);
        ASSERTION_TYPES.add(SP11Constants.ENCRYPTED_ELEMENTS);
        ASSERTION_TYPES.add(SP11Constants.SIGNED_ELEMENTS);
        ASSERTION_TYPES.add(SP11Constants.CONTENT_ENCRYPTED_ELEMENTS);
        ASSERTION_TYPES.add(SP11Constants.SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP11Constants.SIGNED_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP11Constants.ENDORSING_SUPPORTING_TOKENS);
        ASSERTION_TYPES.add(SP11Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
    }

    public WSSecurityPolicyInterceptorProvider() {
        super(ASSERTION_TYPES);
        getOutInterceptors().add(SecurityVerificationOutInterceptor.INSTANCE);
    }
}
