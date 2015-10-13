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
package org.apache.cxf.rs.security.jose.jws;

import java.util.Map;

import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseType;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;

public class JwsHeaders extends JoseHeaders {
    public JwsHeaders() {
    }
    public JwsHeaders(JoseType type) {
        super(type);
    }
    public JwsHeaders(JoseHeaders headers) {
        super(headers.asMap());
    }
    
    public JwsHeaders(Map<String, Object> values) {
        super(values);
    }
    public JwsHeaders(SignatureAlgorithm sigAlgo) {
        init(sigAlgo);
    }
    public JwsHeaders(JoseType type, SignatureAlgorithm sigAlgo) {
        super(type);
        init(sigAlgo);
    }
    private void init(SignatureAlgorithm sigAlgo) {
        setSignatureAlgorithm(sigAlgo);
    }
    
    public void setSignatureAlgorithm(SignatureAlgorithm algo) {
        super.setAlgorithm(algo.getJwaName());
    }
    
    public SignatureAlgorithm getSignatureAlgorithm() {
        String algo = super.getAlgorithm();
        return algo == null ? null : SignatureAlgorithm.getAlgorithm(algo);
    }
    public void setPayloadEncodingStatus(Boolean status) {
        super.setProperty(JoseConstants.JWS_HEADER_B64_STATUS_HEADER, status);
    }
    public Boolean getPayloadEncodingStatus() {
        return super.getBooleanProperty(JoseConstants.JWS_HEADER_B64_STATUS_HEADER);
    }
}
