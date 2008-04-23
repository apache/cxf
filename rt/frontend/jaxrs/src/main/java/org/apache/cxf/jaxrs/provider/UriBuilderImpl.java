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

package org.apache.cxf.jaxrs.provider;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.cxf.jaxrs.JAXRSUtils;
import org.apache.cxf.jaxrs.MetadataMap;

public class UriBuilderImpl extends UriBuilder {
    
    private String scheme;
    private String userInfo;
    private int port;
    private String host;
    private List<PathSegment> paths;
    private MultivaluedMap<String, String> matrix;
    private String fragment;
    private MultivaluedMap<String, String> query;
    
       
    public UriBuilderImpl() {
    }
    
    public UriBuilderImpl(URI uri) {
        setUriParts(uri);
    }

    @Override
    public URI build() throws UriBuilderException {
        
        try {
            return new URI(scheme, 
                           userInfo, 
                           host, 
                           port, 
                           buildPath(), 
                           buildQuery(), 
                           fragment);
        } catch (URISyntaxException ex) {
            throw new UriBuilderException("URI can not be built", ex);
        }
        
    }

    @Override
    public URI build(Map<String, String> parts) throws IllegalArgumentException, UriBuilderException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URI build(String... values) throws IllegalArgumentException, UriBuilderException {
        // TODO Auto-generated method stub
        return null;
    }

//CHECKSTYLE:OFF
    @Override
    public UriBuilder clone() {
        return new UriBuilderImpl(build());
    }
//CHECKSTYLE:ON
    
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
        matrix.putSingle(name, value);
        return this;
    }

    @Override
    public UriBuilder path(String... segments) throws IllegalArgumentException {
        if (paths == null) {
            paths = new ArrayList<PathSegment>();
        }
        for (String segment : segments) {
            paths.add(new PathSegmentImpl(segment, false));
        }
        return this;
    }

    @Override
    public UriBuilder path(Class resource) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UriBuilder path(Method... methods) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UriBuilder path(Class resource, String method) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UriBuilder port(int thePort) throws IllegalArgumentException {
        this.port = thePort;
        return this;
    }

    @Override
    public UriBuilder queryParam(String name, String value) throws IllegalArgumentException {
        if (query == null) {
            query = new MetadataMap<String, String>();
        }
        query.add(name, value);
        return this;
    }

    @Override
    public UriBuilder replaceMatrixParams(String m) throws IllegalArgumentException {
        matrix = JAXRSUtils.getStructuredParams(m, ";", true);
        return this;
    }

    @Override
    public UriBuilder replaceQueryParams(String q) throws IllegalArgumentException {
        this.query = JAXRSUtils.getStructuredParams(q, "&", true);
        return this;
    }

    @Override
    public UriBuilder scheme(String s) throws IllegalArgumentException {
        scheme = s;
        return this;
    }

    @Override
    public UriBuilder schemeSpecificPart(String ssp) throws IllegalArgumentException {
        //schemeSpPart = ssp;
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
            if (!p.startsWith("/")) {
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
    public boolean isEncode() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public UriBuilder replacePath(String... segments) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }
    
    
}
