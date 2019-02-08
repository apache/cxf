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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.ws.rm.v200702.Identifier;

public class AbstractEndpoint {

    /* the number of currently processing sequences */
    protected AtomicInteger processingSequenceCount;
    /* the number of completed sequences since last started */
    protected AtomicInteger completedSequenceCount;

    private final RMEndpoint reliableEndpoint;

    protected AbstractEndpoint(RMEndpoint rme) {
        reliableEndpoint = rme;
        processingSequenceCount = new AtomicInteger();
        completedSequenceCount = new AtomicInteger();
    }

    public String getName() {
        return RMUtils.getEndpointIdentifier(getEndpoint(), getBus());
    }

    /**
     * @return Returns the reliableEndpoint.
     */
    public RMEndpoint getReliableEndpoint() {
        return reliableEndpoint;
    }

    /**
     * @return Returns the endpoint.
     */
    public Endpoint getEndpoint() {
        return reliableEndpoint.getApplicationEndpoint();
    }

    /**
     * @return Returns the manager.
     */
    public RMManager getManager() {
        return reliableEndpoint.getManager();
    }

    /**
     * Generates and returns a new sequence identifier.
     *
     * @return the sequence identifier.
     */
    public Identifier generateSequenceIdentifier() {
        return reliableEndpoint.getManager().getIdGenerator().generateSequenceIdentifier();
    }

    int getProcessingSequenceCount() {
        return processingSequenceCount.get();
    }

    int getCompletedSequenceCount() {
        return completedSequenceCount.get();
    }

    private Bus getBus() {
        return reliableEndpoint.getManager().getBus();
    }
}
