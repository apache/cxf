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

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertionBuilder;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.neethi.Constants;
import org.apache.neethi.PolicyComponent;

/**
 * 
 */
public class RMAssertionBuilder extends JaxbAssertionBuilder<RMAssertion> {
    public static final List<QName> KNOWN_ELEMENTS 
        = Collections.singletonList(RMConstants.getRMAssertionQName());

    public RMAssertionBuilder() throws JAXBException {
        super(RMAssertion.class, RMConstants.getRMAssertionQName());     
    }

    @Override
    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        if (RMConstants.getRMAssertionQName().equals(a.getName())
            && RMConstants.getRMAssertionQName().equals(b.getName())) {
            
            RMAssertion compatible = PolicyUtils.intersect(
                JaxbAssertion.cast(a, RMAssertion.class).getData(),
                JaxbAssertion.cast(b, RMAssertion.class).getData());
            if (null == compatible) {
                return null;
            }
            JaxbAssertion<RMAssertion> ca = 
                new JaxbAssertion<RMAssertion>(RMConstants.getRMAssertionQName(), 
                    a.isOptional() && b.isOptional());
            ca.setData(compatible);
            return ca;
        }
        return null;
    }

    @Override
    protected JaxbAssertion<RMAssertion> buildAssertion() {
        return new RMPolicyAssertion();
    }
    
    class RMPolicyAssertion extends JaxbAssertion<RMAssertion> {
        RMPolicyAssertion() {
            super(RMConstants.getRMAssertionQName(), false);
        }

        @Override
        public boolean equal(PolicyComponent policyComponent) {
            if (policyComponent.getType() != Constants.TYPE_ASSERTION
                || !getName().equals(((PolicyAssertion)policyComponent).getName())) {
                return false;
            }
            JaxbAssertion<RMAssertion> other = 
                    JaxbAssertion.cast((PolicyAssertion)policyComponent);            
            return PolicyUtils.equals(this.getData(), other.getData());  
        }
        
        @Override
        protected PolicyAssertion cloneMandatory() {
            RMPolicyAssertion a = new RMPolicyAssertion();
            a.setData(getData());
            return a;        
        }
    }
    
    

        
}

