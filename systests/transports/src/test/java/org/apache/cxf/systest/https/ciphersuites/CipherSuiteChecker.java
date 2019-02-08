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

package org.apache.cxf.systest.https.ciphersuites;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.transport.TLSSessionInfo;

/**
 * A service side interceptor to check that the negotiated TLS protocol matches a desired
 * algorithm
 */
public class CipherSuiteChecker extends AbstractPhaseInterceptor<Message> {

    private String requiredAlgorithm;

    public CipherSuiteChecker() {
        super(Phase.PRE_INVOKE);
    }

    public CipherSuiteChecker(String phase) {
        super(phase);
    }

    public void handleMessage(Message mc) throws Fault {
        TLSSessionInfo session = mc.get(TLSSessionInfo.class);
        if (!session.getCipherSuite().contains(requiredAlgorithm)) {
            throw new Fault(new Exception("Required algorithm not found"));
        }
    }

    public String getRequiredAlgorithm() {
        return requiredAlgorithm;
    }

    public void setRequiredAlgorithm(String requiredAlgorithm) {
        this.requiredAlgorithm = requiredAlgorithm;
    }


}
