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
package org.apache.cxf.systest.ws.wssec11;

import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.WSSPolicyException;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;

/**
 * This AlgorithmSuite supports restricted security policies - by setting the minimum asymmetric
 * key length to be 512.
 */
public class RestrictedAlgorithmSuite extends AlgorithmSuite {
    
    public RestrictedAlgorithmSuite(SPConstants version) {
        super(version);
    }

    public RestrictedAlgorithmSuite() {
        super(SP12Constants.INSTANCE);
    }
    
    /**
     * Set the algorithm suite
     * 
     * @param algoSuite
     * @throws WSSPolicyException
     */
    @Override
    public void setAlgorithmSuite(String algoSuite) throws WSSPolicyException {
        super.setAlgorithmSuite(algoSuite);
        this.minimumAsymmetricKeyLength = 512;
    }
}
