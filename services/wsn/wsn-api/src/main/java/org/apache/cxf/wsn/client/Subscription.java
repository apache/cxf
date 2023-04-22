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
import org.oasis_open.docs.wsn.b_2.PauseSubscription;
import org.oasis_open.docs.wsn.b_2.Renew;
import org.oasis_open.docs.wsn.b_2.ResumeSubscription;
import org.oasis_open.docs.wsn.b_2.Unsubscribe;
import org.oasis_open.docs.wsn.bw_2.PausableSubscriptionManager;
import org.oasis_open.docs.wsn.bw_2.PauseFailedFault;
import org.oasis_open.docs.wsn.bw_2.ResumeFailedFault;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroySubscriptionFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableTerminationTimeFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

public class Subscription implements Referencable {

    private final PausableSubscriptionManager subscription;
    private final W3CEndpointReference epr;

    public Subscription(String address) {
        this(WSNHelper.getInstance().createWSA(address));
    }

    public Subscription(W3CEndpointReference epr) {
        this.subscription = WSNHelper.getInstance().getPort(epr, PausableSubscriptionManager.class);
        this.epr = epr;
    }

    public PausableSubscriptionManager getSubscription() {
        return subscription;
    }

    public W3CEndpointReference getEpr() {
        return epr;
    }

    public void pause() throws PauseFailedFault, ResourceUnknownFault {
        subscription.pauseSubscription(new PauseSubscription());
    }

    public void resume() throws ResourceUnknownFault, ResumeFailedFault {
        subscription.resumeSubscription(new ResumeSubscription());
    }

    public void renew(String terminationTime) throws ResourceUnknownFault, UnacceptableTerminationTimeFault {
        Renew renew = new Renew();
        renew.setTerminationTime(terminationTime);
        subscription.renew(renew);
    }

    public void unsubscribe() throws UnableToDestroySubscriptionFault, ResourceUnknownFault {
        subscription.unsubscribe(new Unsubscribe());
    }
}
