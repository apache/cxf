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

package org.apache.cxf.rt.security.saml.xacml.pep;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.LoginSecurityContext;
import org.apache.cxf.security.SecurityContext;


/**
 * An abstract interceptor to perform an XACML authorization request to a remote PDP,
 * and make an authorization decision based on the response. It takes the principal and roles
 * from the SecurityContext, and uses the XACMLRequestBuilder to construct an XACML Request
 * statement. 
 * 
 * This class must be subclassed to actually perform the request to the PDP and to parse
 * the response.
 */
public abstract class AbstractXACMLAuthorizingInterceptor extends AbstractPhaseInterceptor<Message> {
    
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractXACMLAuthorizingInterceptor.class);
    
    private XACMLRequestBuilder requestBuilder = new OpenSAMLXACMLRequestBuilder();
    
    public AbstractXACMLAuthorizingInterceptor() {
        super(Phase.PRE_INVOKE);
        org.apache.wss4j.common.saml.OpenSAMLUtil.initSamlEngine();
    }
    
    public void handleMessage(Message message) throws Fault {
        SecurityContext sc = message.get(SecurityContext.class);
        
        if (sc instanceof LoginSecurityContext) {
            Principal principal = sc.getUserPrincipal();
            
            LoginSecurityContext loginSecurityContext = (LoginSecurityContext)sc;
            Set<Principal> principalRoles = loginSecurityContext.getUserRoles();
            List<String> roles = new ArrayList<>();
            if (principalRoles != null) {
                for (Principal p : principalRoles) {
                    if (p != principal) {
                        roles.add(p.getName());
                    }
                }
            }
            
            try {
                Object request = requestBuilder.createRequest(principal, roles, message);
                
                if (authorize(request, principal, message)) {
                    return;
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Unauthorized: " + e.getMessage(), e);
                throw new AccessDeniedException("Unauthorized");
            }
        } else {
            LOG.log(
                Level.FINE,
                "The SecurityContext was not an instance of LoginSecurityContext. No authorization "
                + "is possible as a result"
            );
        }
        
        throw new AccessDeniedException("Unauthorized");
    }
    
    public XACMLRequestBuilder getRequestBuilder() {
        return requestBuilder;
    }

    public void setRequestBuilder(XACMLRequestBuilder requestBuilder) {
        this.requestBuilder = requestBuilder;
    }

    /**
     * Perform a (remote) authorization decision and return a boolean depending on the result
     */
    protected abstract boolean authorize(
        Object xacmlRequest, Principal principal, Message message
    ) throws Exception;
    
}
