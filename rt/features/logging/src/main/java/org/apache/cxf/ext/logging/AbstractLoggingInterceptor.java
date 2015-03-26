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
package org.apache.cxf.ext.logging;

import java.util.UUID;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

abstract class AbstractLoggingInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final int DEFAULT_LIMIT = 48 * 1024;

    protected int limit = DEFAULT_LIMIT;
    protected long threshold = -1;

    protected LogEventSender sender;

    public AbstractLoggingInterceptor(String phase, LogEventSender sender) {
        super(phase);
        this.sender = sender;
    }
    
    public void setLimit(int lim) {
        this.limit = lim;
    }

    public int getLimit() {
        return this.limit;
    }

    public void setInMemThreshold(long t) {
        this.threshold = t;
    }

    public long getInMemThreshold() {
        return threshold;
    }

    public void createExchangeId(Message message) {
        Exchange exchange = message.getExchange();
        String exchangeId = (String)exchange.get(LogEvent.KEY_EXCHANGE_ID);
        if (exchangeId == null) {
            exchangeId = UUID.randomUUID().toString();
            exchange.put(LogEvent.KEY_EXCHANGE_ID, exchangeId);
        }
    }

}
