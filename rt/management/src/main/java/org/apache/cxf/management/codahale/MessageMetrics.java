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

package org.apache.cxf.management.codahale;

import java.util.Deque;
import java.util.LinkedList;

import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;

/**
 * 
 */
public class MessageMetrics {
    Deque<MetricsContext> contexts = new LinkedList<MetricsContext>();
    boolean started;
    long startTime = -1;
    
    public MessageMetrics() {
    }

    public MessageMetrics addContext(MetricsContext ctx) {
        contexts.addLast(ctx);
        if (started) {
            ctx.start();
        }
        return this;
    }
    
    public void start() {
        started = true;
        startTime = System.nanoTime();
        for (MetricsContext ctx : contexts) {
            ctx.start();
        }
    }
    
    public void stop(Message m) {
        started = false;
        if (startTime == -1) {
            return;
        }
        FaultMode fm = m.getExchange().get(FaultMode.class);       
        CountingInputStream in = m.getExchange().get(CountingInputStream.class);
        long inSize = -1;
        long outSize = -1;
        if (in != null) {
            inSize = in.getCount();
        }
        CountingOutputStream out = m.getExchange().get(CountingOutputStream.class);
        if (out != null) {
            outSize = out.getCount();
        }
        long l = System.nanoTime() - startTime;
        for (MetricsContext ctx : contexts) {
            ctx.stop(l, inSize, outSize, fm);
        }
    }
    
}
