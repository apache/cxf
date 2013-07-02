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

import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JStaxInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JStaxOutInterceptor;
import org.apache.cxf.ws.security.wss4j.UsernameTokenInterceptor;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;

/**
 * 
 */
public class UsernameTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {
    private static final long serialVersionUID = -1507727324874727254L;
    private static final Collection<QName> ASSERTION_TYPES;
    static {
        ASSERTION_TYPES = new ArrayList<QName>();
        
        ASSERTION_TYPES.add(SP12Constants.USERNAME_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.USERNAME_TOKEN);
    }

    public UsernameTokenInterceptorProvider() {
        this(new UsernameTokenInterceptor());
    }
    
    public UsernameTokenInterceptorProvider(Bus bus) {
        this((UsernameTokenInterceptor)
             bus.getProperty("org.apache.cxf.ws.security.usernametoken.interceptor"));
    }
    
    public UsernameTokenInterceptorProvider(UsernameTokenInterceptor inInterceptor) {
        super(ASSERTION_TYPES);
        this.getOutInterceptors().add(new UsernameTokenInterceptor());
        this.getInInterceptors().add(inInterceptor == null ? new UsernameTokenInterceptor() : inInterceptor);
        //not needed on fault chains
        
        this.getOutInterceptors().add(PolicyBasedWSS4JStaxOutInterceptor.INSTANCE);
        this.getOutFaultInterceptors().add(PolicyBasedWSS4JStaxOutInterceptor.INSTANCE);
        this.getInInterceptors().add(PolicyBasedWSS4JStaxInInterceptor.INSTANCE);
        this.getInFaultInterceptors().add(PolicyBasedWSS4JStaxInInterceptor.INSTANCE);
    }
    
}
