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

package org.apache.cxf.testutil.recorders;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.io.WriteOnCloseOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;


public class OutMessageRecorder extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getLogger(OutMessageRecorder.class);
    private final List<byte[]> outbound = new CopyOnWriteArrayList<>();

    public OutMessageRecorder() {
        super(Phase.PREPARE_SEND);
        addAfter(MessageSenderInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {

        OutputStream os = message.getContent(OutputStream.class);
        if (null == os) {
            return;
        }

        WriteOnCloseOutputStream stream = createCachedStream(message, os);
        stream.registerCallback(new RecorderCallback());
    }


    public static WriteOnCloseOutputStream createCachedStream(Message message, OutputStream os) {
        // We need to ensure that we have an output stream which won't start writing the
        // message until we have a chance to send a createsequence
        if (!(os instanceof WriteOnCloseOutputStream)) {
            WriteOnCloseOutputStream cached = new WriteOnCloseOutputStream(os);
            message.setContent(OutputStream.class, cached);
            os = cached;
        }
        return (WriteOnCloseOutputStream) os;
    }
    public List<byte[]> getOutboundMessages() {
        return outbound;
    }

    class RecorderCallback implements CachedOutputStreamCallback {

        public void onFlush(CachedOutputStream cos) {

        }

        public void onClose(CachedOutputStream cos) {
            // bytes were already copied after flush
            try {
                outbound.add(cos.getBytes());
            } catch (Exception e) {
                LOG.fine("Can't record message from output stream class: "
                         + cos.getOut().getClass().getName());
            }
        }

    }

}
