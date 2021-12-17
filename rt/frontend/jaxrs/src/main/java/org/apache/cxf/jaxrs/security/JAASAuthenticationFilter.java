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
package org.apache.cxf.jaxrs.security;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.cxf.interceptor.security.JAASLoginInterceptor;
import org.apache.cxf.interceptor.security.NamePasswordCallbackHandler;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class JAASAuthenticationFilter implements ContainerRequestFilter {

    private static final List<MediaType> HTML_MEDIA_TYPES =
        Arrays.asList(MediaType.APPLICATION_XHTML_XML_TYPE, MediaType.TEXT_HTML_TYPE);

    private URI redirectURI;
    private String realmName;
    private boolean ignoreBasePath = true;

    private JAASLoginInterceptor interceptor;

    public JAASAuthenticationFilter() {
        interceptor = new JAASLoginInterceptor() {
            protected CallbackHandler getCallbackHandler(String name, String password) {
                return JAASAuthenticationFilter.this.getCallbackHandler(name, password);
            }
        };
        interceptor.setUseDoAs(false);
    }

    @Deprecated
    public void setRolePrefix(String name) {
        interceptor.setRolePrefix(name);
    }


    public void setIgnoreBasePath(boolean ignore) {
        this.ignoreBasePath = ignore;
    }

    public void setContextName(String name) {
        interceptor.setContextName(name);
    }

    public void setLoginConfig(Configuration config) {
        interceptor.setLoginConfig(config);
    }

    public void setRoleClassifier(String rc) {
        interceptor.setRoleClassifier(rc);
    }

    public void setRoleClassifierType(String rct) {
        interceptor.setRoleClassifierType(rct);
    }


    public void setRedirectURI(String uri) {
        this.redirectURI = URI.create(uri);
    }

    public void setRealmName(String name) {
        this.realmName = name;
    }

    protected CallbackHandler getCallbackHandler(String name, String password) {
        return new NamePasswordCallbackHandler(name, password);
    }

    @Override
    public void filter(ContainerRequestContext context) {
        Message m = JAXRSUtils.getCurrentMessage();
        try {
            interceptor.handleMessage(m);
        } catch (SecurityException ex) {
            context.abortWith(handleAuthenticationException(ex, m));
        }
    }

    protected Response handleAuthenticationException(SecurityException ex, Message m) {
        HttpHeaders headers = new HttpHeadersImpl(m);
        if (redirectURI != null && isRedirectPossible(headers)) {

            final URI finalRedirectURI;

            if (!redirectURI.isAbsolute()) {
                String endpointAddress = HttpUtils.getEndpointAddress(m);
                Object basePathProperty = m.get(Message.BASE_PATH);
                if (ignoreBasePath && basePathProperty != null && !"/".equals(basePathProperty)) {
                    int index = endpointAddress.lastIndexOf(basePathProperty.toString());
                    if (index != -1) {
                        endpointAddress = endpointAddress.substring(0, index);
                    }
                }
                finalRedirectURI = UriBuilder.fromUri(endpointAddress).path(redirectURI.toString()).build();
            } else {
                finalRedirectURI = redirectURI;
            }

            return Response.status(getRedirectStatus()).
                    header(HttpHeaders.LOCATION, finalRedirectURI).build();
        }
        ResponseBuilder builder = Response.status(Response.Status.UNAUTHORIZED);

        StringBuilder sb = new StringBuilder();

        List<String> authHeader = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && !authHeader.isEmpty()) {
            // should HttpHeadersImpl do it ?
            String[] authValues = authHeader.get(0).split(" ");
            if (authValues.length > 0) {
                sb.append(authValues[0]);
            }
        } else {
            sb.append("Basic");
        }
        if (realmName != null) {
            sb.append(" realm=\"").append(realmName).append('"');
        }
        builder.header(HttpHeaders.WWW_AUTHENTICATE, sb.toString());

        return builder.build();
    }

    protected Response.Status getRedirectStatus() {
        return Response.Status.TEMPORARY_REDIRECT;
    }

    protected boolean isRedirectPossible(HttpHeaders headers) {
        List<MediaType> clientTypes = headers.getAcceptableMediaTypes();
        return !JAXRSUtils.intersectMimeTypes(clientTypes, HTML_MEDIA_TYPES, false)
                          .isEmpty();
    }
}
