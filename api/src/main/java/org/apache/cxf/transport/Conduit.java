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
 * A pipe between peers that channels transport-level messages.
 * <p>
 * A Conduit channels messages to a <b>single</b> destination, though
 * this destination may fan-out to multiple receivers (for example a JMS topic).
 * <p>
 * A Conduit may have a back-channel, on which transport-level responses
 * are received. Alternatively the back-channel destination may be decoupled,
 * in which case the response it is received via a separate Conduit.
 * The crucial distinction is whether the Conduit can itself correlate
 * the response (which may be synchronous, or may be delivered via 
 * a dedicated destination). 
 * <p>
 * Conduits may be used for multiple messages, either serially or 
 * concurrently, with the implementation taking care of mapping onto
 * multiple transport resources (e.g. connections) if neccessary to
 * support concurrency.
 * <p>
 * Binding-level MEPs may be realized over one or more Conduits.
 */
public interface Conduit extends Observable {
    
    /**
     * Prepare the message for sending. This will typically involve setting
     * an OutputStream on the message, but it may do nothing at all.
     * 
     * @param message the message to be sent.
     */
    void prepare(Message message) throws IOException;
    
    /**
     * Close the connections associated with the message
     */
    void close(Message message) throws IOException;
    
    /**
     * @return the reference associated with the target Destination
     */    
    EndpointReferenceType getTarget();
    
    /**
     * Retreive the back-channel Destination.
     * 
     * @return the backchannel Destination (or null if the backchannel is
     * built-in)
     */
    Destination getBackChannel();
        
    /**
     * Close the conduit
     */
    void close();
}
