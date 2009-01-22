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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;

public class UriBuilderImpl extends UriBuilder {

    private String scheme;
    private String userInfo;
    private int port;
    private String host;
    private List<PathSegment> paths = new ArrayList<PathSegment>();
    private String fragment;
    private MultivaluedMap<String, String> query = new MetadataMap<String, String>();

    public UriBuilderImpl() {
    }

    public UriBuilderImpl(URI uri) {
        setUriParts(uri);
    }

    @Override
    public URI build(Object... values) throws IllegalArgumentException, UriBuilderException {
        try {
            return new URI(scheme, userInfo, host, port, buildPath(), buildQuery(), fragment);
        } catch (URISyntaxException ex) {
            throw new UriBuilderException("URI can not be built", ex);
        }
    }

    // CHECKSTYLE:OFF
    @Override
    public UriBuilder clone() {
        return new UriBuilderImpl(build());
    }

    // CHECKSTYLE:ON

    @Override
    public UriBuilder fragment(String theFragment) throws IllegalArgumentException {
        this.fragment = theFragment;
        return this;
    }

    @Override
    public UriBuilder host(String theHost) throws IllegalArgumentException {
        this.host = theHost;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public UriBuilder path(Class resource) throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        Annotation ann = resource.getAnnotation(Path.class);
        if (ann == null) {
            throw new IllegalArgumentException("Class '" + resource.getCanonicalName()
                                               + "' is not annotated with Path");
        }
        // path(String) decomposes multi-segment path when necessary
        return path(((Path)ann).value());
    }

    @SuppressWarnings("unchecked")
    @Override
    public UriBuilder path(Class resource, String method) throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }
        Annotation foundAnn = null;
        for (Method meth : resource.getMethods()) {
            if (meth.getName().equals(method)) {
                Annotation ann = meth.getAnnotation(Path.class);
                if (foundAnn != null && ann != null) {
                    throw new IllegalArgumentException("Multiple Path annotations for '" + method
                                                       + "' overloaded method");
                }
                foundAnn = ann;
            }
        }
        if (foundAnn == null) {
            throw new IllegalArgumentException("No Path annotation for '" + method + "' method");
        }
        // path(String) decomposes multi-segment path when necessary
        return path(((Path)foundAnn).value());
    }

    @Override
    public UriBuilder path(Method method) throws IllegalArgumentException {
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }
        Annotation ann = method.getAnnotation(Path.class);
        if (ann == null) {
            throw new IllegalArgumentException("Method '" + method.getClass().getCanonicalName() + "."
                                               + method.getName() + "' is not annotated with Path");
        }
        // path(String) decomposes multi-segment path when necessary
        return path(((Path)ann).value());
    }

    @Override
    public UriBuilder path(String path) throws IllegalArgumentException {
        List<PathSegment> segments = JAXRSUtils.getPathSegments(path, false);
        paths.addAll(segments);
        return this;
    }

    @Override
    public UriBuilder port(int thePort) throws IllegalArgumentException {
        this.port = thePort;
        return this;
    }

    @Override
    public UriBuilder scheme(String s) throws IllegalArgumentException {
        scheme = s;
        return this;
    }

    @Override
    public UriBuilder schemeSpecificPart(String ssp) throws IllegalArgumentException {
        // scheme-specific part is whatever after ":" of URI
        // see: http://en.wikipedia.org/wiki/URI_scheme
        try {
            URI uri = new URI("whatever://" + ssp);
            port = uri.getPort();
            host = uri.getHost();
            paths = JAXRSUtils.getPathSegments(uri.getPath(), false);
            fragment = uri.getFragment();
            query = JAXRSUtils.getStructuredParams(uri.getQuery(), "&", true);
            userInfo = uri.getUserInfo();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Wrong syntax of scheme-specific part", e);
        }
        return this;
    }

    @Override
    public UriBuilder uri(URI uri) throws IllegalArgumentException {
        setUriParts(uri);
        return this;
    }

    @Override
    public UriBuilder userInfo(String ui) throws IllegalArgumentException {
        this.userInfo = ui;
        return this;
    }

    private void setUriParts(URI uri) {
        scheme = uri.getScheme();
        port = uri.getPort();
        host = uri.getHost();
        paths = JAXRSUtils.getPathSegments(uri.getPath(), false);
        fragment = uri.getFragment();
        query = JAXRSUtils.getStructuredParams(uri.getQuery(), "&", true);
        userInfo = uri.getUserInfo();
    }

    private String buildPath() {
        StringBuilder sb = new StringBuilder();
        for (PathSegment ps : paths) {
            String p = ps.getPath();
            if (!p.startsWith("/") && (sb.length() == 0 || sb.charAt(sb.length() - 1) != '/')) {
                sb.append('/');
            }
            sb.append(p);
        }
        return sb.toString();

    }

    private String buildQuery() {
        StringBuilder b = new StringBuilder();
        for (Iterator<Map.Entry<String, List<String>>> it = query.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, List<String>> entry = it.next();
            b.append(entry.getKey()).append('=').append(entry.getValue().get(0));
            if (it.hasNext()) {
                b.append('&');
            }
        }
        return b.length() > 0 ? b.toString() : null;
    }

    @Override
    public URI buildFromEncoded(Object... values) throws IllegalArgumentException, UriBuilderException {
        try {
            return new URI(scheme, userInfo, host, port, buildPath(), buildQuery(), fragment);
        } catch (URISyntaxException ex) {
            throw new UriBuilderException("URI can not be built", ex);
        }
    }

    @Override
    public URI buildFromEncodedMap(Map<String, ? extends Object> arg0) throws IllegalArgumentException,
        UriBuilderException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented :/");
    }

    @Override
    public URI buildFromMap(Map<String, ? extends Object> arg0) throws IllegalArgumentException,
        UriBuilderException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented :/");
    }

    @Override
    public UriBuilder matrixParam(String name, Object... values) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented :/");
    }

    @Override
    public UriBuilder queryParam(String name, Object... values) throws IllegalArgumentException {
        List<String> queryList = new ArrayList<String>();
        for (Object value : values) {
            queryList.add(value.toString());
        }
        query.put(name, queryList);
        return this;
    }

    @Override
    public UriBuilder replaceMatrix(String matrix) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented :/");
    }

    @Override
    public UriBuilder replaceMatrixParam(String name, Object... values) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented :/");
    }

    @Override
    public UriBuilder replacePath(String path) {
        paths = JAXRSUtils.getPathSegments(path, false);
        return this;
    }

    @Override
    public UriBuilder replaceQuery(String queryValue) throws IllegalArgumentException {
        query = JAXRSUtils.getStructuredParams(queryValue, "&", true);
        return this;
    }

    @Override
    public UriBuilder segment(String... segments) throws IllegalArgumentException {
        for (String segment : segments) {
            path(segment);
        }
        return this;
    }

    @Override
    public UriBuilder replaceQueryParam(String name, Object... values) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented :/");
    }

}
