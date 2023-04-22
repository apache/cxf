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

import java.math.BigInteger;
import java.util.List;

import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import org.apache.cxf.wsn.util.WSNHelper;
import org.oasis_open.docs.wsn.b_2.DestroyPullPoint;
import org.oasis_open.docs.wsn.b_2.GetMessages;
import org.oasis_open.docs.wsn.b_2.GetMessagesResponse;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroyPullPointFault;
import org.oasis_open.docs.wsn.bw_2.UnableToGetMessagesFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

public class PullPoint implements Referencable {

    private final org.oasis_open.docs.wsn.bw_2.PullPoint pullPoint;
    private final W3CEndpointReference epr;

    public PullPoint(String address) {
        this(WSNHelper.getInstance().createWSA(address));
    }

    public PullPoint(W3CEndpointReference epr) {
        this.pullPoint = WSNHelper.getInstance().getPort(epr, org.oasis_open.docs.wsn.bw_2.PullPoint.class);
        this.epr = epr;
    }

    public org.oasis_open.docs.wsn.bw_2.PullPoint getPullPoint() {
        return pullPoint;
    }

    public W3CEndpointReference getEpr() {
        return this.epr;
    }

    public List<NotificationMessageHolderType> getMessages(long max)
        throws UnableToGetMessagesFault, ResourceUnknownFault {
        GetMessages getMessages = new GetMessages();
        getMessages.setMaximumNumber(BigInteger.valueOf(max));
        GetMessagesResponse response = pullPoint.getMessages(getMessages);
        return response.getNotificationMessage();
    }

    public void destroy() throws UnableToDestroyPullPointFault, ResourceUnknownFault {
        this.pullPoint.destroyPullPoint(new DestroyPullPoint());
    }
}
