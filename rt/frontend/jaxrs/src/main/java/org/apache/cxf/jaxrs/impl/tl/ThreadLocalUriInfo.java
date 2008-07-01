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

package org.apache.cxf.jaxrs.impl.tl;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class ThreadLocalUriInfo extends AbstractThreadLocalProxy<UriInfo> 
                                    implements UriInfo {

    
    public URI getAbsolutePath() {
        return get().getAbsolutePath();
    }

    public UriBuilder getAbsolutePathBuilder() {
        return get().getAbsolutePathBuilder();
    }

    public List<String> getAncestorResourceURIs() {
        return get().getAncestorResourceURIs();
    }

    public List<Object> getAncestorResources() {
        return get().getAncestorResources();
    }

    public URI getBaseUri() {
        return get().getBaseUri();
    }

    public UriBuilder getBaseUriBuilder() {
        return get().getBaseUriBuilder();
    }

    public String getPath() {
        return get().getPath();
    }

    public String getPath(boolean decode) {
        return get().getPath(decode);
    }

    public List<PathSegment> getPathSegments() {
        return get().getPathSegments();
    }

    public List<PathSegment> getPathSegments(boolean decode) {
        return get().getPathSegments(decode);
    }

    public MultivaluedMap<String, String> getQueryParameters() {
        return get().getQueryParameters();
    }

    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        return get().getQueryParameters(decode);
    }

    public URI getRequestUri() {
        return get().getRequestUri();
    }

    public UriBuilder getRequestUriBuilder() {
        return get().getRequestUriBuilder();
    }

    public MultivaluedMap<String, String> getPathParameters() {
        return get().getPathParameters();
    }

    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        return get().getPathParameters(decode);
    }

    public List<String> getAncestorResourceURIs(boolean decode) {
        return get().getAncestorResourceURIs(decode);
    }

    public String getPathExtension() {
        return get().getPathExtension();
    }

    public UriBuilder getPlatonicRequestUriBuilder() {
        return get().getPlatonicRequestUriBuilder();
    }

}
