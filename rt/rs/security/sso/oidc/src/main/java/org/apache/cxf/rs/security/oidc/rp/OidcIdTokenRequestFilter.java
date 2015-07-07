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

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oidc.common.IdToken;

public class OidcIdTokenRequestFilter implements ContainerRequestFilter {
    private String tokenFormParameter = "idtoken"; 
    private IdTokenReader idTokenReader;
    private Consumer consumer;
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        MultivaluedMap<String, String> form = toFormData(requestContext);
        String idTokenParamValue = form.getFirst(tokenFormParameter);
        if (idTokenParamValue == null) {
            requestContext.abortWith(Response.status(401).build());
            return;
        }
        
        IdToken idToken = idTokenReader.getIdToken(idTokenParamValue, consumer.getKey());
        JAXRSUtils.getCurrentMessage().setContent(IdToken.class, idToken);
        requestContext.setSecurityContext(new OidcSecurityContext(idToken));
        
    }
    private MultivaluedMap<String, String> toFormData(ContainerRequestContext rc) {
        MultivaluedMap<String, String> requestState = new MetadataMap<String, String>();
        if (MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(rc.getMediaType())) {
            String body = FormUtils.readBody(rc.getEntityStream(), "UTF-8");
            FormUtils.populateMapFromString(requestState, JAXRSUtils.getCurrentMessage(), body, 
                                            "UTF-8", false);
        }
        return requestState;
    }
    public void setIdTokenReader(IdTokenReader idTokenReader) {
        this.idTokenReader = idTokenReader;
    }
    public void setTokenFormParameter(String tokenFormParameter) {
        this.tokenFormParameter = tokenFormParameter;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }
}
