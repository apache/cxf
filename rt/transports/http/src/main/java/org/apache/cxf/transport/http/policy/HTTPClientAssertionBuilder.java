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

import javax.xml.bind.JAXBException;

import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertionBuilder;
import org.apache.neethi.Constants;
import org.apache.neethi.PolicyComponent;

/**
 * 
 */
public class HTTPClientAssertionBuilder extends JaxbAssertionBuilder<HTTPClientPolicy> {
 
    public HTTPClientAssertionBuilder() throws JAXBException {
        super(HTTPClientPolicy.class, PolicyUtils.HTTPCLIENTPOLICY_ASSERTION_QNAME);     
    }

    @Override
    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        if (PolicyUtils.HTTPCLIENTPOLICY_ASSERTION_QNAME.equals(a.getName())
            && PolicyUtils.HTTPCLIENTPOLICY_ASSERTION_QNAME.equals(b.getName())) {
            
            HTTPClientPolicy compatible = PolicyUtils.intersect(
                JaxbAssertion.cast(a, HTTPClientPolicy.class).getData(),
                JaxbAssertion.cast(b, HTTPClientPolicy.class).getData());
            if (null == compatible) {
                return null;
            }
            JaxbAssertion<HTTPClientPolicy> ca = 
                new JaxbAssertion<HTTPClientPolicy>(PolicyUtils.HTTPCLIENTPOLICY_ASSERTION_QNAME, 
                    a.isOptional() && b.isOptional());
            ca.setData(compatible);
            return ca;
        }
        return null;
    }

    @Override
    protected JaxbAssertion<HTTPClientPolicy> buildAssertion() {
        return new HTTPClientPolicyAssertion();
    }
    
    class HTTPClientPolicyAssertion extends JaxbAssertion<HTTPClientPolicy> {
        HTTPClientPolicyAssertion() {
            super(PolicyUtils.HTTPCLIENTPOLICY_ASSERTION_QNAME, false);
        }

        @Override
        public boolean equal(PolicyComponent policyComponent) {
            if (policyComponent.getType() != Constants.TYPE_ASSERTION
                || !getName().equals(((PolicyAssertion)policyComponent).getName())) {
                return false;
            }
            JaxbAssertion<HTTPClientPolicy> other = JaxbAssertion.cast((PolicyAssertion)policyComponent);
            return PolicyUtils.equals(this.getData(), other.getData());  
        }
        
        @Override
        protected PolicyAssertion cloneMandatory() {
            HTTPClientPolicyAssertion a = new HTTPClientPolicyAssertion();
            a.setData(getData());
            return a;        
        }
    }
    
    
    
    
}
