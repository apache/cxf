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
package org.apache.cxf.rs.security.saml.sso;

import java.io.IOException;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.saml.sso.state.RequestState;

@PreMatching
@BindingPriority(BindingPriority.AUTHENTICATION)
public class RequestAssertionConsumerFilter extends AbstractRequestAssertionConsumerHandler 
    implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext ct) throws IOException {
        String httpMethod = ct.getMethod();
        if (HttpMethod.GET.equals(httpMethod)) {
            MultivaluedMap<String, String> params = ct.getUriInfo().getQueryParameters();
            processParams(ct, params, false);
        } else if (HttpMethod.POST.equals(httpMethod) 
            && MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(ct.getMediaType())) {
            String strForm = IOUtils.toString(ct.getEntityStream());
            MultivaluedMap<String, String> params = JAXRSUtils.getStructuredParams(strForm, "&", false, false);
            processParams(ct, params, true);
        } else {
            ct.abortWith(Response.status(400).build());
        }
        
    }
    
    protected void processParams(ContainerRequestContext ct,
                                 MultivaluedMap<String, String> params, 
                                 boolean postBinding) {
        String encodedSamlResponse = params.getFirst(SSOConstants.SAML_RESPONSE);
        String relayState = params.getFirst(SSOConstants.RELAY_STATE); 
        RequestState requestState = processRelayState(relayState);
        String targetUri = requestState.getTargetAddress();
        if (targetUri != null 
            && targetUri.startsWith(ct.getUriInfo().getRequestUri().toString())) {
            reportError("INVALID_TARGET_URI");
            ct.abortWith(Response.status(400).build());
            return;
        }
            
        
        String contextCookie = createSecurityContext(requestState,
                                                     encodedSamlResponse,
                                                     relayState,
                                                     postBinding);
        ct.getHeaders().add(HttpHeaders.COOKIE, contextCookie);
        
    }
    
}
