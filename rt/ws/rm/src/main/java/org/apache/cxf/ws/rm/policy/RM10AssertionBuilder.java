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

package org.apache.cxf.ws.rm.policy;

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBException;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertionBuilder;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;
import org.apache.neethi.Assertion;
import org.apache.neethi.Constants;
import org.apache.neethi.PolicyComponent;

/**
 * Policy assertion builder for WS-RMP 1.0 (submission). Since this version of WS-RMP nests everything as
 * direct child elements of the RMAssertion JAXB can be used directly to convert to/from XML.
 */
public class RM10AssertionBuilder extends JaxbAssertionBuilder<RMAssertion> {
    public static final List<QName> KNOWN_ELEMENTS
        = Collections.singletonList(RM10Constants.WSRMP_RMASSERTION_QNAME);

    public RM10AssertionBuilder() throws JAXBException {
        super(RMAssertion.class, RM10Constants.WSRMP_RMASSERTION_QNAME);
    }

    @Override
    protected JaxbAssertion<RMAssertion> buildAssertion() {
        return new RMPolicyAssertion();
    }

    class RMPolicyAssertion extends JaxbAssertion<RMAssertion> {
        RMPolicyAssertion() {
            super(RM10Constants.WSRMP_RMASSERTION_QNAME, false);
        }
        RMPolicyAssertion(boolean opt) {
            super(RM10Constants.WSRMP_RMASSERTION_QNAME, opt);
        }
        RMPolicyAssertion(boolean opt, boolean ignore) {
            super(RM10Constants.WSRMP_RMASSERTION_QNAME, opt, ignore);
        }

        @Override
        public boolean equal(PolicyComponent policyComponent) {
            if (policyComponent.getType() != Constants.TYPE_ASSERTION
                || !getName().equals(((Assertion)policyComponent).getName())) {
                return false;
            }
            JaxbAssertion<RMAssertion> other =
                    JaxbAssertion.cast((Assertion)policyComponent);
            return RMPolicyUtilities.equals(this.getData(), other.getData());
        }

        @Override
        protected Assertion clone(boolean b) {
            RMPolicyAssertion a = new RMPolicyAssertion();
            a.setData(getData());
            return a;
        }
    }
}
