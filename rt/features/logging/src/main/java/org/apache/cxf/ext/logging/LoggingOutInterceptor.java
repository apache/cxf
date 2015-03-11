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
import java.io.StringWriter;
import java.io.Writer;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ext.logging.event.DefaultLogEventMapper;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
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

    public LoggingOutInterceptor(LogEventSender sender) {
        super(Phase.PRE_STREAM, sender);
        addBefore(StaxOutInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
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
        final CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
        if (threshold > 0) {
            newOut.setThreshold(threshold);
        }
        if (limit > 0) {
            newOut.setCacheLimit(limit);
        }
        newOut.registerCallback(callback);
        return newOut;
    }

    private static class LogEventSendingWriter extends FilterWriter {
        StringWriter out2;
        int count;
        Message message;
        final int lim;
        private LogEventSender sender;

        public LogEventSendingWriter(LogEventSender sender, Message message, Writer writer, int limit) {
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
            final LogEvent event = new DefaultLogEventMapper().map(message);
            StringWriter w2 = out2;
            if (w2 == null) {
                w2 = (StringWriter)out;
            }
            String ct = (String)message.get(Message.CONTENT_TYPE);
            StringBuilder payload = new StringBuilder();
            try {
                writePayload(payload, w2, ct);
            } catch (Exception ex) {
                // ignore
            }
            event.setPayload(payload.toString());
            sender.send(event);
            message.setContent(Writer.class, out);
            super.close();
        }
        
        protected void writePayload(StringBuilder builder, StringWriter stringWriter, String contentType)
            throws Exception {
            StringBuffer buffer = stringWriter.getBuffer();
            if (buffer.length() > lim) {
                builder.append(buffer.subSequence(0, lim));
            } else {
                builder.append(buffer);
            }
        }
    }

    public static class LoggingCallback implements CachedOutputStreamCallback {

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
            final LogEvent event = new DefaultLogEventMapper().map(message);
            try {
                String encoding = (String)message.get(Message.ENCODING);
                StringBuilder payload = new StringBuilder();
                writePayload(payload, cos, encoding, event.getContentType());
                event.setPayload(payload.toString());
            } catch (Exception ex) {
                // ignore
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
        
        protected void writePayload(StringBuilder builder, CachedOutputStream cos, String encoding,
                                    String contentType) throws Exception {
            if (StringUtils.isEmpty(encoding)) {
                cos.writeCacheTo(builder, lim);
            } else {
                cos.writeCacheTo(builder, encoding, lim);
            }
        }
    }

}
