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
package org.apache.cxf.rs.security.xml;

import java.util.Map;

public class SignatureProperties {
    private String signatureAlgo;
    private String signatureDigestAlgo;
    private String signatureC14nMethod;
    private String signatureC14nTransform;
    private String signatureKeyIdType;
    private String signatureKeyName;
    private Map<String, String> keyNameAliasMap;
    private Integer signatureLocation;
    private Boolean signatureGenerateIdAttributes;
    private Boolean signatureOmitC14nTransform;

    public void setSignatureAlgo(String signatureAlgo) {
        this.signatureAlgo = signatureAlgo;
    }
    public String getSignatureAlgo() {
        return signatureAlgo;
    }
    public void setSignatureDigestAlgo(String signatureDigestAlgo) {
        this.signatureDigestAlgo = signatureDigestAlgo;
    }
    public String getSignatureDigestAlgo() {
        return signatureDigestAlgo;
    }
    public void setSignatureC14nMethod(String signatureC14nMethod) {
        this.signatureC14nMethod = signatureC14nMethod;
    }
    public String getSignatureC14nMethod() {
        return signatureC14nMethod;
    }
    public void setSignatureC14nTransform(String signatureC14nTransform) {
        this.signatureC14nTransform = signatureC14nTransform;
    }
    public String getSignatureC14nTransform() {
        return signatureC14nTransform;
    }
    public String getSignatureKeyIdType() {
        return signatureKeyIdType;
    }
    public void setSignatureKeyIdType(String signatureKeyIdType) {
        this.signatureKeyIdType = signatureKeyIdType;
    }
    public String getSignatureKeyName() {
        return signatureKeyName;
    }
    public void setSignatureKeyName(String signatureKeyName) {
        this.signatureKeyName = signatureKeyName;
    }

    public Map<String, String> getKeyNameAliasMap() {
        return keyNameAliasMap;
    }

    /**
     * Set the Signature KeyName alias lookup map. It is used on the receiving side for signature.
     * It maps a KeyName to a key alias - so it allows us to associate a (e.g.) key alias in
     * a keystore with a given KeyName contained in a KeyInfo structure of the Signature.
     */
    public void setKeyNameAliasMap(Map<String, String> keyNameAliasMap) {
        this.keyNameAliasMap = keyNameAliasMap;
    }

    public Integer getSignatureLocation() {
        return signatureLocation;
    }

    public void setSignatureLocation(Integer signatureLocation) {
        this.signatureLocation = signatureLocation;
    }

    public Boolean getSignatureGenerateIdAttributes() {
        return signatureGenerateIdAttributes;
    }

    public void setSignatureGenerateIdAttributes(Boolean signatureGenerateIdAttributes) {
        this.signatureGenerateIdAttributes = signatureGenerateIdAttributes;
    }

    public Boolean getSignatureOmitC14nTransform() {
        return signatureOmitC14nTransform;
    }

    public void setSignatureOmitC14nTransform(Boolean signatureOmitC14nTransform) {
        this.signatureOmitC14nTransform = signatureOmitC14nTransform;
    }
}
