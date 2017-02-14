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

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.util.Collection;
import java.util.Collections;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ext.logging.event.DefaultLogEventMapper;
import org.apache.cxf.ext.logging.event.EventType;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.event.PrintWriterEventSender;
import org.apache.cxf.ext.logging.slf4j.Slf4jEventSender;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedWriter;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;

/**
 *
 */
@NoJSR250Annotations
public class LoggingInInterceptor extends AbstractLoggingInterceptor {
    class SendLogEventInterceptor extends AbstractPhaseInterceptor<Message> {
        SendLogEventInterceptor() {
            super(Phase.PRE_INVOKE);
        }
        @Override
        public void handleMessage(Message message) throws Fault {
            LogEvent event = message.get(LogEvent.class);
            if (event != null) {
                DefaultLogEventMapper mapper = new DefaultLogEventMapper();
                mapper.setEpInfo(message, event);
                event.setType(mapper.getEventType(message));
                message.remove(LogEvent.class);
                sender.send(event);
            }
        }
    }

    public LoggingInInterceptor() {
        this(new Slf4jEventSender());
    }
    public LoggingInInterceptor(PrintWriter writer) {
        this(new PrintWriterEventSender(writer));
    }
    public LoggingInInterceptor(LogEventSender sender) {
        super(Phase.RECEIVE, sender);
    }
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return Collections.singleton(new SendLogEventInterceptor());
    }


    public void handleFault(Message message) {
        LogEvent event = message.get(LogEvent.class);
        if (event != null) {
            DefaultLogEventMapper mapper = new DefaultLogEventMapper();
            mapper.setEpInfo(message, event);
            event.setType(EventType.FAULT_IN);
            message.remove(LogEvent.class);
            sender.send(event);
        }
    }
    public void handleMessage(Message message) throws Fault {
        LogEvent event = message.get(LogEvent.class);
        if (event == null) {
            createExchangeId(message);
            event = new DefaultLogEventMapper().map(message);
            if (shouldLogContent(event)) {
                addContent(message, event);
            } else {
                event.setPayload(AbstractLoggingInterceptor.CONTENT_SUPPRESSED);
            }
            // at this point, we have the payload.  However, we may not have the endpoint yet. Delay sending
            // the event till a little bit later
            message.put(LogEvent.class, event);
        }
    }

    private void addContent(Message message, final LogEvent event) {
        InputStream is = message.getContent(InputStream.class);
        if (is != null) {
            logInputStream(message, is, event);
        } else {
            Reader reader = message.getContent(Reader.class);
            if (reader != null) {
                logReader(message, reader, event);
            }
        }
    }

    protected void logInputStream(Message message, InputStream is, LogEvent event) {
        CachedOutputStream bos = new CachedOutputStream();
        if (threshold > 0) {
            bos.setThreshold(threshold);
        }
        String encoding = event.getEncoding();
        try {
            // use the appropriate input stream and restore it later
            InputStream bis = is instanceof DelegatingInputStream
                ? ((DelegatingInputStream)is).getInputStream() : is;


            //only copy up to the limit since that's all we need to log
            //we can stream the rest
            IOUtils.copyAtLeast(bis, bos, limit == -1 ? Integer.MAX_VALUE : limit);
            bos.flush();
            bis = new SequenceInputStream(bos.getInputStream(), bis);

            // restore the delegating input stream or the input stream
            if (is instanceof DelegatingInputStream) {
                ((DelegatingInputStream)is).setInputStream(bis);
            } else {
                message.setContent(InputStream.class, bis);
            }

            if (bos.getTempFile() != null) {
                //large thing on disk...
                event.setFullContentFile(bos.getTempFile());
            }
            if (bos.size() > limit && limit != -1) {
                event.setTruncated(true);
            }

            StringBuilder builder = new StringBuilder(limit);
            if (StringUtils.isEmpty(encoding)) {
                bos.writeCacheTo(builder, limit);
            } else {
                bos.writeCacheTo(builder, encoding, limit);
            }
            bos.close();
            event.setPayload(builder.toString());
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    protected void logReader(Message message, Reader reader, LogEvent event) {
        try {
            CachedWriter writer = new CachedWriter();
            IOUtils.copyAndCloseInput(reader, writer);
            message.setContent(Reader.class, writer.getReader());

            if (writer.getTempFile() != null) {
                //large thing on disk...
                event.setFullContentFile(writer.getTempFile());
            }
            if (writer.size() > limit && limit != -1) {
                event.setTruncated(true);
            }
            int max = writer.size() > limit ? (int)limit : (int)writer.size();
            StringBuilder b = new StringBuilder(max);
            writer.writeCacheTo(b);
            event.setPayload(b.toString());
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

}
