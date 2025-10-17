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

package org.apache.cxf.metrics;

import java.util.Deque;
import java.util.LinkedList;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.metrics.interceptors.CountingInputStream;
import org.apache.cxf.metrics.interceptors.CountingOutputStream;

/**
 *
 */
public class ExchangeMetrics {
    Deque<MetricsContext> contexts = new LinkedList<>();
    Exchange exchange;
    boolean started;
    long startTime = -1;

    public ExchangeMetrics(Exchange e) {
        exchange = e;
    }

    public ExchangeMetrics addContext(MetricsContext ctx) {
        if (!contexts.contains(ctx)) {
            contexts.addLast(ctx);
            if (started) {
                ctx.start(exchange);
            }
        }
        return this;
    }

    public void start() {
        started = true;
        startTime = System.nanoTime();
        for (MetricsContext ctx : contexts) {
            ctx.start(exchange);
        }
    }

    public void stop() {
        started = false;
        if (startTime == -1) {
            return;
        }
        CountingInputStream in = exchange.get(CountingInputStream.class);
        long inSize = -1;
        long outSize = -1;
        if (in != null) {
            inSize = in.getCount();
        }
        CountingOutputStream out = exchange.get(CountingOutputStream.class);
        if (out != null) {
            outSize = out.getCount();
        }
        long l = System.nanoTime() - startTime;
        for (MetricsContext ctx : contexts) {
            ctx.stop(l, inSize, outSize, exchange);
        }
    }

}
