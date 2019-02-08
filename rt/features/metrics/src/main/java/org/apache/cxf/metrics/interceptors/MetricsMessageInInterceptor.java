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

import java.io.InputStream;

import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.metrics.ExchangeMetrics;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.phase.Phase;

public class MetricsMessageInInterceptor extends AbstractMetricsInterceptor {
    public MetricsMessageInInterceptor(MetricsProvider[] p) {
        super(Phase.RECEIVE, p);
        addBefore(AttachmentInInterceptor.class.getName());
    }
    public void handleMessage(Message message) throws Fault {
        if (!isRequestor(message)) {
            ExchangeMetrics ctx = getExchangeMetrics(message, true);
            InputStream in = message.getContent(InputStream.class);
            if (in != null) {
                CountingInputStream newIn = new CountingInputStream(in);
                message.setContent(InputStream.class, newIn);
                message.getExchange().put(CountingInputStream.class, newIn);
            }
            ctx.start();
        }
    }
    public void handleFault(Message message) {
        if (isRequestor(message)) {
            //fault on the incoming chains for the client.  It will be thrown back to client so stop
            stop(message);
        } else if (message.getExchange().isOneWay()) {
            stop(message);
        }
    }
}