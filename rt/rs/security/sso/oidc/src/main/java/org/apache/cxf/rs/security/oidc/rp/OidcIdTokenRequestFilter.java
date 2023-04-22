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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oidc.common.IdToken;

public class OidcIdTokenRequestFilter implements ContainerRequestFilter {
    private String tokenFormParameter = "id_token";
    private IdTokenReader idTokenReader;
    private Consumer consumer;
    private String roleClaim;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        MultivaluedMap<String, String> form = toFormData(requestContext);
        String idTokenParamValue = form.getFirst(tokenFormParameter);
        if (idTokenParamValue == null) {
            requestContext.abortWith(Response.status(401).build());
            return;
        }

        IdToken idToken = idTokenReader.getIdToken(idTokenParamValue, consumer);
        JAXRSUtils.getCurrentMessage().setContent(IdToken.class, idToken);

        OidcSecurityContext oidcSecCtx = new OidcSecurityContext(idToken);
        oidcSecCtx.setRoleClaim(roleClaim);
        requestContext.setSecurityContext(oidcSecCtx);
    }

    private MultivaluedMap<String, String> toFormData(ContainerRequestContext rc) {
        MultivaluedMap<String, String> requestState = new MetadataMap<>();
        if (MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(rc.getMediaType())) {
            String body = FormUtils.readBody(rc.getEntityStream(), StandardCharsets.UTF_8.name());
            FormUtils.populateMapFromString(requestState, JAXRSUtils.getCurrentMessage(), body,
                                            StandardCharsets.UTF_8.name(), false);
            rc.setEntityStream(new ByteArrayInputStream(StringUtils.toBytesUTF8(body)));
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

    public void setRoleClaim(String roleClaim) {
        this.roleClaim = roleClaim;
    }
}
