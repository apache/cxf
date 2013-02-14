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

package org.apache.cxf.jaxrs.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.model.MethodInvocationInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfoStack;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

public class UriInfoImpl implements UriInfo {
    private static final Logger LOG = LogUtils.getL7dLogger(UriInfoImpl.class);
    private static final String CASE_INSENSITIVE_QUERIES = "org.apache.cxf.http.case_insensitive_queries";
    private static final String CURRENT_PATH_SEGMENT = ".";
    private static final String PARENT_PATH_SEGMENT = "..";
    
    private MultivaluedMap<String, String> templateParams;
    private Message message;
    private OperationResourceInfoStack stack;
    private boolean caseInsensitiveQueries;

    @SuppressWarnings("unchecked")
    public UriInfoImpl(Message m) {
        this(m, (MultivaluedMap<String, String>)m.get(URITemplate.TEMPLATE_PARAMETERS));
    }
    
    public UriInfoImpl(Message m, MultivaluedMap<String, String> templateParams) {
        this.message = m;
        this.templateParams = templateParams;
        if (m != null) {
            this.stack = m.get(OperationResourceInfoStack.class);
            this.caseInsensitiveQueries = 
                MessageUtils.isTrue(m.getContextualProperty(CASE_INSENSITIVE_QUERIES));
        }
    }

    public URI getAbsolutePath() {
        String path = getAbsolutePathAsString();
        return URI.create(path);
    }

    public UriBuilder getAbsolutePathBuilder() {
        return new UriBuilderImpl(getAbsolutePath());
    }

    public URI getBaseUri() {
        URI u = URI.create(HttpUtils.getEndpointAddress(message));
        return HttpUtils.toAbsoluteUri(u, message);
    }

    public UriBuilder getBaseUriBuilder() {
        return new UriBuilderImpl(getBaseUri());
    }

    public String getPath() {
        return getPath(true);
    }

    public String getPath(boolean decode) {
        String value = doGetPath(decode, true);
        if (value.length() > 1 && value.startsWith("/")) { 
            return value.substring(1);
        } else {
            return value;
        }
    }

    public List<PathSegment> getPathSegments() {
        return getPathSegments(true);
    }

    public List<PathSegment> getPathSegments(boolean decode) {
        return JAXRSUtils.getPathSegments(getPath(false), decode);
    }

    public MultivaluedMap<String, String> getQueryParameters() {
        return getQueryParameters(true);
    }

    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        
        if (!caseInsensitiveQueries) {
            return JAXRSUtils.getStructuredParams((String)message.get(Message.QUERY_STRING), 
                              "&", decode, decode);
        }
        
        MultivaluedMap<String, String> queries = new MetadataMap<String, String>(false, true);
        JAXRSUtils.getStructuredParams(queries, (String)message.get(Message.QUERY_STRING), 
                                      "&", decode, decode);
        return queries;
        
    }

    public URI getRequestUri() {
        String path = getAbsolutePathAsString();
        String queries = (String)message.get(Message.QUERY_STRING);
        if (queries != null) {
            path += "?" + queries;
        }
        return URI.create(path);
    }

    public UriBuilder getRequestUriBuilder() {
        return new UriBuilderImpl(getRequestUri());
    }

    public MultivaluedMap<String, String> getPathParameters() {
        return getPathParameters(true);
    }

    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        MetadataMap<String, String> values = new MetadataMap<String, String>();
        if (templateParams == null) {
            return values;
        }
        for (Map.Entry<String, List<String>> entry : templateParams.entrySet()) {
            if (entry.getKey().equals(URITemplate.FINAL_MATCH_GROUP)) {
                continue;
            }
            values.add(entry.getKey(), decode ? HttpUtils.pathDecode(entry.getValue().get(0)) : entry
                .getValue().get(0));
        }
        return values;
    }

    public List<Object> getMatchedResources() {
        if (stack != null) {
            List<Object> resources = new LinkedList<Object>();
            for (MethodInvocationInfo invocation : stack) {
                resources.add(0, invocation.getRealClass());
            }
            return resources;
        }
        LOG.fine("No resource stack information, returning empty list");
        return Collections.emptyList();
    }

    public List<String> getMatchedURIs() {
        return getMatchedURIs(true);
    }

    public List<String> getMatchedURIs(boolean decode) {
        if (stack != null) {
            List<String> objects = new ArrayList<String>();
            List<String> uris = new LinkedList<String>();
            StringBuilder sum = new StringBuilder("");
            for (MethodInvocationInfo invocation : stack) {
                OperationResourceInfo ori = invocation.getMethodInfo();
                URITemplate[] paths = {
                    ori.getClassResourceInfo().getURITemplate(),
                    ori.getURITemplate()
                };
                for (URITemplate t : paths) {
                    if (t != null) {
                        String v = t.getValue();
                        sum.append("/" + (decode ? HttpUtils.pathDecode(v) : v));
                    }
                }
                UriBuilder ub = UriBuilder.fromPath(sum.toString());
                objects.addAll(invocation.getTemplateValues());
                uris.add(0, ub.build(objects.toArray()).normalize().getPath());
            }
            return uris;
        }
        LOG.fine("No resource stack information, returning empty list");
        return Collections.emptyList();
    }

    private String doGetPath(boolean decode, boolean addSlash) {
        String path = HttpUtils.getPathToMatch(message, addSlash);
        return decode ? HttpUtils.pathDecode(path) : path;
    }

    private String getAbsolutePathAsString() {
        String address = getBaseUri().toString();
        if (MessageUtils.isRequestor(message)) {
            return address;
        }
        String path = doGetPath(false, false);
        if (path.startsWith("/") && address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        return address + path;
    }

    @Override
    public URI relativize(URI uri) {
        URI resolved = resolve(uri);
        URI requestURI = getRequestUri();
        if (!getUriPrefix(resolved).equals(getUriPrefix(requestURI))) {
            return resolved;
        }
        List<PathSegment> resolvedSegments = JAXRSUtils.getPathSegments(resolved.getRawPath(), false);
        List<PathSegment> requestSegments = JAXRSUtils.getPathSegments(requestURI.getRawPath(), false);
        
        int count = 0;
        for (int i = resolvedSegments.size() - 1; i >= 0; i--) {
            if (i <= requestSegments.size() - 1) {
                String resolvedPath = resolvedSegments.get(i).getPath();
                String requestPath = requestSegments.get(i).getPath();
                if (!resolvedPath.equals(requestPath)) {
                    count++;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < count; i++) {
            if (i != 0) {
                sb.append("/");
            }
            sb.append(PARENT_PATH_SEGMENT);
        }
        for (int i = count + 1; i < resolvedSegments.size(); i++) {
            if (i != 0) {
                sb.append("/");
            }
            sb.append(resolvedSegments.get(i).getPath());
        }
        return URI.create(sb.toString());
    }
    
    private static String getUriPrefix(URI uri) {
        StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme()).append(uri.getHost()).append(uri.getPort());
        
        return sb.toString();
    }

    @Override
    public URI resolve(URI uri) {
        if (!uri.isAbsolute()) {
            String uriValue = uri.toString();
            boolean parentPathSegmentAvail = uriValue.contains(PARENT_PATH_SEGMENT)
                || uriValue.contains(CURRENT_PATH_SEGMENT); 
            uri = getBaseUriBuilder().path(uriValue).build();
            if (parentPathSegmentAvail) {
                List<PathSegment> segments = JAXRSUtils.getPathSegments(uri.getRawPath(), false);
                List<PathSegment> actualSegments = new LinkedList<PathSegment>();
                UriBuilder ub = UriBuilder.fromUri(uri).replacePath(null);
                for (PathSegment ps : segments) {
                    if (PARENT_PATH_SEGMENT.equals(ps.getPath()) && !actualSegments.isEmpty()) {
                        actualSegments.remove(actualSegments.size() - 1);
                    } else if (!CURRENT_PATH_SEGMENT.equals(ps.getPath())) {
                        actualSegments.add(ps);
                    }
                }
                for (PathSegment ps : actualSegments) {
                    ub.segment(ps.toString());
                }
                uri = ub.build();
            }
        }
        return uri;
    }
    
    @Override
    public URI relativize(URI arg0, URI arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URI resolve(URI arg0, URI arg1) {
        // TODO Auto-generated method stub
        return null;
    }
}
