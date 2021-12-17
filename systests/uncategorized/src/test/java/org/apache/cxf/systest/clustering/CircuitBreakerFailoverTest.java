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

package org.apache.cxf.systest.clustering;

import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.greeter_control.Greeter;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests failover within a static cluster.
 */
public class CircuitBreakerFailoverTest extends FailoverTest {
    private static final String FAILOVER_CONFIG =
            "org/apache/cxf/systest/clustering/circuit_breaker_failover.xml";

    protected String getConfig() {
        return FAILOVER_CONFIG;
    }

    @Test
    public void testWithNoAlternativeEndpoints() throws Exception {
        final Greeter g = getGreeter(REPLICA_E);

        try {
            g.greetMe("fred");
            fail("Expecting communication exception");
        } catch (WebServiceException ex) {
            assertThat(ex.getMessage(), equalTo("Could not send Message."));
        }

        try {
            g.greetMe("fred");
            fail("Expecting no alternative endpoints exception");
        } catch (SOAPFaultException ex) {
            assertThat(ex.getMessage(), equalTo("None of alternative addresses are available at the moment"));
        }
    }

    @Test
    public void testWithAlternativeEnpdpoints() throws Exception {
        final Greeter g = getGreeter(REPLICA_A);
        startTarget(REPLICA_E);

        try {
            final String response = g.greetMe("fred");
            assertNotNull("expected non-null response", response);
        } finally {
            stopTarget(REPLICA_E);
        }

        try {
            g.greetMe("fred");
            fail("Expecting no alternative endpoints exception");
        } catch (WebServiceException ex) {
            assertThat(ex.getMessage(), equalTo("Could not send Message."));
        }
    }
}
