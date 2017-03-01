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

import java.util.Date;

/**
 * A generic interface to represent the retrying status of a repeating activity
 * at some WS-RM component.
 */
public interface RetryStatus {
    /**
     * @return the next retry time
     */
    Date getNext();

    /**
     * @return the previous retry time
     */
    Date getPrevious();

    /**
     * @return the number of retries
     */
    int getRetries();

    /**
     * @return the max number of retries permitted
     */
    int getMaxRetries();

    /**
     * @return the nextInterval
     */
    long getNextInterval();

    /**
     * @return the backoff
     */
    long getBackoff();

    /**
     * @return the pending
     */
    boolean isPending();

    /**
     * @return the suspended
     */
    boolean isSuspended();
}
