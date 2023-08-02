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

package org.apache.cxf.systest.ws.rm;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.io.AbstractWrappedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMMessageConstants;

/**
 *
 */
public class MessageLossSimulator extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getLogger(MessageLossSimulator.class);
    private int appMessageCount;
    private boolean throwsException;
    private int mode;

    public MessageLossSimulator() {
        super(Phase.PREPARE_SEND);
        addBefore(MessageSenderInterceptor.class.getName());
    }

    public boolean isThrowsException() {
        return throwsException;
    }

    public void setThrowsException(boolean throwsException) {
        this.throwsException = throwsException;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void handleMessage(Message message) throws Fault {
        AddressingProperties maps = RMContextUtils.retrieveMAPs(message, false, true);
        String action = null;
        if (maps != null && null != maps.getAction()) {
            action = maps.getAction().getValue();
        }
        if (RMContextUtils.isRMProtocolMessage(action)) {
            return;
        }
        if (MessageUtils.isPartialResponse(message)) {
            return;
        }
        if (Boolean.TRUE.equals(message.get(RMMessageConstants.RM_RETRANSMISSION))) {
            return;
        }

        if (mode == 1) {
            // never lose
            return;
        } else if (mode == -1) {
            // always lose
        } else {
            // alternatively lose
            synchronized (this) {
                appMessageCount++;
                if (0 != (appMessageCount % 2)) {
                    return;
                }
            }
        }

        InterceptorChain chain = message.getInterceptorChain();
        ListIterator<Interceptor<? extends Message>> it = chain.getIterator();
        while (it.hasNext()) {
            PhaseInterceptor<?> pi = (PhaseInterceptor<? extends Message>)it.next();
            if (MessageSenderInterceptor.class.getName().equals(pi.getId())) {
                chain.remove(pi);
                LOG.fine("Removed MessageSenderInterceptor from interceptor chain.");
                break;
            }
        }

        message.setContent(OutputStream.class, new WrappedOutputStream(message));

        message.getInterceptorChain().add(new MessageLossEndingInterceptor(throwsException));
    }

    /**
     * Ending interceptor to discard message output. Note that the name is used as a String by RMCaptureOutInterceptor,
     * so if ever changed here also needs to be changed there.
     */
    public static final class MessageLossEndingInterceptor extends AbstractPhaseInterceptor<Message> {

        private final boolean throwsException;

        public MessageLossEndingInterceptor(boolean except) {
            super(Phase.PREPARE_SEND_ENDING);
            throwsException = except;
        }

        public void handleMessage(Message message) throws Fault {
            try {
                message.getContent(OutputStream.class).close();
                if (throwsException) {
                    throw new IOException("simulated transmission exception");
                }
            } catch (IOException e) {
                throw new Fault(e);
            }
        }
    }

    private class WrappedOutputStream extends AbstractWrappedOutputStream {

        private Message outMessage;

        WrappedOutputStream(Message m) {
            this.outMessage = m;
        }

        @Override
        protected void onFirstWrite() throws IOException {
            if (LOG.isLoggable(Level.FINE)) {
                Long nr = RMContextUtils.retrieveRMProperties(outMessage, true)
                    .getSequence().getMessageNumber();
                LOG.fine("Losing message " + nr);
            }
            wrappedStream = new DummyOutputStream();
        }
    }

    private final class DummyOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {

        }

    }

}
