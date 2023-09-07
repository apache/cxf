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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.event.PrintWriterEventSender;
import org.apache.cxf.ext.logging.slf4j.Slf4jVerboseEventSender;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

/**
 *
 */
@NoJSR250Annotations
public class LoggingOutInterceptor extends AbstractLoggingInterceptor {

    public LoggingOutInterceptor() {
        this(new Slf4jVerboseEventSender());
    }

    public LoggingOutInterceptor(PrintWriter writer) {
        this(new PrintWriterEventSender(writer));
    }

    public LoggingOutInterceptor(LogEventSender sender) {
        super(Phase.PRE_STREAM, sender);
        addBefore(StaxOutInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
        if (isLoggingDisabledNow(message)) {
            return;
        } else {
            //ensure only logging once for a certain message
            //this can prevent message logging again when fault
            //happen after PRE_STREAM phase(LoggingOutInterceptor is called both in out chain and fault out chain)
            message.put(LIVE_LOGGING_PROP, Boolean.FALSE);
        }
        createExchangeId(message);
        final OutputStream os = message.getContent(OutputStream.class);
        if (os != null) {
            LoggingCallback callback = new LoggingCallback(sender, message, os, limit);
            message.setContent(OutputStream.class, createCachingOut(message, os, callback));
        } else {
            final Writer iowriter = message.getContent(Writer.class);
            if (iowriter != null) {
                message.setContent(Writer.class, new LogEventSendingWriter(sender, message, iowriter, limit));
            }
        }
    }

    private OutputStream createCachingOut(Message message, final OutputStream os, CachedOutputStreamCallback callback) {
        final CacheAndWriteOutputStream newOut = new LoggingOutputStream(os);
        if (threshold > 0) {
            newOut.setThreshold(threshold);
        }
        if (limit > 0) {
            // make the limit for the cache greater than the limit for the truncated payload in the log event,
            // this is necessary for finding out that the payload was truncated
            //(see boolean isTruncated = cos.size() > limit && limit != -1;)  in method copyPayload
            newOut.setCacheLimit(getCacheLimit());
        }
        newOut.registerCallback(callback);
        return newOut;
    }

    private int getCacheLimit() {
        if (limit == Integer.MAX_VALUE) {
            return limit;
        }
        return limit + 1;
    }

    private class LogEventSendingWriter extends FilterWriter {
        StringWriter out2;
        int count;
        Message message;
        final int lim;
        private LogEventSender sender;

        LogEventSendingWriter(LogEventSender sender, Message message, Writer writer, int limit) {
            super(writer);
            this.sender = sender;
            this.message = message;
            if (!(writer instanceof StringWriter)) {
                out2 = new StringWriter();
            }
            lim = limit == -1 ? Integer.MAX_VALUE : limit;
        }

        public void write(int c) throws IOException {
            super.write(c);
            if (out2 != null && count < lim) {
                out2.write(c);
            }
            count++;
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            super.write(cbuf, off, len);
            if (out2 != null && count < lim) {
                out2.write(cbuf, off, len);
            }
            count += len;
        }

        public void write(String str, int off, int len) throws IOException {
            super.write(str, off, len);
            if (out2 != null && count < lim) {
                out2.write(str, off, len);
            }
            count += len;
        }

        public void close() throws IOException {
            final LogEvent event = eventMapper.map(message, sensitiveProtocolHeaderNames);
            StringWriter w2 = out2;
            if (w2 == null) {
                w2 = (StringWriter) out;
            }

            String payload = shouldLogContent(event) ? getPayload(event, w2) : CONTENT_SUPPRESSED;
            String maskedContent = maskSensitiveElements(message, payload);
            if (!logBinary) {
                maskedContent = stripBinaryParts(event, maskedContent);
            }
            event.setPayload(transform(message, maskedContent));
            sender.send(event);
            message.setContent(Writer.class, out);
            super.close();
        }

        private String getPayload(final LogEvent event, StringWriter w2) {
            StringBuilder payload = new StringBuilder();
            writePayload(payload, w2, event);
            return payload.toString();
        }

        private void writePayload(StringBuilder builder, StringWriter stringWriter, LogEvent event) {
            StringBuffer buffer = stringWriter.getBuffer();
            if (buffer.length() > lim) {
                builder.append(buffer.subSequence(0, lim));
                event.setTruncated(true);
            } else {
                builder.append(buffer);
                event.setTruncated(false);
            }
        }
    }

    public class LoggingCallback implements CachedOutputStreamCallback {

        private final Message message;
        private final OutputStream origStream;
        private final int lim;
        private LogEventSender sender;

        public LoggingCallback(final LogEventSender sender, final Message msg, final OutputStream os, int limit) {
            this.sender = sender;
            this.message = msg;
            this.origStream = os;
            this.lim = limit == -1 ? Integer.MAX_VALUE : limit;
        }

        public void onFlush(CachedOutputStream cos) {

        }

        public void onClose(CachedOutputStream cos) {
            final LogEvent event = eventMapper.map(message, sensitiveProtocolHeaderNames);
            if (shouldLogContent(event)) {
                copyPayload(cos, event);
            } else {
                event.setPayload(CONTENT_SUPPRESSED);
            }

            sender.send(event);
            try {
                // empty out the cache
                cos.lockOutputStream();
                cos.resetOut(null, false);
            } catch (Exception ex) {
                // ignore
            }
            message.setContent(OutputStream.class, origStream);
        }

        private void copyPayload(CachedOutputStream cos, final LogEvent event) {
            try {
                String encoding = (String) message.get(Message.ENCODING);
                StringBuilder payload = new StringBuilder();
                writePayload(payload, cos, encoding, event.getContentType());
                String maskedContent = maskSensitiveElements(message, payload.toString());
                if (!logBinary) {
                    maskedContent = stripBinaryParts(event, maskedContent);
                }
                event.setPayload(transform(message, maskedContent));
                boolean isTruncated = cos.size() > limit && limit != -1;
                event.setTruncated(isTruncated);
            } catch (Exception ex) {
                // ignore
            }
        }

        protected void writePayload(StringBuilder builder, CachedOutputStream cos, String encoding, String contentType)
                throws Exception {
            if (StringUtils.isEmpty(encoding)) {
                cos.writeCacheTo(builder, lim);
            } else {
                cos.writeCacheTo(builder, encoding, lim);
            }
        }
    }

}
