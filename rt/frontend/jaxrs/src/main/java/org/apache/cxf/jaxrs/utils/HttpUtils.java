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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.servlet.ServletDestination;

public final class HttpUtils {
    
    private static final String LOCAL_IP_ADDRESS = "127.0.0.1";
    private static final String LOCAL_HOST = "localhost";
    private static final Pattern ENCODE_PATTERN = Pattern.compile("%[0-9a-fA-F][0-9a-fA-F]");
    
    private HttpUtils() {
    }
    
    public static String urlDecode(String value) {
        return UrlUtils.urlDecode(value);
    }
    
    public static String pathDecode(String value) {
        return UrlUtils.pathDecode(value);
    }
    
    public static String urlEncode(String value) {
            
        try {
            value = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // unlikely to happen
        }
        
        return value;
    }
    
    public static String pathEncode(String value) {
        
        String result = urlEncode(value);
        // URLEncoder will encode '+' to %2B but will turn ' ' into '+'
        // We need to retain '+' and encode ' ' as %20
        if (result.indexOf('+') != -1) {
            result = result.replace("+", "%20");
        }
        if (result.indexOf("%2B") != -1) {
            result = result.replace("%2B", "+");
        }

        return result;
    }
    
    /**
     * Encodes partially encoded string. Encode all values but those matching pattern 
     * "percent char followed by two hexadecimal digits".
     * 
     * @param encoded fully or partially encoded string.
     * @return fully encoded string
     */
    public static String encodePartiallyEncoded(String encoded, boolean query) {
        if (encoded.length() == 0) {
            return encoded;
        }
        Matcher m = ENCODE_PATTERN.matcher(encoded);
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (m.find()) {
            String before = encoded.substring(i, m.start());
            sb.append(query ? HttpUtils.urlEncode(before) : HttpUtils.pathEncode(before));
            sb.append(m.group());
            i = m.end();            
        }
        String tail = encoded.substring(i, encoded.length());
        sb.append(query ? HttpUtils.urlEncode(tail) : HttpUtils.pathEncode(tail));
        return sb.toString();
    }
    
    public static SimpleDateFormat getHttpDateFormat() {
        SimpleDateFormat dateFormat = 
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        TimeZone tZone = TimeZone.getTimeZone("GMT");
        dateFormat.setTimeZone(tZone);
        return dateFormat;
    }
    
    public static boolean isDateRelatedHeader(String headerName) {
        return HttpHeaders.DATE.equalsIgnoreCase(headerName)
               || HttpHeaders.IF_MODIFIED_SINCE.equalsIgnoreCase(headerName)
               || HttpHeaders.IF_UNMODIFIED_SINCE.equalsIgnoreCase(headerName)
               || HttpHeaders.EXPIRES.equalsIgnoreCase(headerName)
               || HttpHeaders.LAST_MODIFIED.equalsIgnoreCase(headerName); 
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
        if (d != null) {
            if (d instanceof ServletDestination) {
                address = ((ServletDestination)d).getEndpointInfo().getAddress();
            } else {
                address = d.getAddress().getAddress().getValue();
            }
        } else {
            address = (String)m.get(Message.ENDPOINT_ADDRESS);
        }
        
        return address;
    }
    
    public static void updatePath(Message m, String path) {
        String baseAddress = getBaseAddress(m);
        boolean pathSlash = path.startsWith("/");
        boolean baseSlash = baseAddress.endsWith("/");
        if (pathSlash && baseSlash) {
            path = path.substring(1);
        } else if (!pathSlash && !baseSlash) {
            path = "/" + path;
        }
        m.put(Message.REQUEST_URI, baseAddress + path);
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
    
    public static Response.Status getParameterFailureStatus(ParameterType pType) {
        if (pType == ParameterType.MATRIX || pType == ParameterType.PATH
            || pType == ParameterType.QUERY) {
            return Response.Status.NOT_FOUND;
        }
        return Response.Status.BAD_REQUEST;
    }
    
}
