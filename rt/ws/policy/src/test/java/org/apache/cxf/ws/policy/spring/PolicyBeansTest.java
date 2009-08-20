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
package org.apache.cxf.ws.policy.spring;

import java.util.Collection;

import junit.framework.Assert;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyEngineImpl;
import org.apache.cxf.ws.policy.PolicyProvider;
import org.apache.cxf.ws.policy.attachment.external.ExternalAttachmentProvider;
import org.apache.cxf.ws.policy.selector.MaximalAlternativeSelector;
import org.junit.Test;

public class PolicyBeansTest extends Assert {
    

    @Test
    public void testParse() {
        Bus bus = new SpringBusFactory().createBus("org/apache/cxf/ws/policy/spring/beans.xml");
        try {
            PolicyEngine pe = bus.getExtension(PolicyEngine.class);
            assertTrue("Policy engine is not enabled", pe.isEnabled());
            assertTrue("Unknown assertions are not ignored", pe.isIgnoreUnknownAssertions());
            
            assertEquals(MaximalAlternativeSelector.class.getName(), 
                         pe.getAlternativeSelector().getClass().getName()); 
            
            
            PolicyEngineImpl pei = (PolicyEngineImpl)pe;
            Collection<PolicyProvider> providers = pei.getPolicyProviders();
            assertEquals(4, providers.size());
            int n = 0;
            for (PolicyProvider pp : providers) {
                if (pp instanceof ExternalAttachmentProvider) {
                    n++;
                }
            }
            assertEquals("Unexpected number of external providers", 2, n);
        } finally {
            bus.shutdown(true);
        }
    }
        
    
   
}
