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
package org.apache.cxf.ws.security.trust;

import org.apache.neethi.Policy;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.EncryptionToken;
import org.apache.wss4j.policy.model.ProtectionToken;
import org.apache.wss4j.policy.model.SignatureToken;
import org.apache.wss4j.policy.model.SymmetricBinding;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class DefaultSymmetricBinding extends SymmetricBinding {

    public DefaultSymmetricBinding(SPConstants.SPVersion version, Policy nestedPolicy) {
        super(version, nestedPolicy);
    }

    public void setEncryptionToken(EncryptionToken encryptionToken) {
        super.setEncryptionToken(encryptionToken);
    }

    public void setSignatureToken(SignatureToken signatureToken) {
        super.setSignatureToken(signatureToken);
    }

    public void setProtectionToken(ProtectionToken protectionToken) {
        super.setProtectionToken(protectionToken);
    }

    public void setOnlySignEntireHeadersAndBody(boolean onlySignEntireHeadersAndBody) {
        super.setOnlySignEntireHeadersAndBody(onlySignEntireHeadersAndBody);
    }

    public void setProtectTokens(boolean protectTokens) {
        super.setProtectTokens(protectTokens);
    }

    public void setIncludeTimestamp(boolean includeTimestamp) {
        super.setIncludeTimestamp(includeTimestamp);
    }

    public void setAlgorithmSuite(AlgorithmSuite algorithmSuite) {
        super.setAlgorithmSuite(algorithmSuite);
    }
}
