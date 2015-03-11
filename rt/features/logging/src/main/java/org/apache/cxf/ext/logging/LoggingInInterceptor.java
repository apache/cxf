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

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ext.logging.event.DefaultLogEventMapper;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedWriter;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

/**
 * 
 */
@NoJSR250Annotations
public class LoggingInInterceptor extends AbstractLoggingInterceptor {

    public LoggingInInterceptor(LogEventSender sender) {
        super(Phase.PRE_INVOKE, sender);
    }

    public void handleMessage(Message message) throws Fault {
        createExchangeId(message);
        final LogEvent event = new DefaultLogEventMapper().map(message);
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

        sender.send(event);
    }

    private void handleOutputStream(final LogEvent event, Message message, CachedOutputStream cos) throws IOException {
        String encoding = (String)message.get(Message.ENCODING);
        if (StringUtils.isEmpty(encoding)) {
            encoding = "UTF-8";
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
        event.setPayload(payload.toString());
        event.setTruncated(isTruncated);
        event.setFullContentFile(writer.getTempFile());
    }

}
