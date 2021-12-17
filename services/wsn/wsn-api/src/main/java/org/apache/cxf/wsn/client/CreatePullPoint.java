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
package org.apache.cxf.wsn.client;

import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import org.apache.cxf.wsn.util.WSNHelper;
import org.oasis_open.docs.wsn.b_2.CreatePullPointResponse;
import org.oasis_open.docs.wsn.bw_2.UnableToCreatePullPointFault;

public class CreatePullPoint implements Referencable {

    private final org.oasis_open.docs.wsn.bw_2.CreatePullPoint createPullPoint;
    private final W3CEndpointReference epr;

    public CreatePullPoint(String address) {
        this(WSNHelper.getInstance().createWSA(address));
    }

    public CreatePullPoint(W3CEndpointReference epr) {
        this.createPullPoint
            = WSNHelper.getInstance().getPort(epr,
                                              org.oasis_open.docs.wsn.bw_2.CreatePullPoint.class);
        this.epr = epr;
    }

    public org.oasis_open.docs.wsn.bw_2.CreatePullPoint getCreatePullPoint() {
        return createPullPoint;
    }

    public W3CEndpointReference getEpr() {
        return epr;
    }

    public PullPoint create() throws UnableToCreatePullPointFault {
        org.oasis_open.docs.wsn.b_2.CreatePullPoint request
            = new org.oasis_open.docs.wsn.b_2.CreatePullPoint();
        CreatePullPointResponse response = createPullPoint.createPullPoint(request);
        return new PullPoint(response.getPullPoint());
    }

    public PullPoint create(String queueName) throws UnableToCreatePullPointFault {
        org.oasis_open.docs.wsn.b_2.CreatePullPoint request
            = new org.oasis_open.docs.wsn.b_2.CreatePullPoint();
        request.getOtherAttributes().put(NotificationBroker.QNAME_PULLPOINT_QUEUE_NAME, queueName);
        CreatePullPointResponse response = createPullPoint.createPullPoint(request);
        return new PullPoint(response.getPullPoint());
    }
}
