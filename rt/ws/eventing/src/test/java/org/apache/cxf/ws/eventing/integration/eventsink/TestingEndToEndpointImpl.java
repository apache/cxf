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

package org.apache.cxf.ws.eventing.integration.eventsink;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import jakarta.jws.WebParam;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ws.eventing.SubscriptionEnd;
import org.apache.cxf.ws.eventing.client.EndToEndpoint;

public class TestingEndToEndpointImpl implements EndToEndpoint {

    public static final AtomicInteger RECEIVED_ENDS = new AtomicInteger(0);

    protected static final Logger LOG = LogUtils.getLogger(TestingEndToEndpointImpl.class);


    @Override
    public void subscriptionEnd(@WebParam SubscriptionEnd subscriptionEnd) {
        LOG.info("Received subscription end: " + subscriptionEnd.getStatus());
        RECEIVED_ENDS.incrementAndGet();
    }

}
