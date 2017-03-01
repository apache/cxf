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

import java.util.List;
import java.util.Map;

import org.apache.cxf.message.Message;

public interface RedeliveryQueue {


    String DEFAULT_BASE_REDELIVERY_INTERVAL = "3000";
    int DEFAULT_EXPONENTIAL_BACKOFF = 2;

    /**
     * @param seq the sequence under consideration
     * @return the number of unacknowledged messages for that sequence
     */
    int countUndelivered(DestinationSequence seq);

    /**
     * @return the total number of undelivered messages in this queue
     */
    int countUndelivered();

    /**
     * @return true if there are no unacknowledged messages in the queue
     */
    boolean isEmpty();

    /**
     * Accepts a failed message for possible future redelivery.
     * @param message the message context.
     */
    void addUndelivered(Message message);

    /**
     * Purge all candiates for the given sequence.
     *
     * @param seq the sequence object
     */
    void purgeAll(DestinationSequence seq);

    /**
     *
     * @param seq
     * @return
     */
    List<Long> getUndeliveredMessageNumbers(DestinationSequence seq);

    /**
     * Returns the retransmission status for the specified message.
     * @param seq
     * @param num
     * @return
     */
    RetryStatus getRedeliveryStatus(DestinationSequence seq, long num);

    /**
     * Return the retransmission status of all the messages assigned to the sequence.
     * @param seq
     * @return
     */
    Map<Long, RetryStatus> getRedeliveryStatuses(DestinationSequence seq);

    /**
     * Initiate resends.
     */
    void start();

    /**
     * Stops redelivery queue.
     * @param seq
     */
    void stop(DestinationSequence seq);

    /**
     * Suspends the redelivery attempts for the specified sequence
     * @param seq
     */
    void suspend(DestinationSequence seq);

    /**
     * Resumes the redelivery attempts for the specified sequence
     * @param seq
     */
    void resume(DestinationSequence seq);
}