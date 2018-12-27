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

package org.apache.cxf.systest.ws.policy;

import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.recorders.InMessageRecorder;
import org.apache.cxf.testutil.recorders.OutMessageRecorder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;

import static org.junit.Assert.assertEquals;

/**
 * Base class for testing WS-Policy Framework using engage WS-RM Policy to engage WS-RM.
 */
public class RMPolicyWsdlTestBase extends AbstractBusClientServerTestBase {
    protected static final String GREETMEONEWAY_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeOneWayRequest";
    protected static final String GREETME_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeRequest";
    protected static final String GREETME_RESPONSE_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeResponse";
    protected static final String PINGME_ACTION = "http://cxf.apache.org/greeter_control/Greeter/pingMeRequest";
    protected static final String PINGME_RESPONSE_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/pingMeResponse";
    protected static final String GREETER_FAULT_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/pingMe/Fault/faultDetail";

    private static final Logger LOG = LogUtils.getLogger(RMPolicyWsdlTestBase.class);

    protected OutMessageRecorder outRecorder;
    protected InMessageRecorder inRecorder;

    public abstract static class ServerBase extends AbstractBusTestServerBase {

        protected abstract String getConfigPath();

        protected void run()  {
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus(getConfigPath());
            BusFactory.setDefaultBus(bus);
            setBus(bus);

            ServerRegistry sr = bus.getExtension(ServerRegistry.class);
            PolicyEngine pe = bus.getExtension(PolicyEngine.class);

            List<PolicyAssertion> assertions1
                = getAssertions(pe, sr.getServers().get(0));
            assertEquals("2 assertions should be available", 2, assertions1.size());
            List<PolicyAssertion> assertions2 =
                getAssertions(pe, sr.getServers().get(1));
            assertEquals("1 assertion should be available", 1, assertions2.size());

            LOG.info("Published greeter endpoints.");
        }

        protected List<PolicyAssertion> getAssertions(PolicyEngine pe, org.apache.cxf.endpoint.Server s) {
            Policy p1 = pe.getServerEndpointPolicy(
                             s.getEndpoint().getEndpointInfo(), null, null).getPolicy();
            List<ExactlyOne> pops =
                CastUtils.cast(p1.getPolicyComponents(), ExactlyOne.class);
            assertEquals("New policy must have 1 top level policy operator", 1, pops.size());
            List<All> alts =
                CastUtils.cast(pops.get(0).getPolicyComponents(), All.class);
            assertEquals("1 alternatives should be available", 1, alts.size());
            return CastUtils.cast(alts.get(0).getAssertions(), PolicyAssertion.class);
        }
    }

    public void setUpBus(String port) throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("org/apache/cxf/systest/ws/policy/rmwsdl.xml");
        BusFactory.setDefaultBus(bus);
        outRecorder = new OutMessageRecorder();
        bus.getOutInterceptors().add(outRecorder);
        inRecorder = new InMessageRecorder();
        bus.getInInterceptors().add(inRecorder);
    }
}
