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
package org.apache.cxf.transport.jms;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;

final class MessageStreamUtil {

    private MessageStreamUtil() {
    }

    /**
     * Set Writer or OutputStream in message that calls the sender on close with
     * the content of the stream
     *
     * @param message where to set the content
     * @param isTextPayload decides about stream type true:Writer, false: OutputStream
     * @param sender will be called on close
     */
    public static void prepareStream(final Message message, boolean isTextPayload,
                                     final JMSExchangeSender sender) {
        if (isTextPayload) {
            message.setContent(Writer.class, new SendingWriter(sender, message.getExchange()));
        } else {
            SendingOutputStream out = new SendingOutputStream(sender, message.getExchange());
            message.setContent(OutputStream.class, out);
        }
    }

    private static final class SendingWriter extends StringWriter {
        private final JMSExchangeSender sender;
        private Exchange exchange;

        private SendingWriter(JMSExchangeSender sender, Exchange exchange) {
            this.sender = sender;
            this.exchange = exchange;
        }

        @Override
        public void close() throws IOException {
            super.close();
            sender.sendExchange(exchange, toString());
        }
    }

    private static final class SendingOutputStream extends CachedOutputStream {
        private final JMSExchangeSender sender;
        private Exchange exchange;

        SendingOutputStream(JMSExchangeSender sender, Exchange exchange) {
            this.sender = sender;
            this.exchange = exchange;
        }

        @Override
        protected void doClose() throws IOException {
            this.sender.sendExchange(exchange, getBytes());
        }

    }

    public static void closeStreams(Message msg) throws IOException {
        Writer writer = msg.getContent(Writer.class);
        if (writer != null) {
            writer.close();
        }
        Reader reader = msg.getContent(Reader.class);
        if (reader != null) {
            reader.close();
        }
    }
}
