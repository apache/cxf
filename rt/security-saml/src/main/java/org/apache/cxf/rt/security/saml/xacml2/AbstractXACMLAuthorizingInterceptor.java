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

package org.apache.cxf.rt.security.saml.xacml2;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.LoginSecurityContext;
import org.apache.cxf.security.SecurityContext;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.opensaml.xacml.ctx.DecisionType.DECISION;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResponseType;
import org.opensaml.xacml.ctx.ResultType;
import org.opensaml.xacml.ctx.StatusType;

/**
 * An interceptor to perform an XACML 2.0 authorization request to a remote PDP using OpenSAML,
 * and make an authorization decision based on the response. It takes the principal and roles
 * from the SecurityContext, and uses the XACMLRequestBuilder to construct an XACML Request
 * statement. How the actual PDP invocation is made is up to a subclass.
 */
public abstract class AbstractXACMLAuthorizingInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractXACMLAuthorizingInterceptor.class);

    private XACMLRequestBuilder requestBuilder = new DefaultXACMLRequestBuilder();

    public AbstractXACMLAuthorizingInterceptor() {
        super(Phase.PRE_INVOKE);
        org.apache.wss4j.common.saml.OpenSAMLUtil.initSamlEngine();
    }

    public void handleMessage(Message message) throws Fault {
        SecurityContext sc = message.get(SecurityContext.class);

        if (sc instanceof LoginSecurityContext) {
            Principal principal = sc.getUserPrincipal();
            String principalName = null;
            if (principal != null) {
                principalName = principal.getName();
            }

            LoginSecurityContext loginSecurityContext = (LoginSecurityContext)sc;
            Set<Principal> principalRoles = loginSecurityContext.getUserRoles();
            List<String> roles = new ArrayList<>();
            if (principalRoles != null) {
                for (Principal p : principalRoles) {
                    if (p != null && p.getName() != null && !p.getName().equals(principalName)) {
                        roles.add(p.getName());
                    }
                }
            }

            try {
                if (authorize(principal, roles, message)) {
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
    protected boolean authorize(
        Principal principal, List<String> roles, Message message
    ) throws Exception {
        RequestType request = requestBuilder.createRequest(principal, roles, message);
        if (LOG.isLoggable(Level.FINE)) {
            Document doc = DOMUtils.createDocument();
            Element requestElement = OpenSAMLUtil.toDom(request, doc);
            LOG.log(Level.FINE, DOM2Writer.nodeToString(requestElement));
        }

        ResponseType response = performRequest(request, message);

        List<ResultType> results = response.getResults();

        if (results == null) {
            return false;
        }

        for (ResultType result : results) {
            // Handle any Obligations returned by the PDP
            handleObligations(request, principal, message, result);

            DECISION decision = result.getDecision() != null ? result.getDecision().getDecision() : DECISION.Deny;
            String code = "";
            String statusMessage = "";
            if (result.getStatus() != null) {
                StatusType status = result.getStatus();
                code = status.getStatusCode() != null ? status.getStatusCode().getValue() : "";
                statusMessage = status.getStatusMessage() != null ? status.getStatusMessage().getValue() : "";
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("XACML authorization result: " + decision + ", code: " + code + ", message: " + statusMessage);
            }
            return decision == DECISION.Permit;
        }

        return false;
    }

    /**
     * Handle any Obligations returned by the PDP
     */
    protected void handleObligations(
        RequestType request,
        Principal principal,
        Message message,
        ResultType result
    ) throws Exception {
        // Do nothing by default
    }

    protected abstract ResponseType performRequest(RequestType request, Message message) throws Exception;

}
