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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

public class UriBuilderImpl extends UriBuilder {

    private static final Pattern DECODE_PATTERN = Pattern.compile("%[0-9a-fA-F][0-9a-fA-F]");

    private String scheme;
    private String userInfo;
    private int port;
    private String host;
    private List<PathSegment> paths = new ArrayList<PathSegment>();
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

    @Override
    public URI buildFromEncoded(Object... values) throws IllegalArgumentException, UriBuilderException {
        // Problem: multi-arg URI c-tor always forces encoding, operation contract would be broken;
        // use os single-arg URI c-tor requires unnecessary concatenate-parse roundtrip.
        // Solution: decode back given values and pass as non-decoded to regular build() method
        for (int i = 0; i < values.length; i++) {
            values[i] = decodePartiallyEncoded(values[i].toString());
        }
        return build(values);
    }

    @Override
    public URI buildFromMap(Map<String, ? extends Object> map) throws IllegalArgumentException,
        UriBuilderException {
        try {
            String path = buildPath();
            path = substituteMapped(path, map);
            return new URI(scheme, userInfo, host, port, path, buildQuery(), fragment);
        } catch (URISyntaxException ex) {
            throw new UriBuilderException("URI can not be built", ex);
        }
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

    @Override
    public URI buildFromEncodedMap(Map<String, ? extends Object> map) throws IllegalArgumentException,
        UriBuilderException {
        // see buildFromEncoded() comment
        Map<String, String> decodedMap = new HashMap<String, String>(map.size());
        for (Map.Entry<String, ? extends Object> entry : map.entrySet()) {
            decodedMap.put(entry.getKey(), decodePartiallyEncoded(entry.getValue().toString()));
        }
        return buildFromMap(decodedMap);
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

    @Override
    public UriBuilder path(Class resource) throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        Class<?> cls = resource;
        Path ann = cls.getAnnotation(Path.class);
        if (ann == null) {
            throw new IllegalArgumentException("Class '" + resource.getCanonicalName()
                                               + "' is not annotated with Path");
        }
        // path(String) decomposes multi-segment path when necessary
        return path(ann.value());
    }

    @Override
    public UriBuilder path(Class resource, String method) throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }
        Path foundAnn = null;
        for (Method meth : resource.getMethods()) {
            if (meth.getName().equals(method)) {
                Path ann = meth.getAnnotation(Path.class);
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
        return path(foundAnn.value());
    }

    @Override
    public UriBuilder path(Method method) throws IllegalArgumentException {
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }
        Path ann = method.getAnnotation(Path.class);
        if (ann == null) {
            throw new IllegalArgumentException("Method '" + method.getClass().getCanonicalName() + "."
                                               + method.getName() + "' is not annotated with Path");
        }
        // path(String) decomposes multi-segment path when necessary
        return path(ann.value());
    }

    @Override
    public UriBuilder path(String path) throws IllegalArgumentException {
        List<PathSegment> segments = JAXRSUtils.getPathSegments(path, false);
        if (!paths.isEmpty() && !matrix.isEmpty()) {
            PathSegment ps = paths.remove(paths.size() - 1);
            paths.add(replacePathSegment(ps));
        }
        paths.addAll(segments);
        matrix.clear();
        if (!paths.isEmpty()) {
            matrix = paths.get(paths.size() - 1).getMatrixParameters();        
        }
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
        paths = JAXRSUtils.getPathSegments(path, false);
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
            PathSegment ps = iter.next();
            String p = ps.getPath();
            if (p.length() != 0 || !iter.hasNext()) {
                if (!p.startsWith("/")) {
                    sb.append('/');
                }
                sb.append(p);
                if (iter.hasNext()) {
                    buildMatrix(sb, ps.getMatrixParameters());
                }
            }
        }
        buildMatrix(sb, matrix);
        return sb.toString();
    }

    private String buildQuery() {
        return buildParams(query, '&');
    }

    @Override
    public UriBuilder matrixParam(String name, Object... values) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        List<String> list = matrix.get(name);
        if (list == null) {
            matrix.put(name, toStringList(values));
        } else {
            list.addAll(toStringList(values));
        }
        return this;
    }

    @Override
    public UriBuilder queryParam(String name, Object... values) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        List<String> list = query.get(name);
        if (list == null) {
            query.put(name, toStringList(values));
        } else {
            list.addAll(toStringList(values));
        }
        return this;
    }

    @Override
    public UriBuilder replaceMatrix(String matrixValues) throws IllegalArgumentException {
        this.matrix = JAXRSUtils.getStructuredParams(matrixValues, ";", true);
        return this;
    }

    @Override
    public UriBuilder replaceMatrixParam(String name, Object... values) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (values != null && values.length >= 1 && values[0] != null) {
            matrix.put(name, toStringList(values));
        } else {
            matrix.remove(name);
        }
        return this;
    }

    @Override
    public UriBuilder replacePath(String path) {
        if (path == null) {
            paths.clear();
            matrix.clear();
        } else {
            setPathAndMatrix(path);
        }
        return this;
    }

    @Override
    public UriBuilder replaceQuery(String queryValue) throws IllegalArgumentException {
        query = JAXRSUtils.getStructuredParams(queryValue, "&", true);
        return this;
    }

    @Override
    public UriBuilder replaceQueryParam(String name, Object... values) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (values != null && values.length >= 1 && values[0] != null) {
            query.put(name, toStringList(values));
        } else {
            query.remove(name);
        }
        return this;
    }

    @Override
    public UriBuilder segment(String... segments) throws IllegalArgumentException {
        for (String segment : segments) {
            path(segment);
        }
        return this;
    }

    /**
     * Decode partially encoded string. Decode only values that matches patter "percent char followed by two
     * hexadecimal digits".
     * 
     * @param encoded fully or partially encoded string.
     * @return decoded string
     */
    private String decodePartiallyEncoded(String encoded) {
        Matcher m = DECODE_PATTERN.matcher(encoded);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String found = m.group();
            m.appendReplacement(sb, JAXRSUtils.uriDecode(found));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Query or matrix params convertion from object values vararg to list of strings. No encoding is
     * provided.
     * 
     * @param values entry vararg values
     * @return list of strings
     * @throws IllegalArgumentException when one of values is null
     */
    private List<String> toStringList(Object... values) throws IllegalArgumentException {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value == null) {
                throw new IllegalArgumentException("Null value on " + i + " position");
            }
            list.add(value.toString());
        }
        return list;
    }

    /**
     * Builds param string for query part or matrix part of URI.
     * 
     * @param map query or matrix multivalued map
     * @param separator params separator, '&' for query ';' for matrix
     * @return stringified params.
     */
    private static String buildParams(MultivaluedMap<String, String> map, char separator) {
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
    
    /**
     * Builds param string for matrix part of URI.
     * 
     * @param sb buffer to add the matrix part to, will get ';' added if map is not empty 
     * @param map matrix multivalued map
     */    
    private static void buildMatrix(StringBuilder sb, MultivaluedMap<String, String> map) {
        if (!map.isEmpty()) {
            sb.append(';');
            sb.append(buildParams(map, ';'));
        }
    }
    
    private PathSegment replacePathSegment(PathSegment ps) {
        StringBuilder sb = new StringBuilder();
        sb.append(ps.getPath());
        buildMatrix(sb, matrix);
        return new PathSegmentImpl(sb.toString());
    }
}
