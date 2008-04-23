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

import org.apache.cxf.message.Message;

public interface RetransmissionQueue {
    
    
    String DEFAULT_BASE_RETRANSMISSION_INTERVAL = "3000";
    int DEFAULT_EXPONENTIAL_BACKOFF = 2;
    
    /**
     * @param seq the sequence under consideration
     * @return the number of unacknowledged messages for that sequence
     */
    int countUnacknowledged(SourceSequence seq);
    
    /**
     * @return true if there are no unacknowledged messages in the queue
     */
    boolean isEmpty();
    
    /**
     * Accepts a new context for posible future retransmission. 
     * @param ctx the message context.
     */
    void addUnacknowledged(Message message);
    
    /**
     * Purge all candidates for the given sequence that have been acknowledged.
     * 
     * @param seq the sequence object.
     */
    void purgeAcknowledged(SourceSequence seq);
    
    /**
     * Initiate resends.
     *
     */
    void start();
    
    /**
     * Stops retransmission queue.
     */
    void stop(SourceSequence seq);
    
    
}
