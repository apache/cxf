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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;

/**
 * Outputstream that sends a message when the exchange is closed
 */
class JMSOutputStream extends CachedOutputStream {
    static final Logger LOG = LogUtils.getL7dLogger(JMSOutputStream.class);
    
    private final JMSExchangeSender sender;
    private Exchange exchange;
    private boolean isTextPayload;

    public JMSOutputStream(JMSExchangeSender sender, Exchange exchange, boolean isTextPayload) {
        this.sender = sender;
        this.exchange = exchange;
        this.isTextPayload = isTextPayload;
    }

    @Override
    protected void doFlush() throws IOException {
        // do nothing here
    }

    @Override
    /**
     * Close the stream and send the message out
     */
    protected void doClose() throws IOException {
        Object payload = retrieveRequestFromStream(isTextPayload);
        this.sender.sendExchange(exchange, payload);
    }

    /**
     * Extract the request from the OutputStream
     *
     * @return for textPayloads a String is returned else a byte[]
     * @throws IOException
     */
    private Object retrieveRequestFromStream(boolean isTextPayload1) throws IOException {
        Object request = null;
        try {
            if (isTextPayload1) {
                StringBuilder builder = new StringBuilder(2048);
                this.writeCacheTo(builder);
                request = builder.toString();
            } else {
                request = getBytes();
            }
        } catch (IOException ex) {
            throw new IOException("Error creating request Object from Message content, exception " + ex);
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Payload to be sent out is :[" + request + "]");
        }
        return request;
    }

}