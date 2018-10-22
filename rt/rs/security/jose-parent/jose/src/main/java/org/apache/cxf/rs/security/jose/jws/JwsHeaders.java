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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseHeaders;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;

public class JwsHeaders extends JoseHeaders {
    private static final long serialVersionUID = 3422779299093961672L;
    public JwsHeaders() {
    }
    public JwsHeaders(JoseType type) {
        super(type);
    }
    public JwsHeaders(JwsHeaders headers) {
        super(headers.asMap());
    }

    public JwsHeaders(Map<String, Object> values) {
        super(values);
    }
    public JwsHeaders(String kid) {
        this(Collections.singletonMap(JoseConstants.HEADER_KEY_ID, (Object)kid));
    }
    public JwsHeaders(SignatureAlgorithm sigAlgo) {
        init(sigAlgo);
    }
    public JwsHeaders(Properties sigProps) {
        init(getSignatureAlgorithm(sigProps));
    }
    public JwsHeaders(JoseType type, SignatureAlgorithm sigAlgo) {
        super(type);
        init(sigAlgo);
    }
    private void init(SignatureAlgorithm sigAlgo) {
        setSignatureAlgorithm(sigAlgo);
    }

    public final void setSignatureAlgorithm(SignatureAlgorithm algo) {
        super.setAlgorithm(algo.getJwaName());
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        String algo = super.getAlgorithm();
        return algo == null ? null : SignatureAlgorithm.getAlgorithm(algo);
    }
    public void setPayloadEncodingStatus(Boolean status) {
        super.setProperty(JoseConstants.JWS_HEADER_B64_STATUS_HEADER, status);
        if (!status) {
            List<String> critical = this.getCritical();
            if (critical == null) {
                critical = new LinkedList<>();
                setCritical(critical);
            } else if (critical.contains(JoseConstants.JWS_HEADER_B64_STATUS_HEADER)) {
                return;
            }
            critical.add(JoseConstants.JWS_HEADER_B64_STATUS_HEADER);

        }
    }
    public Boolean getPayloadEncodingStatus() {
        return super.getBooleanProperty(JoseConstants.JWS_HEADER_B64_STATUS_HEADER);
    }
    private static SignatureAlgorithm getSignatureAlgorithm(Properties sigProps) {
        return JwsUtils.getSignatureAlgorithm(sigProps, null);
    }
}
