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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Link.Builder;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.cxf.jaxrs.utils.HttpUtils;

public class LinkBuilderImpl implements Builder {
    private static final String DOUBLE_QUOTE = "\"";
    private UriBuilder ub;
    private URI baseUri;
    private Map<String, String> params = new HashMap<>(6);

    @Override
    public Link build(Object... values) {
        URI resolvedLinkUri = getResolvedUri(values);
        return new LinkImpl(resolvedLinkUri, new HashMap<String, String>(params));
    }

    @Override
    public Link buildRelativized(URI requestUri, Object... values) {
        URI resolvedLinkUri = getResolvedUri(values);
        URI relativized = HttpUtils.relativize(requestUri, resolvedLinkUri);
        return new LinkImpl(relativized, new HashMap<String, String>(params));
    }

    private URI getResolvedUri(Object... values) {
        if (ub == null) {
            ub = new UriBuilderImpl();
            if (baseUri != null) {
                ub.uri(baseUri);
            }

        }
        URI uri = ub.build(values);

        if (!uri.isAbsolute() && baseUri != null && baseUri.isAbsolute()) {
            UriBuilder linkUriBuilder = UriBuilder.fromUri(baseUri);
            return HttpUtils.resolve(linkUriBuilder, uri);
        }
        return uri;
    }

    @Override
    public Builder link(Link link) {
        ub = UriBuilder.fromLink(link);
        params.putAll(link.getParams());
        return this;
    }

    @Override
    public Builder link(String link) {

        link = link.trim();
        if (link.length() > 1 && link.startsWith("<")) {
            int index = link.lastIndexOf('>', link.length());
            if (index != -1) {
                String uri = link.substring(1, index);
                ub = UriBuilder.fromUri(uri);
                if (index + 1 == link.length()) {
                    link = "";
                } else {
                    link = link.substring(index + 1);
                }
            }
        }

        String[] tokens = link.split(";");
        for (String token : tokens) {
            String theToken = token.trim();
            if (!theToken.isEmpty()) {
                int i = theToken.indexOf('=');
                if (i != -1) {
                    String name = theToken.substring(0, i);
                    String value = stripQuotes(theToken.substring(i + 1));
                    params.put(name, value);
                }
            }
        }
        return this;
    }

    @Override
    public Builder param(String name, String value) {
        checkNotNull(name);
        checkNotNull(value);
        params.put(name, value);
        return this;
    }

    @Override
    public Builder rel(String rel) {
        String exisingRel = params.get(Link.REL);
        String newRel = exisingRel == null ? rel : exisingRel + " " + rel;
        return param(Link.REL, newRel);
    }

    @Override
    public Builder title(String title) {
        return param(Link.TITLE, title);
    }

    @Override
    public Builder type(String type) {
        return param(Link.TYPE, type);
    }

    @Override
    public Builder uri(URI uri) {
        ub = UriBuilder.fromUri(uri);
        return this;
    }

    @Override
    public Builder uri(String uri) {
        ub = UriBuilder.fromUri(uri);
        return this;
    }

    @Override
    public Builder uriBuilder(UriBuilder builder) {
        this.ub = builder;
        return this;
    }

    private String stripQuotes(String value) {
        return value.replaceAll(DOUBLE_QUOTE, "");
    }

    private void checkNotNull(String value) {
        if (value == null) {
            throw new IllegalArgumentException(value);
        }
    }

    static class LinkImpl extends Link {
        private static final Set<String> MAIN_PARAMETERS =
            new HashSet<>(Arrays.asList(Link.REL, Link.TITLE, Link.TYPE));

        private URI uri;
        private Map<String, String> params;
        LinkImpl(URI uri, Map<String, String> params) {
            this.uri = uri;
            this.params = params;
        }

        @Override
        public Map<String, String> getParams() {
            return Collections.unmodifiableMap(params);
        }

        @Override
        public String getRel() {
            return params.get(Link.REL);
        }

        @Override
        public List<String> getRels() {
            String rel = getRel();
            if (rel == null) {
                return Collections.<String>emptyList();
            }
            String[] values = rel.split(" ");
            List<String> rels = new ArrayList<>(values.length);
            for (String val : values) {
                rels.add(val.trim());
            }
            return rels;
        }

        @Override
        public String getTitle() {
            return params.get(Link.TITLE);
        }

        @Override
        public String getType() {
            return params.get(Link.TYPE);
        }

        @Override
        public URI getUri() {
            return uri;
        }

        @Override
        public UriBuilder getUriBuilder() {
            return UriBuilder.fromUri(uri);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('<').append(uri.toString()).append('>');
            String rel = getRel();
            if (rel != null) {
                sb.append(';').append(Link.REL).append("=\"").append(rel).append('"');
            }
            String title = getTitle();
            if (title != null) {
                sb.append(';').append(Link.TITLE).append("=\"").append(title).append('"');
            }
            String type = getType();
            if (type != null) {
                sb.append(';').append(Link.TYPE).append("=\"").append(type).append('"');
            }
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!MAIN_PARAMETERS.contains(entry.getKey())) {
                    sb.append(';').append(entry.getKey()).append("=\"")
                        .append(entry.getValue()).append('"');
                }
            }
            return sb.toString();
        }

        @Override
        public int hashCode() {
            return uri.hashCode() + 37 * params.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Link) {
                Link other = (Link)o;
                return uri.equals(other.getUri())
                    && getParams().equals(other.getParams());
            }
            return false;
        }
    }

    @Override
    public Builder baseUri(URI uri) {
        this.baseUri = uri;
        return this;
    }

    @Override
    public Builder baseUri(String uri) {
        baseUri = URI.create(uri);
        return this;
    }
}
