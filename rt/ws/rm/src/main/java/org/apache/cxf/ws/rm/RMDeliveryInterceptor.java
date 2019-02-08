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

package org.apache.cxf.ws.rm;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;

/**
 * Interceptor used for InOrder delivery of messages to the destination. This works with
 * {@link DestinationSequence} to allow only one message at a time from a particular sequence through to the
 * destination (since otherwise there is no way to enforce in-order delivery).
 */
public class RMDeliveryInterceptor extends AbstractRMInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(RMDeliveryInterceptor.class);

    public RMDeliveryInterceptor() {
        super(Phase.POST_INVOKE);
        addBefore(OutgoingChainInterceptor.class.getName());
    }

    // Interceptor interface

    public void handle(Message message) throws SequenceFault, RMException {
        final AddressingProperties maps = ContextUtils.retrieveMAPs(message, false, false, false);
        //if wsrmp:RMAssertion and addressing is optional
        if (maps == null && isRMPolicyEnabled(message)) {
            return;
        }
        LOG.entering(getClass().getName(), "handleMessage");
        Destination dest = getManager().getDestination(message);
        final boolean robust =
            MessageUtils.getContextualBoolean(message, Message.ROBUST_ONEWAY, false);
        if (robust) {
            message.remove(RMMessageConstants.DELIVERING_ROBUST_ONEWAY);
            dest.acknowledge(message);
        }
        dest.processingComplete(message);

        // close InputStream of RMCaptureInInterceptor, to delete tmp files in filesystem
        Closeable closable = (Closeable)message.get("org.apache.cxf.ws.rm.content.closeable");
        if (null != closable) {
            try {
                closable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
