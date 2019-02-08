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
package org.apache.cxf.metrics.interceptors;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.phase.Phase;

public class MetricsMessageOutInterceptor extends AbstractMetricsInterceptor {
    public MetricsMessageOutInterceptor(MetricsProvider[] p) {
        super(Phase.PREPARE_SEND_ENDING, p);
        addBefore(MessageSenderInterceptor.MessageSenderEndingInterceptor.class.getName());
    }
    public void handleMessage(Message message) throws Fault {
        if (!isRequestor(message)) {
            stop(message);
        } else if (message.getExchange().isOneWay()) {
            //one way on the client, it's sent, now stop
            stop(message);
        }
    }
}