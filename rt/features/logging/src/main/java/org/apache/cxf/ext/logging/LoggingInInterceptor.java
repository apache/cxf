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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.event.PrintWriterEventSender;
import org.apache.cxf.ext.logging.slf4j.Slf4jVerboseEventSender;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedWriter;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;

/**
 * 
 */
@NoJSR250Annotations
public class LoggingInInterceptor extends AbstractLoggingInterceptor {
    
      
    class LoggingInFaultInterceptor extends AbstractPhaseInterceptor<Message> {
        LoggingInFaultInterceptor() {
            super(Phase.RECEIVE);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
        }

        @Override
        public void handleFault(Message message) throws Fault {
            LoggingInInterceptor.this.handleMessage(message);
        }
    }


    public LoggingInInterceptor() {
        this(new Slf4jVerboseEventSender());
    }

    public LoggingInInterceptor(PrintWriter writer) {
        this(new PrintWriterEventSender(writer));
    }

    public LoggingInInterceptor(LogEventSender sender) {
        super(Phase.PRE_INVOKE, sender);
    }

    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        Collection<PhaseInterceptor<? extends Message>> ret = new ArrayList<>();
        ret.add(new WireTapIn(getWireTapLimit(), threshold));
        ret.add(new LoggingInFaultInterceptor());
        return ret;
    }

    public void handleMessage(Message message) throws Fault {
        if (isLoggingDisabledNow(message)) {
            return;
        } else {
            //ensure only logging once for a certain message
            //this can prevent message logging again when fault
            //happen after PRE_INVOKE phase(rewind calls into LoggingInFaultInterceptor)
            message.put(LIVE_LOGGING_PROP, Boolean.FALSE);
        }
        createExchangeId(message);
        final LogEvent event = eventMapper.map(message, sensitiveProtocolHeaderNames);
        if (shouldLogContent(event)) {
            addContent(message, event);
        } else {
            event.setPayload(AbstractLoggingInterceptor.CONTENT_SUPPRESSED);
        }
        String maskedContent = maskSensitiveElements(message, event.getPayload());
        if (!logBinary) {
            maskedContent = stripBinaryParts(event, maskedContent);
        }
        event.setPayload(transform(message, maskedContent));
        sender.send(event);
    }

    private void addContent(Message message, final LogEvent event) {
        try {
            CachedOutputStream cos = message.getContent(CachedOutputStream.class);
            if (cos != null) {
                handleOutputStream(event, message, cos);
            } else {
                CachedWriter writer = message.getContent(CachedWriter.class);
                if (writer != null) {
                    handleWriter(event, writer);
                }
            }
        } catch (IOException e) {
            throw new Fault(e);
        }
    }

    private void handleOutputStream(final LogEvent event, Message message, CachedOutputStream cos) throws IOException {
        String encoding = (String) message.get(Message.ENCODING);
        if (StringUtils.isEmpty(encoding)) {
            encoding = StandardCharsets.UTF_8.name();
        }
        StringBuilder payload = new StringBuilder();
        cos.writeCacheTo(payload, encoding, limit);
        cos.close();
        event.setPayload(payload.toString());
        boolean isTruncated = cos.size() > limit && limit != -1;
        event.setTruncated(isTruncated);
        event.setFullContentFile(cos.getTempFile());
    }

    private void handleWriter(final LogEvent event, CachedWriter writer) throws IOException {
        boolean isTruncated = writer.size() > limit && limit != -1;
        StringBuilder payload = new StringBuilder();
        writer.writeCacheTo(payload, limit);
        writer.close();
        event.setPayload(payload.toString());
        event.setTruncated(isTruncated);
        event.setFullContentFile(writer.getTempFile());
    }

    int getWireTapLimit() {
        if (limit == -1) {
            return -1;
        } else if (limit == Integer.MAX_VALUE) {
            return limit;
        } else {
            // add limit +1 as limit for the wiretab in order to read one byte more, so that truncated
            // is correctly calculated in LogginInIntecepteor! 
            // See code line :  boolean isTruncated = cos.size() > limit && limit != -1; 
            // cos is here the outputstream read by the wiretab which will return for cos.size() the 
            // limit in the truncated case!
            return limit + 1;
        }
    }

}
