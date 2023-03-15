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

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class InMessageRecorder extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getLogger(InMessageRecorder.class);
    private final List<byte[]> inbound = new CopyOnWriteArrayList<>();

    public InMessageRecorder() {
        super(Phase.RECEIVE);
    }

    public void handleMessage(Message message) throws Fault {
        try (InputStream is = message.getContent(InputStream.class)) {
            if (is != null) {
                int i = is.available();
                if (i < 4096) {
                    i = 4096;
                }
                LoadingByteArrayOutputStream bout = new LoadingByteArrayOutputStream(i);
                IOUtils.copy(is, bout);
                is.close();
                
                inbound.add(bout.toByteArray());
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("inbound: " + new String(bout.toByteArray()));
                }
                message.setContent(InputStream.class, bout.createInputStream());
            }
        } catch (Exception ex) {
            throw new Fault(ex);
        }
    }

    public List<byte[]> getInboundMessages() {
        return inbound;
    }
}
