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

package org.apache.cxf.transport;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

/**
 * Abstract base class factoring out common Destination logic,
 * allowing non-decoupled transports to be written without any
 * regard for the decoupled back-channel or partial response logic.
 */
public abstract class AbstractDestination
    extends AbstractObservable implements Destination, DestinationWithEndpoint {

    protected final EndpointReferenceType reference;
    protected final EndpointInfo endpointInfo;
    protected final Bus bus;

    public AbstractDestination(EndpointReferenceType ref,
                               EndpointInfo ei) {
        this(null, ref, ei);
    }

    public AbstractDestination(Bus b,
                               EndpointReferenceType ref,
                               EndpointInfo ei) {
        reference = ref;
        endpointInfo = ei;
        bus = b;
    }

    /**
     * @return the reference associated with this Destination
     */
    public EndpointReferenceType getAddress() {
        return reference;
    }

    /**
     * Retrieve a back-channel Conduit, which must be policy-compatible
     * with the current Message and associated Destination. For example
     * compatible Quality of Protection must be asserted on the back-channel.
     * This would generally only be an issue if the back-channel is decoupled.
     *
     * @param inMessage the current inbound message (null to indicate a
     * disassociated back-channel)
     * @return a suitable Conduit
     */
    public Conduit getBackChannel(Message inMessage)
        throws IOException {

        return getInbuiltBackChannel(inMessage);
    }

    /**
     * Shutdown the Destination, i.e. stop accepting incoming messages.
     */
    public void shutdown() {
        // nothing to do by default
    }

    /**
     * @param inMessage the incoming message
     * @return the inbuilt backchannel
     */
    protected abstract Conduit getInbuiltBackChannel(Message inMessage);

    /**
     * Backchannel conduit.
     */
    protected abstract class AbstractBackChannelConduit extends AbstractConduit {

        public AbstractBackChannelConduit() {
            super(getAnonymousEndpointReference());
        }

        /**
         * Register a message observer for incoming messages.
         *
         * @param observer the observer to notify on receipt of incoming
         */
        public void setMessageObserver(MessageObserver observer) {
            // shouldn't be called for a back channel conduit
        }

        protected Logger getLogger() {
            return AbstractDestination.this.getLogger();
        }
    }

    // EndpointReferenceUtils#getAnonymousEndpointReference would load jaxb, avoid it
    private static EndpointReferenceType getAnonymousEndpointReference() {
        final EndpointReferenceType reference = new EndpointReferenceType();
        final AttributedURIType a = new AttributedURIType();
        a.setValue(EndpointReferenceUtils.ANONYMOUS_ADDRESS);
        reference.setAddress(a);
        return reference;
    }

    /**
     * {@inheritDoc}
     */
    public EndpointInfo getEndpointInfo() {
        return endpointInfo;
    }
}
