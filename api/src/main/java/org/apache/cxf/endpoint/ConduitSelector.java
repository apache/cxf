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

package org.apache.cxf.endpoint;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Conduit;


/**
 * Strategy for retreiving a Conduit to mediate an outbound message.
 * A specific instance implementing a particular strategy may be injected
 * into the Client via config.
 */
public interface ConduitSelector {
        
    /**
     * Called prior to the interceptor chain being traversed.
     * This is the point at which an eager strategy would retrieve
     * a Conduit.
     * 
     * @param message the current Message
     */
    void prepare(Message message);

    /**
     * Called when a Conduit is actually required.
     * This is the point at which a lazy strategy would retrieve
     * a Conduit.
     * 
     * @param message
     * @return the Conduit to use for mediation of the message
     */
    Conduit selectConduit(Message message);
    
    /**
     * Called on completion of the MEP for which the Conduit was required.
     * This is the point at which a one-shot strategy would dispose of
     * the Conduit.
     * 
     * @param exchange represents the completed MEP
     */
    void complete(Exchange exchange);

    /**
     * @return the encapsulated Endpoint
     */
    Endpoint getEndpoint();

    /**
     * @param endpoint the Endpoint to encapsulate
     */
    void setEndpoint(Endpoint endpoint);
}
