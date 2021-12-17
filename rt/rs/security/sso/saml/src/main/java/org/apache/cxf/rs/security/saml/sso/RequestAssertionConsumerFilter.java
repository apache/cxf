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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class RequestAssertionConsumerFilter extends AbstractRequestAssertionConsumerHandler
    implements ContainerRequestFilter {

    private boolean supportPostBinding;

    @Override
    public void filter(ContainerRequestContext ct) throws IOException {
        String httpMethod = ct.getMethod();
        if (HttpMethod.GET.equals(httpMethod) && !supportPostBinding) {
            MultivaluedMap<String, String> params = ct.getUriInfo().getQueryParameters();
            processParams(ct, params, false);
        } else if (HttpMethod.POST.equals(httpMethod)
            && supportPostBinding
            && MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(ct.getMediaType())) {
            String strForm = IOUtils.toString(ct.getEntityStream());
            MultivaluedMap<String, String> params = JAXRSUtils.getStructuredParams(strForm, "&", false, false);
            if (!processParams(ct, params, true)) {
                // restore the stream
                ct.setEntityStream(new ByteArrayInputStream(strForm.getBytes()));
            }
        }

    }

    protected boolean processParams(ContainerRequestContext ct,
                                 MultivaluedMap<String, String> params,
                                 boolean postBinding) {
        String encodedSamlResponse = params.getFirst(SSOConstants.SAML_RESPONSE);
        String relayState = params.getFirst(SSOConstants.RELAY_STATE);
        if (relayState == null && encodedSamlResponse == null) {
            // initial redirect to IDP has not happened yet, let the SAML authentication filter do it
            JAXRSUtils.getCurrentMessage().put(SSOConstants.RACS_IS_COLLOCATED, Boolean.TRUE);
            return false;
        }
        ct.abortWith(doProcessSamlResponse(encodedSamlResponse, relayState, postBinding));
        return true;
    }
    public void setSupportPostBinding(boolean supportPostBinding) {
        this.supportPostBinding = supportPostBinding;
    }

}
