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

package org.apache.cxf.ws.eventing.integration;

import java.io.IOException;

import org.apache.cxf.ws.eventing.DeliveryType;
import org.apache.cxf.ws.eventing.ExpirationType;
import org.apache.cxf.ws.eventing.GetStatus;
import org.apache.cxf.ws.eventing.GetStatusResponse;
import org.apache.cxf.ws.eventing.Renew;
import org.apache.cxf.ws.eventing.Subscribe;
import org.apache.cxf.ws.eventing.SubscribeResponse;
import org.apache.cxf.ws.eventing.Unsubscribe;
import org.apache.cxf.ws.eventing.UnsubscribeResponse;
import org.apache.cxf.ws.eventing.base.SimpleEventingIntegrationTest;
import org.apache.cxf.ws.eventing.manager.SubscriptionManagerEndpoint;
import org.apache.cxf.ws.eventing.shared.faults.UnknownSubscription;
import org.apache.cxf.ws.eventing.shared.utils.DurationAndDateUtil;

import org.junit.Assert;
import org.junit.Test;



/**
 * Tests to verify that a Subscription Manager can be properly used to manage existing subscriptions.
 * Typically, such test will create a subscription using the Event Source and then invoke
 * possible operations on the Subscription Manager to manage it.
 */
public class SubscriptionManagementTest extends SimpleEventingIntegrationTest {

    /**
     * Creates a subscription and then retrieves its status from the Subscription Manager.
     */
    @Test
    public void getStatus() throws Exception {
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT0S")));
        subscribe.setExpires(exp);
        DeliveryType delivery = new DeliveryType();
        subscribe.setDelivery(delivery);
        subscribe.getDelivery().getContent().add(createDummyNotifyTo());
        SubscribeResponse resp = eventSourceClient.subscribeOp(subscribe);

        SubscriptionManagerEndpoint client = createSubscriptionManagerClient(
                resp.getSubscriptionManager().getReferenceParameters());
        GetStatusResponse response = client.getStatusOp(new GetStatus());
        System.out.println("EXPIRES: " + response.getGrantedExpires().getValue());
        Assert.assertTrue("GetStatus operation should return a XMLGregorianCalendar",
                DurationAndDateUtil.isXMLGregorianCalendar(response.getGrantedExpires().getValue()));
    }

    /**
     * Tries to create a subscription, then cancel it, then obtain its status.
     * The last mentioned operation should fail.
     */
    @Test
    public void unsubscribeAndThenGetStatus() throws Exception {
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT0S")));
        subscribe.setExpires(exp);
        DeliveryType delivery = new DeliveryType();
        subscribe.setDelivery(delivery);
        subscribe.getDelivery().getContent().add(createDummyNotifyTo());
        SubscribeResponse subscribeResponse = eventSourceClient.subscribeOp(subscribe);

        SubscriptionManagerEndpoint client = createSubscriptionManagerClient(
                subscribeResponse.getSubscriptionManager().getReferenceParameters());
        UnsubscribeResponse unsubscribeResponse = client.unsubscribeOp(new Unsubscribe());
        Assert.assertNotNull(unsubscribeResponse);

        try {
            client.getStatusOp(new GetStatus());
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            Assert.assertTrue(ex.getFault().getFaultCode().contains(UnknownSubscription.LOCAL_PART));
            Assert.assertTrue(ex.getFault().getTextContent().contains(UnknownSubscription.REASON));
            return;
        }
        Assert.fail(
                "The subscription manager should have refused to send status of a cancelled subscription");
    }


    /**
     * Tests the Renew operation, while specifying an xs:dateTime in the renew request,
     * eg. the subscriber requests to set the subscription expiration to a specific date/time.
     */
    @Test
    public void renewWithDateTime() throws IOException {
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(DurationAndDateUtil
                .convertToXMLString(DurationAndDateUtil
                        .parseDurationOrTimestamp("2018-10-21T14:52:46.826+02:00")));  // 5 minutes
        subscribe.setExpires(exp);
        DeliveryType delivery = new DeliveryType();
        subscribe.setDelivery(delivery);
        subscribe.getDelivery().getContent().add(createDummyNotifyTo());
        SubscribeResponse resp = eventSourceClient.subscribeOp(subscribe);

        SubscriptionManagerEndpoint client = createSubscriptionManagerClient(
                resp.getSubscriptionManager().getReferenceParameters());
        GetStatusResponse response = client.getStatusOp(new GetStatus());
        String expirationBefore = response.getGrantedExpires().getValue();
        System.out.println("EXPIRES before renew: " + expirationBefore);
        Assert.assertTrue(expirationBefore.length() > 0);

        Renew renewRequest = new Renew();
        ExpirationType renewExp = new ExpirationType();
        renewExp.setValue(DurationAndDateUtil
                .convertToXMLString(DurationAndDateUtil.
                        parseDurationOrTimestamp("2056-10-21T14:54:46.826+02:00")));  // 10 minutes
        renewRequest.setExpires(renewExp);
        client.renewOp(renewRequest);
        response = client.getStatusOp(new GetStatus());
        String expirationAfter = response.getGrantedExpires().getValue();
        System.out.println("EXPIRES after renew: " + expirationAfter);

        Assert.assertFalse("Renew request should change the expiration time at least a bit",
                expirationAfter.equals(expirationBefore));
    }

    /**
     * Tests the Renew operation, while specifying an xs:duration in the renew request,
     * eg. the subscriber requests to prolong the subscription by a specific amount of time.
     */
    @Test
    public void renewWithDuration() throws IOException {
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(DurationAndDateUtil
                .convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT5M0S")));  // 5 minutes
        subscribe.setExpires(exp);
        DeliveryType delivery = new DeliveryType();
        subscribe.setDelivery(delivery);
        subscribe.getDelivery().getContent().add(createDummyNotifyTo());
        SubscribeResponse resp = eventSourceClient.subscribeOp(subscribe);

        SubscriptionManagerEndpoint client = createSubscriptionManagerClient(
                resp.getSubscriptionManager().getReferenceParameters());
        GetStatusResponse response = client.getStatusOp(new GetStatus());
        String expirationBefore = response.getGrantedExpires().getValue();
        System.out.println("EXPIRES before renew: " + expirationBefore);
        Assert.assertTrue(expirationBefore.length() > 0);

        Renew renewRequest = new Renew();
        ExpirationType renewExp = new ExpirationType();
        renewExp.setValue(DurationAndDateUtil
                .convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT10M0S")));  // 10 minutes
        renewRequest.setExpires(renewExp);
        client.renewOp(renewRequest);
        response = client.getStatusOp(new GetStatus());
        String expirationAfter = response.getGrantedExpires().getValue();
        System.out.println("EXPIRES after renew: " + expirationAfter);

        Assert.assertFalse("Renew request should change the expiration time at least a bit",
                expirationAfter.equals(expirationBefore));
    }


}
