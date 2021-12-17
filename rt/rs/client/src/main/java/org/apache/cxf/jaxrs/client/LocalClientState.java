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
package org.apache.cxf.jaxrs.client;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;

/**
 * Keeps the client state such as the baseURI, currentURI, requestHeaders, current response
 *
 */
public class LocalClientState implements ClientState {
    private static final String HTTP_SCHEME = "http";
    private static final String WS_SCHEME = "ws";

    private MultivaluedMap<String, String> requestHeaders = new MetadataMap<>(false, true);
    private MultivaluedMap<String, String> templates;
    private Response response;
    private URI baseURI;
    private UriBuilder currentBuilder;
    private Map<String, Object> properties;

    public LocalClientState() {

    }

    public LocalClientState(URI baseURI) {
        this(baseURI, Collections.emptyMap());
    }
    
    public LocalClientState(URI baseURI, Map<String, Object> properties) {
        this.baseURI = baseURI;
        
        if (properties != null) {
            this.properties = new HashMap<>(properties);
        }
        
        resetCurrentUri(properties);
    }

    public LocalClientState(URI baseURI, URI currentURI) {
        this(baseURI, currentURI, Collections.emptyMap()); 
    }

    public LocalClientState(URI baseURI, URI currentURI, Map<String, Object> properties) {
        this.baseURI = baseURI;
        
        if (properties != null) {
            this.properties = new HashMap<>(properties);
        }
        
        this.currentBuilder = new UriBuilderImpl(properties).uri(currentURI);
    }

    public LocalClientState(LocalClientState cs) {
        this.requestHeaders = new MetadataMap<>(cs.requestHeaders);
        this.templates = cs.templates == null ? null : new MetadataMap<String, String>(cs.templates);
        this.response = cs.response;

        this.baseURI = cs.baseURI;
        this.currentBuilder = cs.currentBuilder != null ? cs.currentBuilder.clone() : null;
        this.properties = cs.properties;
    }

    private void resetCurrentUri(Map<String, Object> props) {
        if (isSupportedScheme(baseURI)) {
            this.currentBuilder = new UriBuilderImpl(props).uri(baseURI);
        } else {
            this.currentBuilder = new UriBuilderImpl(props).uri("/");
        }
    }

    public void setCurrentBuilder(UriBuilder currentBuilder) {
        this.currentBuilder = currentBuilder;
    }

    public UriBuilder getCurrentBuilder() {
        return currentBuilder;
    }

    public void setBaseURI(URI baseURI) {
        this.baseURI = baseURI;
        resetCurrentUri(Collections.emptyMap());
    }

    public URI getBaseURI() {
        return baseURI;
    }

    public void setResponse(Response r) {
        this.response = r;
    }

    public Response getResponse() {
        return response;
    }

    public void setRequestHeaders(MultivaluedMap<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public MultivaluedMap<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public MultivaluedMap<String, String> getTemplates() {
        return templates;
    }

    public void setTemplates(MultivaluedMap<String, String> map) {
        if (templates == null) {
            this.templates = map;
        } else if (map != null) {
            templates.putAll(map);
        } else {
            templates = null;
        }
    }

    public void reset() {
        requestHeaders.clear();
        response = null;
        currentBuilder = new UriBuilderImpl(properties).uri(baseURI);
        templates = null;
    }
    
    public ClientState newState(URI currentURI, MultivaluedMap<String, String> headers,
            MultivaluedMap<String, String> templatesMap, Map<String, Object> props) {
        final ClientState state;
        if (isSupportedScheme(currentURI)) {
            state = new LocalClientState(currentURI, props);
        } else {
            state = new LocalClientState(baseURI, currentURI, props);
        }
        if (headers != null) {
            state.setRequestHeaders(headers);
        }
        // we need to carry the template parameters forward
        MultivaluedMap<String, String> newTemplateParams = templates;
        if (newTemplateParams != null && templatesMap != null) {
            newTemplateParams.putAll(templatesMap);
        } else {
            newTemplateParams = templatesMap;
        }
        state.setTemplates(newTemplateParams);
        return state;
    }

    public ClientState newState(URI currentURI,
                                MultivaluedMap<String, String> headers,
                                MultivaluedMap<String, String> templatesMap) {
        return newState(currentURI, headers, templatesMap, properties);
    }

    private static boolean isSupportedScheme(URI uri) {
        return !StringUtils.isEmpty(uri.getScheme())
            && (uri.getScheme().startsWith(HTTP_SCHEME) || uri.getScheme().startsWith(WS_SCHEME));
    }
}
