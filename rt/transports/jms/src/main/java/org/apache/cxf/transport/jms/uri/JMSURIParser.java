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
package org.apache.cxf.transport.jms.uri;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

/**
 * Unfortunately soap/jms URIs are not recognized correctly in URI.
 * So this class is specialized on parsing jms uris into their parts
 */
final class JMSURIParser {
    private static final Logger LOG = LogUtils.getL7dLogger(JMSURIParser.class);

    String uri;
    int pos;
    private String scheme;
    private String variant;
    private String destination;
    private String query;

    JMSURIParser(String uri) {
        this.uri = UnsafeUriCharactersEncoder.encode(uri);
        this.scheme = parseUntil(":");
        this.variant = parseUntil(":");
        this.destination = parseUntil("?");
        String rest = parseToEnd();
        if (this.destination == null) {
            this.destination = rest;
            this.query = null;
        } else {
            this.query = rest;
        }
        LOG.log(Level.FINE, "Creating endpoint uri=[" + uri + "], destination=[" + destination
                + "], query=[" + query + "]");
    }

    private String parseToEnd() {
        return uri.substring(pos, uri.length());
    }

    private String parseUntil(String separator) {
        int separatorPos = uri.indexOf(separator, pos);
        if (separatorPos != -1) {
            String found = uri.substring(pos, separatorPos);
            pos = separatorPos + 1;
            return found;
        }
        return null;
    }

    public Map<String, Object> parseQuery() {
        Map<String, Object> rc = new HashMap<>();
        if (query != null) {
            String[] parameters = query.split("&");
            for (String parameter : parameters) {
                int p = parameter.indexOf('=');
                if (p >= 0) {
                    String name = urldecode(parameter.substring(0, p));
                    String value = urldecode(parameter.substring(p + 1));
                    rc.put(name, value);
                } else {
                    rc.put(parameter, null);
                }
            }
        }
        return rc;

    }

    private static String urldecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Encoding UTF-8 not supported");
        }
    }

    public String getScheme() {
        return scheme;
    }

    public String getVariant() {
        return variant;
    }

    public String getDestination() {
        return destination;
    }


}
