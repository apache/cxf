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

package org.apache.cxf.transport.http.policy;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.transport.http.policy.impl.ClientPolicyCalculator;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertionBuilder;
import org.apache.neethi.Assertion;
import org.apache.neethi.Constants;
import org.apache.neethi.PolicyComponent;

/**
 *
 */
@NoJSR250Annotations
public class HTTPClientAssertionBuilder extends JaxbAssertionBuilder<HTTPClientPolicy> {
    public static final List<QName> KNOWN_ELEMENTS
        = Collections.singletonList(new ClientPolicyCalculator().getDataClassName());

    public HTTPClientAssertionBuilder() throws JAXBException {
        super(HTTPClientPolicy.class, new ClientPolicyCalculator().getDataClassName());
    }

    @Override
    protected JaxbAssertion<HTTPClientPolicy> buildAssertion() {
        return new HTTPClientPolicyAssertion();
    }

    class HTTPClientPolicyAssertion extends JaxbAssertion<HTTPClientPolicy> {
        HTTPClientPolicyAssertion() {
            super(new ClientPolicyCalculator().getDataClassName(), false);
        }

        @Override
        public boolean equal(PolicyComponent policyComponent) {
            if (policyComponent == this) {
                return true;
            }
            if (policyComponent.getType() != Constants.TYPE_ASSERTION
                || !(policyComponent instanceof Assertion)
                || !getName().equals(((Assertion)policyComponent).getName())) {
                return false;
            }
            JaxbAssertion<HTTPClientPolicy> other = JaxbAssertion.cast((Assertion)policyComponent);
            return new ClientPolicyCalculator().equals(this.getData(), other.getData());
        }

        @Override
        protected Assertion clone(boolean optional) {
            HTTPClientPolicyAssertion a = new HTTPClientPolicyAssertion();
            a.setData(getData());
            return a;
        }
    }


}

