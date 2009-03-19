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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

public class UriBuilderImpl extends UriBuilder {

    private String scheme;
    private String userInfo;
    private int port;
    private String host;
    private List<PathSegment> paths;
    private String fragment;
    private MultivaluedMap<String, String> query = new MetadataMap<String, String>();
    private MultivaluedMap<String, String> matrix = new MetadataMap<String, String>();

    /**
     * Creates builder with empty URI.
     */
    public UriBuilderImpl() {
    }

    /**
     * Creates builder initialized with given URI.
     * 
     * @param uri initial value for builder
     * @throws IllegalArgumentException when uri is null
     */
    public UriBuilderImpl(URI uri) throws IllegalArgumentException {
        setUriParts(uri);
    }

    @Override
    public URI build() throws UriBuilderException {
        try {
            return new URI(scheme, userInfo, host, port, buildPath(), buildQuery(), fragment);
        } catch (URISyntaxException ex) {
            throw new UriBuilderException("URI can not be built", ex);
        }
    }

    @Override
    public URI build(Map<String, Object> map) throws IllegalArgumentException, UriBuilderException {
        try {
            String path = buildPath();
            path = substituteMapped(path, map);
            return new URI(scheme, userInfo, host, port, path, buildQuery(), fragment);
        } catch (URISyntaxException ex) {
            throw new UriBuilderException("URI can not be built", ex);
        }
    }

    @Override
    public URI build(Object... values) throws IllegalArgumentException, UriBuilderException {
        try {
            String path = buildPath();
            path = substituteVarargs(path, values);
            return new URI(scheme, userInfo, host, port, path, buildQuery(), fragment);
        } catch (URISyntaxException ex) {
            throw new UriBuilderException("URI can not be built", ex);
        }
    }

    private String substituteVarargs(String path, Object... values) {
        Map<String, String> varValueMap = new HashMap<String, String>();
        URITemplate templ = new URITemplate(path);
        // vars in set are properly ordered due to linking in hash set
        Set<String> uniqueVars = new LinkedHashSet<String>(templ.getVariables());
        if (values.length < uniqueVars.size()) {
            throw new IllegalArgumentException("Unresolved variables; only " + values.length
                                               + " value(s) given for " + uniqueVars.size()
                                               + " unique variable(s)");
        }
        int idx = 0;
        for (String var : uniqueVars) {
            Object oval = values[idx++];
            varValueMap.put(var, oval.toString());
        }
        return templ.substitute(varValueMap);
    }

    private String substituteMapped(String path, Map<String, ? extends Object> varValueMap) {
        URITemplate templ = new URITemplate(path);
        Set<String> uniqueVars = new HashSet<String>(templ.getVariables());
        if (varValueMap.size() < uniqueVars.size()) {
            throw new IllegalArgumentException("Unresolved variables; only " + varValueMap.size()
                                               + " value(s) given for " + uniqueVars.size()
                                               + " unique variable(s)");
        }
        return templ.substitute(varValueMap);
    }

    // CHECKSTYLE:OFF
    @Override
    public UriBuilder clone() {
        return new UriBuilderImpl(build());
    }

    // CHECKSTYLE:ON

    @Override
    public UriBuilder encode(boolean enable) {
        //this.encode = enable;
        return this;
    }

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

    @Override
    public UriBuilder matrixParam(String name, String value) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (value != null) {
            matrix.add(name, value);
        } else {
            matrix.remove(name);
        }
        return this;
    }

    @Override
    public UriBuilder path(String... segments) throws IllegalArgumentException {
        
        if (paths == null) {
            paths = new ArrayList<PathSegment>();
        }
        for (String seg : segments) {
            paths.addAll(JAXRSUtils.getPathSegments(seg, false));
        }
        matrix.clear();
        if (!paths.isEmpty()) {
            matrix = paths.get(paths.size() - 1).getMatrixParameters();        
        }
        
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

    @Override
    public UriBuilder path(Method... methods) throws IllegalArgumentException {
        if (methods == null) {
            throw new IllegalArgumentException("methods is null");
        }
        for (Method method : methods) {
            if (method == null) {
                throw new IllegalArgumentException("method is null");
            }
            Annotation ann = method.getAnnotation(Path.class);
            if (ann == null) {
                throw new IllegalArgumentException("Method '" + method.getClass().getCanonicalName() + "."
                                                   + method.getName() + "' is not annotated with Path");
            }
            // path(String) decomposes multi-segment path when necessary
            path(((Path)ann).value());
        }
        return this;
    }

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
    public UriBuilder port(int thePort) throws IllegalArgumentException {
        this.port = thePort;
        return this;
    }

    @Override
    public UriBuilder queryParam(String name, String value) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (value != null) {
            query.add(name, value);
        } else {
            query.remove(name);
        }
        return this;
    }

    public UriBuilder replaceMatrixParams(String m) throws IllegalArgumentException {
        if (m == null) {
            throw new IllegalArgumentException("name is null");
        }
        MultivaluedMap<String, String> values = JAXRSUtils.getStructuredParams(m, ";", true);
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            matrix.put(entry.getKey(), entry.getValue());
        }
        
        return this;
    }

    @Override
    public UriBuilder replaceQueryParams(String q) throws IllegalArgumentException {
        if (q == null) {
            throw new IllegalArgumentException("name is null");
        }
        MultivaluedMap<String, String> values = JAXRSUtils.getStructuredParams(q, "&", true);
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            query.put(entry.getKey(), entry.getValue());
        }
        
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
            if (scheme == null) {
                scheme = "http";
            }
            URI uri = new URI(scheme + "://" + ssp);
            setUriParts(uri);
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
    
    @Override
    public boolean isEncode() {
        throw new UnsupportedOperationException("Not implemented :/");        
    }
  
    @Override
    public UriBuilder extension(String arg0) {
        throw new UnsupportedOperationException("Not implemented :/");
    }
    
    @Override
    public UriBuilder replacePath(String... path) throws IllegalArgumentException {
        for (String p : path) {
            paths = JAXRSUtils.getPathSegments(p, false);
        }
        if (!paths.isEmpty()) {
            matrix = paths.get(paths.size() - 1).getMatrixParameters();
        } else {
            matrix.clear();
        }
        return this;
    }

    private void setUriParts(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }
        scheme = uri.getScheme();
        port = uri.getPort();
        host = uri.getHost();
        setPathAndMatrix(uri.getPath());
        fragment = uri.getFragment();
        query = JAXRSUtils.getStructuredParams(uri.getQuery(), "&", true);
        userInfo = uri.getUserInfo();
    }

    private void setPathAndMatrix(String path) {
        paths = JAXRSUtils.getPathSegments(path, false, false);
        if (!paths.isEmpty()) {
            matrix = paths.get(paths.size() - 1).getMatrixParameters();
        } else {
            matrix.clear();
        }
    }
    
    private String buildPath() {
        StringBuilder sb = new StringBuilder();
        Iterator<PathSegment> iter = paths.iterator();
        while (iter.hasNext()) {
            String p = iter.next().getPath();
            if (p.length() != 0 || !iter.hasNext()) {
                if (!p.startsWith("/")) {
                    sb.append('/');
                }
                sb.append(p);
            }
        }
        if (!matrix.isEmpty()) {
            sb.append(';');
            sb.append(buildParams(matrix, ';'));
        }
        return sb.toString();
    }

    private String buildQuery() {
        return buildParams(query, '&');
    }

    /**
     * Builds param string for query part or matrix part of URI.
     * 
     * @param map query or matrix multivalued map
     * @param separator params separator, '&' for query ';' for matrix
     * @return stringified params.
     */
    private String buildParams(MultivaluedMap<String, String> map, char separator) {
        StringBuilder b = new StringBuilder();
        for (Iterator<Map.Entry<String, List<String>>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, List<String>> entry = it.next();
            for (Iterator<String> sit = entry.getValue().iterator(); sit.hasNext();) {
                String val = sit.next();
                b.append(entry.getKey()).append('=').append(val);
                if (sit.hasNext() || it.hasNext()) {
                    b.append(separator);
                }
            }
        }
        return b.length() > 0 ? b.toString() : null;
    }
    
    
}
