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
import java.io.InputStream;
import java.io.Reader;
import java.io.SequenceInputStream;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedWriter;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class WireTapIn extends AbstractPhaseInterceptor<Message> {
    private static final String WIRE_TAP_STARTED = WireTapIn.class.getName() + ".Started";

    private long threshold;
    private int limit;

    /**
     * Instantiates a new WireTapIn
     * @param limit
     * @param threshold the log threshold
     */
    public WireTapIn(int limit, long threshold) {
        super(Phase.RECEIVE);
        this.limit = limit;
        this.threshold = threshold;
    }

    @Override
    public void handleMessage(final Message message) throws Fault {
        if (message.containsKey(WIRE_TAP_STARTED)) {
            return;
        }
        message.put(WIRE_TAP_STARTED, Boolean.TRUE);
        try {
            InputStream is = message.getContent(InputStream.class);
            if (is != null) {
                handleInputStream(message, is);
            } else {
                Reader reader = message.getContent(Reader.class);
                if (reader != null) {
                    handleReader(message, reader);
                }
            }
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    private void handleReader(Message message, Reader reader) throws IOException {
        CachedWriter writer = new CachedWriter();
        IOUtils.copyAndCloseInput(reader, writer);
        message.setContent(Reader.class, writer.getReader());
        message.setContent(CachedWriter.class, writer);
    }

    private void handleInputStream(Message message, InputStream is) throws IOException {
        CachedOutputStream bos = new CachedOutputStream();
        if (threshold > 0) {
            bos.setThreshold(threshold);
        }
        // use the appropriate input stream and restore it later
        InputStream bis = is instanceof DelegatingInputStream
            ? ((DelegatingInputStream)is).getInputStream() : is;

        // only copy up to the limit since that's all we need to log
        // we can stream the rest
        IOUtils.copyAtLeast(bis, bos, limit == -1 ? Integer.MAX_VALUE : limit);
        bos.flush();
        bis = new SequenceInputStream(bos.getInputStream(), bis);

        // restore the delegating input stream or the input stream
        if (is instanceof DelegatingInputStream) {
            ((DelegatingInputStream)is).setInputStream(bis);
        } else {
            message.setContent(InputStream.class, bis);
        }
        message.setContent(CachedOutputStream.class, bos);

    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

}
