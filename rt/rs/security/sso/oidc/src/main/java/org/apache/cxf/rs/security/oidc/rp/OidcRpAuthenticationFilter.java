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
package org.apache.cxf.rs.security.oidc.rp;

import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContext;

@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class OidcRpAuthenticationFilter implements ContainerRequestFilter {
    
    private OidcRpStateManager stateManager;
    private String rpServiceAddress;
    
    public void filter(ContainerRequestContext rc) {
        if (checkSecurityContext(rc)) {
            return;
        } else {
            UriBuilder ub = rc.getUriInfo().getBaseUriBuilder().path(rpServiceAddress);
            rc.abortWith(Response.seeOther(ub.build())
                           .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store")
                           .header("Pragma", "no-cache") 
                           .build());
        }
    }
    protected boolean checkSecurityContext(ContainerRequestContext rc) {
        Map<String, Cookie> cookies = rc.getCookies();
        
        Cookie securityContextCookie = cookies.get("org.apache.cxf.websso.context");
        if (securityContextCookie == null) {
            return false;
        }
        String contextKey = securityContextCookie.getValue();
        
        OidcClientTokenContext tokenContext = stateManager.getTokenContext(contextKey);
        if (tokenContext == null) {
            return false;
        }
        OidcClientTokenContextImpl newTokenContext = new OidcClientTokenContextImpl();
        newTokenContext.setToken(tokenContext.getToken());
        newTokenContext.setIdToken(tokenContext.getIdToken());
        newTokenContext.setUserInfo(tokenContext.getUserInfo());
        newTokenContext.setState(toRequestState(rc));
        JAXRSUtils.getCurrentMessage().setContent(ClientTokenContext.class, newTokenContext);
        rc.setSecurityContext(new OidcSecurityContext(newTokenContext));
        return true;
    }
    private MultivaluedMap<String, String> toRequestState(ContainerRequestContext rc) {
        MultivaluedMap<String, String> requestState = new MetadataMap<String, String>();
        requestState.putAll(rc.getUriInfo().getQueryParameters(true));
        if (MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(rc.getMediaType())) {
            String body = FormUtils.readBody(rc.getEntityStream(), "UTF-8");
            FormUtils.populateMapFromString(requestState, JAXRSUtils.getCurrentMessage(), body, 
                                            "UTF-8", true);
        }
        return requestState;
    }
    public void setRpServiceAddress(String rpServiceAddress) {
        this.rpServiceAddress = rpServiceAddress;
    }
    public void setStateManager(OidcRpStateManager stateManager) {
        this.stateManager = stateManager;
    }
}
