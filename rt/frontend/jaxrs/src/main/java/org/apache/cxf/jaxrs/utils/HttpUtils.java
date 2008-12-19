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

package org.apache.cxf.jaxrs.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.PathSegment;

import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.servlet.ServletDestination;

public final class HttpUtils {
    private static final String LOCAL_IP_ADDRESS = "127.0.0.1";
    private static final String LOCAL_HOST = "localhost";
    
    
    private HttpUtils() {
    }
    
    public static URI toAbsoluteUri(URI u, Message message) { 
        if (!u.isAbsolute()) {
            HttpServletRequest httpRequest = 
                (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
            if (httpRequest != null) {
                String scheme = httpRequest.isSecure() ? "https" : "http";
                String host = httpRequest.getLocalName();
                if (LOCAL_IP_ADDRESS.equals(host)) {
                    host = LOCAL_HOST;
                }
                int port = httpRequest.getLocalPort();
                return URI.create(scheme + "://" + host + ':' + port + u.toString());
            }
        }
        return u;
    }
    
    public static String getPathToMatch(Message m, boolean addSlash) {
        String requestAddress = (String)m.get(Message.REQUEST_URI);
        String baseAddress = getBaseAddress(m);
        return getPathToMatch(requestAddress, baseAddress, addSlash);
    }
    
    
    public static String getBaseAddress(Message m) {
        try {
            String endpointAddress = getEndpointAddress(m);
            return new URL(endpointAddress).getPath();
        } catch (MalformedURLException ex) {
            return (String)m.get(Message.BASE_PATH);
        }
    }
    
    public static String getEndpointAddress(Message m) {
        String address = null;
        Destination d = m.getExchange().getDestination();
        if (d instanceof ServletDestination) {
            address = ((ServletDestination)d).getEndpointInfo().getAddress();
        } else {
            address = d.getAddress().getAddress().getValue();
        }
        
        return address;
    }
    
    public static String getPathToMatch(String path, String address, boolean addSlash) {
        
        int ind = path.indexOf(address);
        if (ind == 0) {
            path = path.substring(ind + address.length());
        }
        if (addSlash && !path.startsWith("/")) {
            path = "/" + path;
        }
        
        return path;
    }
    
    public static String getOriginalAddress(Message m) {
        Destination d = m.getDestination();
        return d == null ? "/" : d.getAddress().getAddress().getValue();
    }
    
    public static String fromPathSegment(PathSegment ps) {
        if (PathSegmentImpl.class.isAssignableFrom(ps.getClass())) {
            return ((PathSegmentImpl)ps).getOriginalPath();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ps.getPath());
        for (Map.Entry<String, List<String>> entry : ps.getMatrixParameters().entrySet()) {
            for (String value : entry.getValue()) {
                sb.append(';').append(entry.getKey());
                if (value != null) {
                    sb.append('=').append(value);
                }
            }
        }
        return sb.toString();
    }
    
}
