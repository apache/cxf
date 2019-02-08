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
package org.apache.cxf.rs.security.jose.jwk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.rs.security.jose.common.JoseConstants;


public class JsonWebKey extends JsonMapObject {

    public static final String KEY_TYPE = "kty";
    public static final String PUBLIC_KEY_USE = "use";
    public static final String KEY_OPERATIONS = "key_ops";
    public static final String KEY_ALGO = JoseConstants.HEADER_ALGORITHM;
    public static final String KEY_ID = JoseConstants.HEADER_KEY_ID;
    public static final String X509_URL = JoseConstants.HEADER_X509_URL;
    public static final String X509_CHAIN = JoseConstants.HEADER_X509_CHAIN;
    public static final String X509_THUMBPRINT = JoseConstants.HEADER_X509_THUMBPRINT;
    public static final String X509_THUMBPRINT_SHA256 = JoseConstants.HEADER_X509_THUMBPRINT_SHA256;

    public static final String KEY_TYPE_RSA = "RSA";
    public static final String RSA_MODULUS = "n";
    public static final String RSA_PUBLIC_EXP = "e";
    public static final String RSA_PRIVATE_EXP = "d";
    public static final String RSA_FIRST_PRIME_FACTOR = "p";
    public static final String RSA_SECOND_PRIME_FACTOR = "q";
    public static final String RSA_FIRST_PRIME_CRT = "dp";
    public static final String RSA_SECOND_PRIME_CRT = "dq";
    public static final String RSA_FIRST_CRT_COEFFICIENT = "qi";

    public static final String KEY_TYPE_OCTET = "oct";
    public static final String OCTET_KEY_VALUE = "k";

    public static final String KEY_TYPE_ELLIPTIC = "EC";
    public static final String EC_CURVE = "crv";
    public static final String EC_CURVE_P256 = "P-256";
    public static final String EC_CURVE_P384 = "P-384";
    public static final String EC_CURVE_P521 = "P-521";
    public static final String EC_X_COORDINATE = "x";
    public static final String EC_Y_COORDINATE = "y";
    public static final String EC_PRIVATE_KEY = "d";

    public static final String PUBLIC_KEY_USE_SIGN = "sig";
    public static final String PUBLIC_KEY_USE_ENCRYPT = "enc";

    public static final String KEY_OPER_SIGN = "sign";
    public static final String KEY_OPER_VERIFY = "verify";
    public static final String KEY_OPER_ENCRYPT = "encrypt";
    public static final String KEY_OPER_DECRYPT = "decrypt";
    public static final String KEY_OPER_WRAP_KEY = "wrapKey";
    public static final String KEY_OPER_UNWRAP_KEY = "unwrapKey";
    public static final String KEY_OPER_DERIVE_KEY = "deriveKey";
    public static final String KEY_OPER_DERIVE_BITS = "deriveBits";

    private static final long serialVersionUID = 3201315996547826368L;

    public JsonWebKey() {

    }

    public JsonWebKey(Map<String, Object> values) {
        super(values);
    }

    public void setKeyType(KeyType keyType) {
        setProperty(KEY_TYPE, keyType.toString());
    }

    public KeyType getKeyType() {
        Object prop = getProperty(KEY_TYPE);
        return prop == null ? null : KeyType.getKeyType(prop.toString());
    }

    public void setPublicKeyUse(PublicKeyUse use) {
        setProperty(PUBLIC_KEY_USE, use.toString());
    }

    public PublicKeyUse getPublicKeyUse() {
        Object prop = getProperty(PUBLIC_KEY_USE);
        return prop == null ? null : PublicKeyUse.getPublicKeyUse(prop.toString());
    }

    public void setKeyOperation(List<KeyOperation> keyOperation) {
        List<String> ops = new ArrayList<>(keyOperation.size());
        for (KeyOperation op : keyOperation) {
            ops.add(op.toString());
        }
        setProperty(KEY_OPERATIONS, ops);
    }

    public List<KeyOperation> getKeyOperation() {
        List<Object> ops = CastUtils.cast((List<?>)getProperty(KEY_OPERATIONS));
        if (ops == null) {
            return null;
        }
        List<KeyOperation> keyOps = new ArrayList<>(ops.size());
        for (Object op : ops) {
            keyOps.add(KeyOperation.getKeyOperation(op.toString()));
        }
        return keyOps;
    }

    public void setAlgorithm(String algorithm) {
        setProperty(KEY_ALGO, algorithm);
    }

    public String getAlgorithm() {
        return (String)getProperty(KEY_ALGO);
    }

    public void setKeyId(String kid) {
        setProperty(KEY_ID, kid);
    }

    public String getKeyId() {
        return (String)getProperty(KEY_ID);
    }

    public void setX509Url(String x509Url) {
        setProperty(X509_URL, x509Url);
    }

    public String getX509Url() {
        return (String)getProperty(X509_URL);
    }

    public void setX509Chain(List<String> x509Chain) {
        setProperty(X509_CHAIN, x509Chain);
    }

    public List<String> getX509Chain() {
        return CastUtils.cast((List<?>)getProperty(X509_CHAIN));
    }

    public void setX509Thumbprint(String x509Thumbprint) {
        setProperty(X509_THUMBPRINT, x509Thumbprint);
    }

    public String getX509Thumbprint() {
        return (String)getProperty(X509_THUMBPRINT);
    }

    public void setX509ThumbprintSHA256(String x509Thumbprint) {
        setProperty(X509_THUMBPRINT_SHA256, x509Thumbprint);
    }

    public String getX509ThumbprintSHA256() {
        return (String)getProperty(X509_THUMBPRINT_SHA256);
    }

    public JsonWebKey setKeyProperty(String name, Object value) {
        setProperty(name, value);
        return this;
    }
    public Object getKeyProperty(String name) {
        return getProperty(name);
    }



}
