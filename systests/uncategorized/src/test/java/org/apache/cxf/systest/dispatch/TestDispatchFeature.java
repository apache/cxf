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

package org.apache.cxf.systest.dispatch;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;


public class TestDispatchFeature extends AbstractFeature {
    private static final TestInInterceptor IN = new TestInInterceptor();
    private static final TestOutInterceptor OUT = new TestOutInterceptor();
    private static int count;
    
    TestDispatchFeature() {
        ++count;
    }
    
    public static int getCount() {
        return count;
    }

    public static int getInInterceptorCount() {
        return TestInInterceptor.count;
    }

    public static int getOutInterceptorCount() {
        return TestOutInterceptor.count;
    }

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        provider.getInInterceptors().add(IN);
        provider.getOutInterceptors().add(OUT);
    }

    static class TestInInterceptor extends AbstractPhaseInterceptor<Message> {
        private static int count;
        public TestInInterceptor() {
            super(Phase.RECEIVE);
        }
        public void handleMessage(Message message) throws Fault {
            ++count;
        }
    }

    static class TestOutInterceptor extends AbstractPhaseInterceptor<Message> {
        private static int count;
        public TestOutInterceptor() {
            super(Phase.SEND);
        }
        public void handleMessage(Message message) throws Fault {
            ++count;            
        }
    }
}

