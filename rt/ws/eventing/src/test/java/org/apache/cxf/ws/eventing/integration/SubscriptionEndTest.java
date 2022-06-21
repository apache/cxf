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
import java.util.UUID;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.eventing.DeliveryType;
import org.apache.cxf.ws.eventing.ObjectFactory;
import org.apache.cxf.ws.eventing.Subscribe;
import org.apache.cxf.ws.eventing.SubscribeResponse;
import org.apache.cxf.ws.eventing.backend.notification.NotificatorService;
import org.apache.cxf.ws.eventing.backend.notification.SubscriptionEndStatus;
import org.apache.cxf.ws.eventing.base.SimpleEventingIntegrationTest;
import org.apache.cxf.ws.eventing.base.SingletonSubscriptionManagerContainer;
import org.apache.cxf.ws.eventing.base.TestUtil;
import org.apache.cxf.ws.eventing.integration.eventsink.TestingEndToEndpointImpl;

import org.junit.Assert;
import org.junit.Test;

public class SubscriptionEndTest extends SimpleEventingIntegrationTest {

    @Test
    public void doTest() throws IOException {
        NotificatorService service = createNotificatorService();
        service.start();

        Subscribe subscribe = new Subscribe();

        EndpointReferenceType eventSinkERT = new EndpointReferenceType();
        AttributedURIType eventSinkAddr = new AttributedURIType();
        String eventSinkURL = TestUtil.generateRandomURLWithLocalTransport();
        eventSinkAddr.setValue(eventSinkURL);
        eventSinkERT.setAddress(eventSinkAddr);
        subscribe.setDelivery(new DeliveryType());
        subscribe.getDelivery().getContent().add(new ObjectFactory().createNotifyTo(eventSinkERT));

        JAXBElement<String> idqn
            = new JAXBElement<>(new QName("http://www.example.org", "MyReferenceParameter"),
                String.class,
                "380");
        ReferenceParametersType myParams = new ReferenceParametersType();
        myParams.getAny().add(idqn);
        eventSinkERT.setReferenceParameters(myParams);

        EndpointReferenceType endToERT = new EndpointReferenceType();
        AttributedURIType endToAddr = new AttributedURIType();
        String endToURL = TestUtil.generateRandomURLWithLocalTransport();
        endToAddr.setValue(endToURL);
        endToERT.setAddress(endToAddr);
        subscribe.setEndTo(endToERT);

        SubscribeResponse response = eventSourceClient.subscribeOp(subscribe);
        Element referenceParams = (Element)response.getSubscriptionManager()
                .getReferenceParameters().getAny().get(0);

        Server endToEndpoint = createEndToEndpointWithReferenceParametersAssertion(endToURL, myParams);

        TestingEndToEndpointImpl.RECEIVED_ENDS.set(0);

        SingletonSubscriptionManagerContainer.getInstance()
                .subscriptionEnd(UUID.fromString(referenceParams.getTextContent()), "Sorry, "
                        + "but we don't like you anymore",
                        SubscriptionEndStatus.SOURCE_CANCELLING);

        for (int i = 0; i < 10; i++) {
            if (TestingEndToEndpointImpl.RECEIVED_ENDS.get() == 1) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        endToEndpoint.stop();
        if (TestingEndToEndpointImpl.RECEIVED_ENDS.get() != 1) {
            Assert.fail("TestingEndToEndpointImpl should have received 1 subscription end notification but received "
                    + TestingEndToEndpointImpl.RECEIVED_ENDS.get());
        }
    }
}
