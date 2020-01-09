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

package org.apache.cxf.metrics;


import org.apache.cxf.message.Exchange;


/**
 * Class to hold all the various metric pieces for a given context (Endpoint, Customer, Operation, etc...)
 */
public interface MetricsContext {

    /**
     * Will be called at the start of invoke (or when added to a started MessageMetrics).  This is
     * when the metrics should increment "inFlight" counts and other stats.   There is no need to
     * record a "start time" as the invoke time will be passed into the stop method.
     */
    void start(Exchange m);

    /**
     * Called when the invocation is complete.
     *
     * @param timeInNS
     * @param inSize
     * @param outSize
     * @param exchange
     */
    void stop(long timeInNS, long inSize, long outSize, Exchange exchange);
}
