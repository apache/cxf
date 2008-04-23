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

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * A Destination is a transport-level endpoint capable of receiving
 * unsolicited incoming messages from different peers.
 */
public interface Destination extends Observable {
    
    /**
     * @return the reference associated with this Destination
     */    
    EndpointReferenceType getAddress();
    
    /**
     * Retreive a back-channel Conduit, which must be policy-compatible
     * with the current Message and associated Destination. For example
     * compatible Quality of Protection must be asserted on the back-channel.
     * This would generally only be an issue if the back-channel is decoupled.
     * 
     * @param inMessage the current message (null to indicate a disassociated
     * back-channel.
     * @param partialResponse in the decoupled case, this is expected to be the
     * outbound Message to be sent over the in-built back-channel. 
     * @param address the backchannel address (null to indicate anonymous)
     * @return a suitable Conduit
     */
    Conduit getBackChannel(Message inMessage,
                           Message partialResponse,
                           EndpointReferenceType address)
        throws IOException;

    /**
     * Shutdown the Destination, i.e. stop accepting incoming messages.
     */
    void shutdown();
    
    MessageObserver getMessageObserver();
}
