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

package org.apache.cxf.throttling;

import java.util.List;

import org.apache.cxf.message.Message;

/**
 *
 */
public interface ThrottlingManager {

    /**
     * Get the list of phases where this manager will expect to have to make throttling decisions.
     * For example: using BasicAuth or other protocol based header, it can be a very early in the
     * chain, but for WS-Security based authentication, it would be later.
     * @return
     */
    List<String> getDecisionPhases();

    /**
     * Use information in the message to determine what throttling measures should be taken
     * @param phase
     * @param m
     * @return
     */
    ThrottleResponse getThrottleResponse(String phase, Message m);
}
