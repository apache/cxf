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

import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.ext.logging.event.DefaultLogEventMapper;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.event.PrettyLoggingFilter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

public abstract class AbstractLoggingInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final int DEFAULT_LIMIT = 48 * 1024;
    public static final int DEFAULT_THRESHOLD = -1;
    public static final String CONTENT_SUPPRESSED = "--- Content suppressed ---";
    private static final String  LIVE_LOGGING_PROP = "org.apache.cxf.logging.enable"; 
    protected int limit = DEFAULT_LIMIT;
    protected long threshold = DEFAULT_THRESHOLD;
    protected boolean logBinary;
    protected boolean logMultipart = true;

    protected LogEventSender sender;
    protected final DefaultLogEventMapper eventMapper = new DefaultLogEventMapper();

    public AbstractLoggingInterceptor(String phase, LogEventSender sender) {
        super(phase);
        this.sender = sender;
    }

    protected static boolean isLoggingDisabledNow(Message message) throws Fault {
        Object liveLoggingProp = message.getContextualProperty(LIVE_LOGGING_PROP);
        return liveLoggingProp != null && PropertyUtils.isFalse(liveLoggingProp);
    }

    public void addBinaryContentMediaTypes(String mediaTypes) {
        eventMapper.addBinaryContentMediaTypes(mediaTypes);
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

    public void setPrettyLogging(boolean prettyLogging) {
        if (sender instanceof PrettyLoggingFilter) {
            ((PrettyLoggingFilter)this.sender).setPrettyLogging(prettyLogging);
        }
    }

    protected boolean shouldLogContent(LogEvent event) {
        return event.isBinaryContent() && logBinary
            || event.isMultipartContent() && logMultipart
            || !event.isBinaryContent() && !event.isMultipartContent();
    }

    public void setLogBinary(boolean logBinary) {
        this.logBinary = logBinary;
    }

    public void setLogMultipart(boolean logMultipart) {
        this.logMultipart = logMultipart;
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
