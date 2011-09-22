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

package org.apache.cxf.ws.policy;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * This policy interceptor provider can be used to implicitly handle unknown policy assertions.
 */
public class IgnorablePolicyInterceptorProvider extends AbstractPolicyInterceptorProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(IgnorablePolicyInterceptorProvider.class); 

    private IgnorableAssertionsInterceptor interceptor = new IgnorableAssertionsInterceptor();
    
    /**
     * @param type
     */
    public IgnorablePolicyInterceptorProvider(QName type) {
        this(Collections.singletonList(type));
    }

    /**
     * @param at
     */
    public IgnorablePolicyInterceptorProvider(Collection<QName> at) {
        super(at);
        
        getInInterceptors().add(interceptor);
        getOutInterceptors().add(interceptor);
        getInFaultInterceptors().add(interceptor);
        getOutFaultInterceptors().add(interceptor);
    }
    
    private class IgnorableAssertionsInterceptor 
        extends AbstractPhaseInterceptor<Message> {

        public IgnorableAssertionsInterceptor() {
            // somewhat irrelevant 
            super(Phase.POST_LOGICAL);
        }
        
        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            for (QName an : getAssertionTypes()) {
                Collection<AssertionInfo> ais = aim.getAssertionInfo(an);
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info("Asserting for " + an);
                }
                if (null != ais) {
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                }
            }
        }
    }

}
