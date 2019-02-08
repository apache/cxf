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
package org.apache.cxf.ws.security.wss4j;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.neethi.Assertion;
import org.apache.wss4j.policy.stax.PolicyAsserter;

/**
 * Assert policies in CXF that are asserted in the WSS4J policy stax module
 */
public class WSS4JPolicyAsserter implements PolicyAsserter {

    private AssertionInfoMap aim;

    public WSS4JPolicyAsserter(AssertionInfoMap aim) {
        this.aim = aim;
    }

    public void assertPolicy(Assertion assertion) {
        Collection<AssertionInfo> ais = aim.getAssertionInfo(assertion.getName());
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setAsserted(true);
                }
            }
        }
    }

    public void assertPolicy(QName qName) {
        Collection<AssertionInfo> ais = aim.getAssertionInfo(qName);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }
        }
    }

    @Override
    public void unassertPolicy(Assertion assertion, String reason) {
        Collection<AssertionInfo> ais = aim.getAssertionInfo(assertion.getName());
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setNotAsserted(reason);
                }
            }
        }
    }

    @Override
    public void unassertPolicy(QName qName, String reason) {
        Collection<AssertionInfo> ais = aim.getAssertionInfo(qName);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setNotAsserted(reason);
            }
        }
    }

}
