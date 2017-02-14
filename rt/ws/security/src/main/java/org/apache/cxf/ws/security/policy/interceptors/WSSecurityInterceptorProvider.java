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
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JStaxInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JStaxOutInterceptor;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;

/**
 *
 */
public class WSSecurityInterceptorProvider extends AbstractPolicyInterceptorProvider {
    private static final long serialVersionUID = -6222118542914666817L;
    private static final Collection<QName> ASSERTION_TYPES;
    static {
        ASSERTION_TYPES = new ArrayList<>();

        ASSERTION_TYPES.add(SP12Constants.TRANSPORT_BINDING);
        ASSERTION_TYPES.add(SP12Constants.ASYMMETRIC_BINDING);
        ASSERTION_TYPES.add(SP12Constants.SYMMETRIC_BINDING);
        ASSERTION_TYPES.add(SP12Constants.SIGNED_PARTS);

        ASSERTION_TYPES.add(SP11Constants.TRANSPORT_BINDING);
        ASSERTION_TYPES.add(SP11Constants.ASYMMETRIC_BINDING);
        ASSERTION_TYPES.add(SP11Constants.SYMMETRIC_BINDING);
        ASSERTION_TYPES.add(SP11Constants.SIGNED_PARTS);
    }

    public WSSecurityInterceptorProvider() {
        super(ASSERTION_TYPES);

        PolicyBasedWSS4JInInterceptor in = new PolicyBasedWSS4JInInterceptor();
        this.getOutInterceptors().add(PolicyBasedWSS4JOutInterceptor.INSTANCE);
        this.getOutFaultInterceptors().add(PolicyBasedWSS4JOutInterceptor.INSTANCE);
        this.getInInterceptors().add(in);
        this.getInFaultInterceptors().add(in);


        PolicyBasedWSS4JStaxOutInterceptor so = new PolicyBasedWSS4JStaxOutInterceptor();
        PolicyBasedWSS4JStaxInInterceptor si = new PolicyBasedWSS4JStaxInInterceptor();
        this.getOutInterceptors().add(so);
        this.getOutFaultInterceptors().add(so);
        this.getInInterceptors().add(si);
        this.getInFaultInterceptors().add(si);
    }
}
