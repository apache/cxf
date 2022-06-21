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

import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.ws.eventing.DeliveryType;
import org.apache.cxf.ws.eventing.ExpirationType;
import org.apache.cxf.ws.eventing.FilterType;
import org.apache.cxf.ws.eventing.Subscribe;
import org.apache.cxf.ws.eventing.SubscribeResponse;
import org.apache.cxf.ws.eventing.base.SimpleEventingIntegrationTest;
import org.apache.cxf.ws.eventing.shared.faults.CannotProcessFilter;
import org.apache.cxf.ws.eventing.shared.faults.NoDeliveryMechanismEstablished;
import org.apache.cxf.ws.eventing.shared.utils.DurationAndDateUtil;

import org.junit.Assert;
import org.junit.Test;

public class SubscriptionGrantingTest extends SimpleEventingIntegrationTest {

/*    *//**
     * specification:
     * The expiration time MAY be either a specific time or a duration but MUST
     * be of the same type as the wse:Expires element of the corresponding request.
     * If the corresponding request did not contain a wse:Expires element, this
     * element MUST be a duration (xs:duration).
     *
     * @throws IOException
     */
    @Test
    public void testExpirationGrantingWithoutBestEffort() throws IOException {
        // we specify a xs:duration
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT0S")));
        subscribe.setExpires(exp);
        DeliveryType delivery = new DeliveryType();
        subscribe.setDelivery(delivery);

        subscribe.getDelivery().getContent().add(createDummyNotifyTo());

        SubscribeResponse resp = eventSourceClient.subscribeOp(subscribe);
        Assert.assertTrue(
                "Specification requires that EventSource return a xs:duration "
                        + "expirationType if a xs:duration was requested by client",
                DurationAndDateUtil.isDuration(resp.getGrantedExpires().getValue()));

        // we specify a xs:dateTime
        subscribe = new Subscribe();
        exp = new ExpirationType();
        XMLGregorianCalendar dateRequest =
                (XMLGregorianCalendar)DurationAndDateUtil.parseDurationOrTimestamp("2138-06-26T12:23:12.000-01:00");
        exp.setValue(DurationAndDateUtil.convertToXMLString(dateRequest));
        subscribe.setExpires(exp);
        delivery = new DeliveryType();
        subscribe.setDelivery(delivery);
        subscribe.getDelivery().getContent().add(createDummyNotifyTo());
        resp = eventSourceClient.subscribeOp(subscribe);
        Assert.assertTrue(
                "Specification requires that EventSource return a "
                        + "xs:dateTime expirationType if a xs:dateTime was requested by client",
                DurationAndDateUtil.isXMLGregorianCalendar(resp.getGrantedExpires().getValue()));
        XMLGregorianCalendar returned = DurationAndDateUtil.parseXMLGregorianCalendar(
                resp.getGrantedExpires().getValue());
        System.out.println("granted expiration: " + returned.normalize().toXMLFormat());
        System.out.println("requested expiration: " + dateRequest.normalize().toXMLFormat());
        Assert.assertTrue("Server should have returned exactly the same date as we requested",
                returned.equals(dateRequest));

        // we don't specify anything
        subscribe = new Subscribe();
        delivery = new DeliveryType();
        subscribe.setDelivery(delivery);
        subscribe.getDelivery().getContent().add(createDummyNotifyTo());
        resp = eventSourceClient.subscribeOp(subscribe);
        Assert.assertTrue(
                "Specification requires that EventSource return a xs:duration "
                        + "expirationType if no specific expirationType was requested by client",
                DurationAndDateUtil.isDuration(resp.getGrantedExpires().getValue()));
    }

    /**
     * When BestEffort=true, the server doesn't have to grant exactly the date as we requested
     * @throws IOException
     */
    @Test
    public void testExpirationGrantingWithBestEffort() throws IOException {
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        DeliveryType delivery = new DeliveryType();
        XMLGregorianCalendar dateRequest =
                (XMLGregorianCalendar)DurationAndDateUtil.parseDurationOrTimestamp("2138-06-26T12:23:12.000-01:00");
        exp.setValue(DurationAndDateUtil.convertToXMLString(dateRequest));
        exp.setBestEffort(true);
        subscribe.setExpires(exp);
        subscribe.setDelivery(delivery);
        subscribe.getDelivery().getContent().add(createDummyNotifyTo());
        SubscribeResponse resp = eventSourceClient.subscribeOp(subscribe);
        Assert.assertTrue(
                "Specification requires that EventSource return a "
                        + "xs:dateTime expirationType if a xs:dateTime was requested by client",
                DurationAndDateUtil.isXMLGregorianCalendar(resp.getGrantedExpires().getValue()));
    }

    @Test
    public void noDeliveryMechanismSpecified() throws IOException {
        // we specify a xs:duration
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT0S")));
        subscribe.setExpires(exp);
        try {
            eventSourceClient.subscribeOp(subscribe);
        } catch (SOAPFaultException ex) {
            Assert.assertTrue(ex.getFault().getFaultCode().contains(NoDeliveryMechanismEstablished.LOCAL_PART));
            Assert.assertTrue(ex.getFault().getTextContent().contains(NoDeliveryMechanismEstablished.REASON));
            return;
        }
        Assert.fail("Event source should have sent a NoDeliveryMechanismEstablished fault");
    }

    @Test
    public void cannotProcessFilter() throws IOException {
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        DeliveryType delivery = new DeliveryType();
        XMLGregorianCalendar dateRequest =
                (XMLGregorianCalendar)DurationAndDateUtil.parseDurationOrTimestamp("2138-06-26T12:23:12.000-01:00");
        exp.setValue(DurationAndDateUtil.convertToXMLString(dateRequest));
        exp.setBestEffort(true);
        subscribe.setExpires(exp);
        subscribe.setDelivery(delivery);
        subscribe.getDelivery().getContent().add(createDummyNotifyTo());


        subscribe.setFilter(new FilterType());
        subscribe.getFilter().getContent()
                .add("@^5this-is-not-a-valid-xpath-expression!!!*-/");

        try {
            eventSourceClient.subscribeOp(subscribe);
        } catch (SOAPFaultException ex) {
            Assert.assertTrue(ex.getFault().getFaultCode().contains(CannotProcessFilter.LOCAL_PART));
            Assert.assertTrue(ex.getFault().getTextContent().contains(CannotProcessFilter.REASON));
            return;
        }
        Assert.fail("Event source should have sent a CannotProcessFilter fault");
    }

}

