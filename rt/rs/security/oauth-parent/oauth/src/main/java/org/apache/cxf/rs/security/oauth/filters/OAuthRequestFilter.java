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
package org.apache.cxf.rs.security.oauth.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth.data.OAuthContext;
import org.apache.cxf.security.SecurityContext;

/**
 * JAX-RS OAuth filter which can be used to protect end user endpoints
 */
@Provider
@PreMatching
public class OAuthRequestFilter extends AbstractAuthFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext context) {
        try {
            Message m = JAXRSUtils.getCurrentMessage();
            MessageContext mc = new MessageContextImpl(m);
            OAuthInfo info = handleOAuthRequest(mc.getHttpServletRequest());
            setSecurityContext(mc, m, info);

        } catch (Exception e) {
            context.abortWith(Response.status(401).header("WWW-Authenticate", "OAuth").build());
        }
    }

    private void setSecurityContext(MessageContext mc, Message m, OAuthInfo info) {

        SecurityContext sc = createSecurityContext(mc.getHttpServletRequest(), info);
        m.setContent(SecurityContext.class, sc);
        m.setContent(OAuthContext.class, createOAuthContext(info));

    }
}
