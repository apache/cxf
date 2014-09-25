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

package org.apache.cxf.rs.security.jose;

import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class JoseHeaders extends AbstractJoseObject {
    
    public JoseHeaders() {
    }
    
    public JoseHeaders(Map<String, Object> values) {
        super(values);
    }
    
    public void setType(String type) {
        setHeader(JoseConstants.HEADER_TYPE, type);
    }
    
    public String getType() {
        return (String)getHeader(JoseConstants.HEADER_TYPE);
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
        return (String)getHeader(JoseConstants.HEADER_ALGORITHM);
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
    
    public void setX509Chain(String x509Chain) {
        setHeader(JoseConstants.HEADER_X509_CHAIN, x509Chain);
    }

    public String getX509Chain() {
        return (String)getHeader(JoseConstants.HEADER_X509_CHAIN);
    }
    
    public void setX509Thumbprint(String x509Thumbprint) {
        setHeader(JoseConstants.HEADER_X509_THUMBPRINT, x509Thumbprint);
    }
    
    public String getX509Thumbprint() {
        return (String)getHeader(JoseConstants.HEADER_X509_THUMBPRINT);
    }
    
    public void setX509ThumbprintSHA256(String x509Thumbprint) {
        super.setValue(JoseConstants.HEADER_X509_THUMBPRINT_SHA256, x509Thumbprint);
    }
    
    public String getX509ThumbprintSHA256() {
        return (String)super.getValue(JoseConstants.HEADER_X509_THUMBPRINT_SHA256);
    }
    
    public void setCritical(List<String> crit) {
        setHeader(JoseConstants.HEADER_CRITICAL, crit);
    }
    
    public List<String> getCritical() {
        return CastUtils.cast((List<?>)getHeader(JoseConstants.HEADER_CRITICAL));
    }
    
    public void setJsonWebKey(JsonWebKey key) {
        setValue(JoseConstants.HEADER_JSON_WEB_KEY, key);
    }
    
    public JsonWebKey getJsonWebKey() {
        Object jsonWebKey = getValue(JoseConstants.HEADER_JSON_WEB_KEY);
        if (jsonWebKey == null || jsonWebKey instanceof JsonWebKey) {
            return (JsonWebKey)jsonWebKey;
        }  
        Map<String, Object> map = CastUtils.cast((Map<?, ?>)jsonWebKey);
        return new JsonWebKey(map);
    }
    
    public JoseHeaders setHeader(String name, Object value) {
        setValue(name, value);
        return this;
    }
    
    public Object getHeader(String name) {
        return getValue(name);
    }
    
    public JoseHeaders setIntegerHeader(String name, Integer value) {
        setValue(name, value);
        return this;
    }
    
    public Integer getIntegerHeader(String name) {
        Object value = getValue(name);
        if (value != null) {
            return value instanceof Integer ? (Integer)value : Integer.parseInt(value.toString());
        } else {
            return null;
        }
    }
    public JoseHeaders setLongHeader(String name, Long value) {
        setValue(name, value);
        return this;
    }
    
    public Long getLongHeader(String name) {
        Object value = getValue(name);
        if (value != null) {
            return value instanceof Long ? (Long)value : Long.parseLong(value.toString());
        } else {
            return null;
        }
    }
}
