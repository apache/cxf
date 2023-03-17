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

package org.apache.cxf.systest.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

class ClientHttpConnectionOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private Collection<Message> messages = new ArrayList<>();

    ClientHttpConnectionOutInterceptor() {
        super(Phase.SEND_ENDING);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        synchronized (messages) {
            messages.add(message);
        }
    }

    public boolean checkAllClosed() {
        synchronized (messages) {
            if (messages.isEmpty()) {
                return false;
            }
            
            return messages
                .stream()
                .anyMatch(this::isClosedInputStream);
        }
    }
    
    private boolean isClosedInputStream(Message message) {
        try {
            final InputStream inputStream = message.getExchange().getInMessage().getContent(InputStream.class);
            if (inputStream == null) {
                return true;
            }
            inputStream.read(new byte [0]); /* 0 bytes to read */
            return false;
        } catch (IOException ex) {
            // The HttpInputStream throws an IOException in case the input stream is already
            // closed (since we actually read nothing).
            String msg = ex.getMessage();
            return msg.contains("closed");
        }
    }
}