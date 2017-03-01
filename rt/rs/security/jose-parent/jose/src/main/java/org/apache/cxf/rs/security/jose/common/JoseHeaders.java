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

package org.apache.cxf.rs.security.jose.common;

import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public abstract class JoseHeaders extends JsonMapObject {
    private static final long serialVersionUID = 1101185302425283553L;

    public JoseHeaders() {
    }

    public JoseHeaders(JoseType type) {
        init(type);
    }

    public JoseHeaders(JoseHeaders headers) {
        this(headers.asMap());
    }

    public JoseHeaders(Map<String, Object> values) {
        super(values);
    }
    private void init(JoseType type) {
        setType(type);
    }
    public final void setType(JoseType type) {
        setHeader(JoseConstants.HEADER_TYPE, type.toString());
    }

    public JoseType getType() {
        Object prop = getHeader(JoseConstants.HEADER_TYPE);
        return prop == null ? null : JoseType.getType(prop.toString());
    }

    public void setContentType(String type) {
        setHeader(JoseConstants.HEADER_CONTENT_TYPE, type);
    }

    public String getContentType() {
        return (String)getHeader(JoseConstants.HEADER_CONTENT_TYPE);
    }

    public void setAlgorithm(String algo) {
        setHeader(JoseConstants.HEADER_ALGORITHM, algo);
    }

    public String getAlgorithm() {
        Object prop = getHeader(JoseConstants.HEADER_ALGORITHM);
        return prop == null ? null : prop.toString();
    }

    public void setKeyId(String kid) {
        setHeader(JoseConstants.HEADER_KEY_ID, kid);
    }

    public String getKeyId() {
        return (String)getHeader(JoseConstants.HEADER_KEY_ID);
    }

    public void setX509Url(String x509Url) {
        setHeader(JoseConstants.HEADER_X509_URL, x509Url);
    }

    public String getX509Url() {
        return (String)getHeader(JoseConstants.HEADER_X509_URL);
    }

    public void setX509Chain(List<String> x509Chain) {
        setProperty(JoseConstants.HEADER_X509_CHAIN, x509Chain);
    }

    public List<String> getX509Chain() {
        return CastUtils.cast((List<?>)getProperty(JoseConstants.HEADER_X509_CHAIN));
    }

    public void setX509Thumbprint(String x509Thumbprint) {
        setHeader(JoseConstants.HEADER_X509_THUMBPRINT, x509Thumbprint);
    }

    public String getX509Thumbprint() {
        return (String)getHeader(JoseConstants.HEADER_X509_THUMBPRINT);
    }

    public void setX509ThumbprintSHA256(String x509Thumbprint) {
        setHeader(JoseConstants.HEADER_X509_THUMBPRINT_SHA256, x509Thumbprint);
    }

    public String getX509ThumbprintSHA256() {
        return (String)getHeader(JoseConstants.HEADER_X509_THUMBPRINT_SHA256);
    }

    public void setCritical(List<String> crit) {
        setHeader(JoseConstants.HEADER_CRITICAL, crit);
    }

    public List<String> getCritical() {
        return CastUtils.cast((List<?>)getHeader(JoseConstants.HEADER_CRITICAL));
    }

    public void setJsonWebKey(JsonWebKey key) {
        setJsonWebKey(JoseConstants.HEADER_JSON_WEB_KEY, key);
    }

    public void setJsonWebKey(String headerName, JsonWebKey key) {
        setHeader(headerName, key);
    }

    public void setJsonWebKeysUrl(String url) {
        setHeader(JoseConstants.HEADER_JSON_WEB_KEY_SET, url);
    }

    public String getJsonWebKeysUrl() {
        return (String)getHeader(JoseConstants.HEADER_JSON_WEB_KEY_SET);
    }

    public JsonWebKey getJsonWebKey() {
        return getJsonWebKey(JoseConstants.HEADER_JSON_WEB_KEY);
    }
    public JsonWebKey getJsonWebKey(String headerName) {
        Object jsonWebKey = getHeader(headerName);
        if (jsonWebKey == null || jsonWebKey instanceof JsonWebKey) {
            return (JsonWebKey)jsonWebKey;
        }
        Map<String, Object> map = CastUtils.cast((Map<?, ?>)jsonWebKey);
        return new JsonWebKey(map);
    }

    public final JoseHeaders setHeader(String name, Object value) {
        setProperty(name, value);
        return this;
    }

    public Object getHeader(String name) {
        return getProperty(name);
    }

    public JoseHeaders setIntegerHeader(String name, Integer value) {
        setHeader(name, value);
        return this;
    }

    public Integer getIntegerHeader(String name) {
        return getIntegerProperty(name);
    }
    public JoseHeaders setLongHeader(String name, Long value) {
        setHeader(name, value);
        return this;
    }

    public Long getLongHeader(String name) {
        return getLongProperty(name);
    }

    public boolean containsHeader(String name) {
        return containsProperty(name);
    }

}
