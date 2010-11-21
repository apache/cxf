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
package org.apache.cxf.transport.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.Message;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.version.Version;

public class Headers {
    /**
     *  This constant is the Message(Map) key for the HttpURLConnection that
     *  is used to get the response.
     */
    public static final String KEY_HTTP_CONNECTION = "http.connection";
    private static final Logger LOG = LogUtils.getL7dLogger(Headers.class);
    private final Message message;
    private final Map<String, List<String>> headers;

    public Headers(Message message) {
        this.message = message;
        this.headers = getSetProtocolHeaders(message);
    }

    public void writeSessionCookies(Map<String, Cookie> sessionCookies) {
        List<String> cookies = null;
        for (String s : headers.keySet()) {
            if (HttpHeaderHelper.COOKIE.equalsIgnoreCase(s)) {
                cookies = headers.remove(s);
                break;
            }
        }
        if (cookies == null) {
            cookies = new ArrayList<String>();
        } else {
            cookies = new ArrayList<String>(cookies);
        }
        headers.put(HttpHeaderHelper.COOKIE, cookies);
        for (Cookie c : sessionCookies.values()) {
            cookies.add(c.requestCookieHeader());
        }
    }

    /**
     * This call places HTTP Header strings into the headers that are relevant
     * to the ClientPolicy that is set on this conduit by configuration.
     * 
     * REVISIT: A cookie is set statically from configuration? 
     */
    void setHeadersByClientPolicy(HTTPClientPolicy policy) {
        if (policy == null) {
            return;
        }
        if (policy.isSetCacheControl()) {
            headers.put("Cache-Control",
                    createMutableList(policy.getCacheControl().value()));
        }
        if (policy.isSetHost()) {
            headers.put("Host",
                    createMutableList(policy.getHost()));
        }
        if (policy.isSetConnection()) {
            headers.put("Connection",
                    createMutableList(policy.getConnection().value()));
        }
        if (policy.isSetAccept()) {
            headers.put("Accept",
                    createMutableList(policy.getAccept()));
        } else if (!headers.containsKey("Accept")) {
            headers.put("Accept", createMutableList("*/*"));
        }
        if (policy.isSetAcceptEncoding()) {
            headers.put("Accept-Encoding",
                    createMutableList(policy.getAcceptEncoding()));
        }
        if (policy.isSetAcceptLanguage()) {
            headers.put("Accept-Language",
                    createMutableList(policy.getAcceptLanguage()));
        }
        if (policy.isSetContentType()) {
            message.put(Message.CONTENT_TYPE, policy.getContentType());
        }
        if (policy.isSetCookie()) {
            headers.put("Cookie",
                    createMutableList(policy.getCookie()));
        }
        if (policy.isSetBrowserType()) {
            headers.put("BrowserType",
                    createMutableList(policy.getBrowserType()));
        }
        if (policy.isSetReferer()) {
            headers.put("Referer",
                    createMutableList(policy.getReferer()));
        }
    }

    public void removeAuthorizationHeaders() {
        headers.remove("Authorization");
        headers.remove("Proxy-Authorization");
    }
    
    public void setAuthorization(String authorization) {
        headers.put("Authorization",
                createMutableList(authorization));
    }
    
    public void setProxyAuthorization(String authorization) {
        headers.put("Proxy-Authorization",
                createMutableList(authorization));
    }
    
    
    /**
     * While extracting the Message.PROTOCOL_HEADERS property from the Message,
     * this call ensures that the Message.PROTOCOL_HEADERS property is
     * set on the Message. If it is not set, an empty map is placed there, and
     * then returned.
     * 
     * @param message The outbound message
     * @return The PROTOCOL_HEADERS map
     */
    public static Map<String, List<String>> getSetProtocolHeaders(final Message message) {
        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));        
        if (null == headers) {
            headers = new LinkedHashMap<String, List<String>>();
        } else if (headers instanceof HashMap) {
            headers = new LinkedHashMap<String, List<String>>(headers);
        }
        message.put(Message.PROTOCOL_HEADERS, headers);
        return headers;
    }
    
    public void readFromConnection(HttpURLConnection connection) {
        Map<String, List<String>> origHeaders = connection.getHeaderFields();
        headers.clear();
        for (String key : connection.getHeaderFields().keySet()) {
            if (key != null) {
                headers.put(HttpHeaderHelper.getHeaderKey(key), 
                    origHeaders.get(key));
            }
        }
    }

    private static List<String> createMutableList(String val) {
        return new ArrayList<String>(Arrays.asList(new String[] {val}));
    }
    
    /**
     * This procedure logs the PROTOCOL_HEADERS from the 
     * Message at the specified logging level.
     * 
     * @param level   The Logging Level.
     * @param headers The Message protocol headers.
     */
    void logProtocolHeaders(Level level) {
        for (String header : headers.keySet()) {
            List<String> headerList = headers.get(header);
            for (String value : headerList) {
                LOG.log(level, header + ": " + value);
            }
        }
    }
    
    /**
     * Put the headers from Message.PROTOCOL_HEADERS headers into the URL
     * connection.
     * Note, this does not mean they immediately get written to the output
     * stream or the wire. They just just get set on the HTTP request.
     * 
     * @param message The outbound message.
     * @throws IOException
     */
    public void setURLRequestHeaders(String conduitName) throws IOException {
        HttpURLConnection connection = 
            (HttpURLConnection)message.get(KEY_HTTP_CONNECTION);

        String ct  = (String)message.get(Message.CONTENT_TYPE);
        String enc = (String)message.get(Message.ENCODING);

        if (null != ct) {
            if (enc != null 
                && ct.indexOf("charset=") == -1
                && !ct.toLowerCase().contains("multipart/related")) {
                ct = ct + "; charset=" + enc;
            }
        } else if (enc != null) {
            ct = "text/xml; charset=" + enc;
        } else {
            ct = "text/xml";
        }
        connection.setRequestProperty(HttpHeaderHelper.CONTENT_TYPE, ct);
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Sending "
                + connection.getRequestMethod() 
                + " Message with Headers to " 
                + connection.getURL()
                + " Conduit :"
                + conduitName
                + "\nContent-Type: " + ct + "\n");
            new Headers(message).logProtocolHeaders(Level.FINE);
        }
        
        transferProtocolHeadersToURLConnection(connection);
    }
    
    /**
     * This procedure sets the URLConnection request properties
     * from the PROTOCOL_HEADERS in the message.
     */
    private void transferProtocolHeadersToURLConnection(URLConnection connection) {
        for (String header : headers.keySet()) {
            List<String> headerList = headers.get(header);
            if (HttpHeaderHelper.CONTENT_TYPE.equalsIgnoreCase(header)) {
                continue;
            }
            if (HttpHeaderHelper.COOKIE.equalsIgnoreCase(header)) {
                for (String s : headerList) {
                    connection.addRequestProperty(HttpHeaderHelper.COOKIE, s);
                }
            } else {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < headerList.size(); i++) {
                    b.append(headerList.get(i));
                    if (i + 1 < headerList.size()) {
                        b.append(',');
                    }
                }
                connection.setRequestProperty(header, b.toString());
            }
        }
        if (!connection.getRequestProperties().containsKey("User-Agent")) {
            connection.addRequestProperty("User-Agent", Version.getCompleteVersionString());
        }
    }

}
