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

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.eventing.DeliveryType;
import org.apache.cxf.ws.eventing.ExpirationType;
import org.apache.cxf.ws.eventing.FilterType;
import org.apache.cxf.ws.eventing.FormatType;
import org.apache.cxf.ws.eventing.ObjectFactory;
import org.apache.cxf.ws.eventing.Subscribe;
import org.apache.cxf.ws.eventing.backend.notification.NotificatorService;
import org.apache.cxf.ws.eventing.backend.notification.emitters.Emitter;
import org.apache.cxf.ws.eventing.backend.notification.emitters.EmitterImpl;
import org.apache.cxf.ws.eventing.base.SimpleEventingIntegrationTest;
import org.apache.cxf.ws.eventing.base.TestUtil;
import org.apache.cxf.ws.eventing.integration.eventsink.TestingEventSinkImpl;
import org.apache.cxf.ws.eventing.integration.eventsink.TestingWrappedEventSinkImpl;
import org.apache.cxf.ws.eventing.integration.notificationapi.EarthquakeEvent;
import org.apache.cxf.ws.eventing.integration.notificationapi.FireEvent;
import org.apache.cxf.ws.eventing.shared.EventingConstants;
import org.apache.cxf.ws.eventing.shared.utils.DurationAndDateUtil;

import org.junit.Assert;
import org.junit.Test;

public class NotificationTest extends SimpleEventingIntegrationTest {

    static final String NOTIFICATION_TEST_PORT = allocatePort(NotificationTest.class);

    @Test
    public void basicReceptionOfEvents() throws IOException {
        NotificatorService service = createNotificatorService();
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT0S")));
        subscribe.setExpires(exp);

        EndpointReferenceType eventSinkERT = new EndpointReferenceType();

        AttributedURIType eventSinkAddr = new AttributedURIType();
        String url = TestUtil.generateRandomURLWithHttpTransport(NOTIFICATION_TEST_PORT);
        eventSinkAddr.setValue(url);
        eventSinkERT.setAddress(eventSinkAddr);
        subscribe.setDelivery(new DeliveryType());
        subscribe.getDelivery().getContent()
            .add(new ObjectFactory().createNotifyTo(eventSinkERT));


        eventSourceClient.subscribeOp(subscribe);
        eventSourceClient.subscribeOp(subscribe);
        eventSourceClient.subscribeOp(subscribe);

        Server eventSinkServer = createEventSink(url);
        TestingEventSinkImpl.RECEIVED_FIRES.set(0);

        service.start();
        Emitter emitter = new EmitterImpl(service);
        emitter.dispatch(new FireEvent("Canada", 8));
        for (int i = 0; i < 10; i++) {
            if (TestingEventSinkImpl.RECEIVED_FIRES.get() == 3) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        eventSinkServer.stop();
        if (TestingEventSinkImpl.RECEIVED_FIRES.get() != 3) {
            Assert.fail("TestingEventSinkImpl should have received 3 events but received "
                + TestingEventSinkImpl.RECEIVED_FIRES.get());
        }
    }

    @Test
    public void basicReceptionOfWrappedEvents() throws IOException {
        NotificatorService service = createNotificatorService();
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT0S")));
        subscribe.setExpires(exp);

        EndpointReferenceType eventSinkERT = new EndpointReferenceType();

        AttributedURIType eventSinkAddr = new AttributedURIType();
        String url = TestUtil.generateRandomURLWithHttpTransport(NOTIFICATION_TEST_PORT);
        eventSinkAddr.setValue(url);
        eventSinkERT.setAddress(eventSinkAddr);
        subscribe.setDelivery(new DeliveryType());
        subscribe.getDelivery().getContent().add(new ObjectFactory().createNotifyTo(eventSinkERT));
        FormatType formatType = new FormatType();
        formatType.setName(EventingConstants.DELIVERY_FORMAT_WRAPPED);
        subscribe.setFormat(formatType);

        eventSourceClient.subscribeOp(subscribe);
        eventSourceClient.subscribeOp(subscribe);
        eventSourceClient.subscribeOp(subscribe);

        Server eventSinkServer = createWrappedEventSink(url);
        TestingWrappedEventSinkImpl.RECEIVED_FIRES.set(0);

        service.start();
        Emitter emitter = new EmitterImpl(service);
        emitter.dispatch(new FireEvent("Canada", 8));
        for (int i = 0; i < 10; i++) {
            if (TestingWrappedEventSinkImpl.RECEIVED_FIRES.get() == 3) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        eventSinkServer.stop();
        if (TestingWrappedEventSinkImpl.RECEIVED_FIRES.get() != 3) {
            Assert.fail("TestingWrappedEventSinkImpl should have received 3 events but received "
                + TestingWrappedEventSinkImpl.RECEIVED_FIRES.get());
        }
    }

    @Test
    public void withWSAAction() throws Exception {
        NotificatorService service = createNotificatorService();
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT0S")));
        subscribe.setExpires(exp);

        EndpointReferenceType eventSinkERT = new EndpointReferenceType();

        AttributedURIType eventSinkAddr = new AttributedURIType();
        String url = TestUtil.generateRandomURLWithHttpTransport(NOTIFICATION_TEST_PORT);
        eventSinkAddr.setValue(url);
        eventSinkERT.setAddress(eventSinkAddr);
        subscribe.setDelivery(new DeliveryType());
        subscribe.getDelivery().getContent().add(new ObjectFactory().createNotifyTo(eventSinkERT));

        eventSourceClient.subscribeOp(subscribe);

        Server eventSinkServer = createEventSinkWithWSAActionAssertion(url, "http://www.fire.com");
        TestingEventSinkImpl.RECEIVED_FIRES.set(0);
        service.start();
        Emitter emitter = new EmitterImpl(service);
        emitter.dispatch(new FireEvent("Canada", 8));
        for (int i = 0; i < 10; i++) {
            if (TestingEventSinkImpl.RECEIVED_FIRES.get() == 1) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        eventSinkServer.stop();
        if (TestingEventSinkImpl.RECEIVED_FIRES.get() != 1) {
            Assert.fail("TestingEventSinkImpl should have received 1 events but received "
                    + TestingEventSinkImpl.RECEIVED_FIRES.get());
        }
    }

    @Test
    public void withReferenceParameters() throws Exception {
        NotificatorService service = createNotificatorService();
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT0S")));
        subscribe.setExpires(exp);

        EndpointReferenceType eventSinkERT = new EndpointReferenceType();

        JAXBElement<String> idqn
            = new JAXBElement<>(new QName("http://www.example.org", "MyReferenceParameter"),
                String.class,
                "380");
        JAXBElement<String> idqn2
            = new JAXBElement<>(new QName("http://www.example.org", "MyReferenceParameter2"),
                String.class,
                "381");
        eventSinkERT.setReferenceParameters(new ReferenceParametersType());
        eventSinkERT.getReferenceParameters().getAny().add(idqn);
        eventSinkERT.getReferenceParameters().getAny().add(idqn2);
        AttributedURIType eventSinkAddr = new AttributedURIType();
        String url = TestUtil.generateRandomURLWithHttpTransport(NOTIFICATION_TEST_PORT);
        eventSinkAddr.setValue(url);
        eventSinkERT.setAddress(eventSinkAddr);
        subscribe.setDelivery(new DeliveryType());
        subscribe.getDelivery().getContent().add(new ObjectFactory().createNotifyTo(eventSinkERT));

        eventSourceClient.subscribeOp(subscribe);

        Server eventSinkServer = createEventSinkWithReferenceParametersAssertion(url,
                eventSinkERT.getReferenceParameters());
        TestingEventSinkImpl.RECEIVED_FIRES.set(0);
        service.start();
        Emitter emitter = new EmitterImpl(service);
        emitter.dispatch(new FireEvent("Canada", 8));
        for (int i = 0; i < 10; i++) {
            if (TestingEventSinkImpl.RECEIVED_FIRES.get() == 1) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        eventSinkServer.stop();
        int received = TestingEventSinkImpl.RECEIVED_FIRES.get();
        if (received != 1) {
            Assert.fail("TestingEventSinkImpl should have received 1 events but received "
                    + received);
        }
    }

    /**
     * We request only to receive notifications about fires in Canada
     * and there will be a fire in Canada. We should receive
     * this notification.
     */
    @Test
    public void withFilter() throws IOException {
        NotificatorService service = createNotificatorService();
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT0S")));
        subscribe.setExpires(exp);

        EndpointReferenceType eventSinkERT = new EndpointReferenceType();

        AttributedURIType eventSinkAddr = new AttributedURIType();
        String url = TestUtil.generateRandomURLWithHttpTransport(NOTIFICATION_TEST_PORT);
        eventSinkAddr.setValue(url);
        eventSinkERT.setAddress(eventSinkAddr);
        subscribe.setDelivery(new DeliveryType());
        subscribe.getDelivery().getContent().add(new ObjectFactory().createNotifyTo(eventSinkERT));

        subscribe.setFilter(new FilterType());
        subscribe.getFilter().getContent().add("//*[local-name()='fire' and "
                + "namespace-uri()='http://www.events.com']/location[text()='Canada']");


        eventSourceClient.subscribeOp(subscribe);

        Server eventSinkServer = createEventSink(url);
        TestingEventSinkImpl.RECEIVED_FIRES.set(0);

        service.start();
        Emitter emitter = new EmitterImpl(service);
        emitter.dispatch(new FireEvent("Canada", 8));
        for (int i = 0; i < 10; i++) {
            if (TestingEventSinkImpl.RECEIVED_FIRES.get() == 1) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        eventSinkServer.stop();
        if (TestingEventSinkImpl.RECEIVED_FIRES.get() != 1) {
            Assert.fail("TestingEventSinkImpl should have received 1 events but received "
                    + TestingEventSinkImpl.RECEIVED_FIRES.get());
        }
    }

    /**
     * We request only to receive notifications about fires in Russia
     * and there will be only a fire in Canada. We should not receive
     * this notification.
     */
    @Test
    public void withFilterNegative() throws IOException {
        NotificatorService service = createNotificatorService();
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT0S")));
        subscribe.setExpires(exp);

        EndpointReferenceType eventSinkERT = new EndpointReferenceType();

        AttributedURIType eventSinkAddr = new AttributedURIType();
        String url = TestUtil.generateRandomURLWithHttpTransport(NOTIFICATION_TEST_PORT);
        eventSinkAddr.setValue(url);
        eventSinkERT.setAddress(eventSinkAddr);
        subscribe.setDelivery(new DeliveryType());
        subscribe.getDelivery().getContent().add(new ObjectFactory().createNotifyTo(eventSinkERT));

        subscribe.setFilter(new FilterType());
        subscribe.getFilter().getContent().add("/*[local-name()='fire']/location[text()='Russia']");


        eventSourceClient.subscribeOp(subscribe);

        Server eventSinkServer = createEventSink(url);
        TestingEventSinkImpl.RECEIVED_FIRES.set(0);

        service.start();
        Emitter emitter = new EmitterImpl(service);
        emitter.dispatch(new FireEvent("Canada", 8));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        eventSinkServer.stop();
        if (TestingEventSinkImpl.RECEIVED_FIRES.get() != 0) {
            Assert.fail("TestingEventSinkImpl should have received 0 events but received "
                    + TestingEventSinkImpl.RECEIVED_FIRES.get());
        }
    }


    /**
     * We request only to receive notifications about earthquakes in Russia with Richter scale equal to 3.5
     * and there will be one fire in Canada and one earthquake in Russia. We should
     * receive only one notification.
     */
    @Test
    public void withFilter2() throws IOException {
        NotificatorService service = createNotificatorService();
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT0S")));
        subscribe.setExpires(exp);

        EndpointReferenceType eventSinkERT = new EndpointReferenceType();

        AttributedURIType eventSinkAddr = new AttributedURIType();
        String url = TestUtil.generateRandomURLWithHttpTransport(NOTIFICATION_TEST_PORT);
        eventSinkAddr.setValue(url);
        eventSinkERT.setAddress(eventSinkAddr);
        subscribe.setDelivery(new DeliveryType());
        subscribe.getDelivery().getContent().add(new ObjectFactory().createNotifyTo(eventSinkERT));

        subscribe.setFilter(new FilterType());
        subscribe.getFilter().getContent()
                .add("//*[local-name()='earthquake']/location[text()='Russia']/"
                        + "../richterScale[contains(text(),'3.5')]");

        eventSourceClient.subscribeOp(subscribe);

        Server eventSinkServer = createEventSink(url);
        TestingEventSinkImpl.RECEIVED_FIRES.set(0);
        TestingEventSinkImpl.RECEIVED_EARTHQUAKES.set(0);

        service.start();
        Emitter emitter = new EmitterImpl(service);
        emitter.dispatch(new FireEvent("Canada", 8));
        emitter.dispatch(new EarthquakeEvent(3.5f, "Russia"));
        for (int i = 0; i < 10; i++) {
            if (TestingEventSinkImpl.RECEIVED_EARTHQUAKES.get() == 1) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        eventSinkServer.stop();
        if (TestingEventSinkImpl.RECEIVED_EARTHQUAKES.get() != 1) {
            Assert.fail("TestingEventSinkImpl should have received 1 earthquake event but received "
                    + TestingEventSinkImpl.RECEIVED_EARTHQUAKES.get());
        }
        if (TestingEventSinkImpl.RECEIVED_FIRES.get() != 0) {
            Assert.fail("TestingEventSinkImpl should have not received a fire event"
                    + TestingEventSinkImpl.RECEIVED_FIRES.get());
        }
    }

    /**
     * request a subscription that expires soon
     * an event will be emitted after the expiration
     * we should not receive notification about the event
     * @throws IOException
     */
    @Test
    public void expiration() throws IOException {
        NotificatorService service = createNotificatorService();
        Subscribe subscribe = new Subscribe();
        ExpirationType exp = new ExpirationType();
        exp.setValue(
                DurationAndDateUtil.convertToXMLString(DurationAndDateUtil.parseDurationOrTimestamp("PT1S")));
        subscribe.setExpires(exp);

        EndpointReferenceType eventSinkERT = new EndpointReferenceType();

        AttributedURIType eventSinkAddr = new AttributedURIType();
        String url = TestUtil.generateRandomURLWithHttpTransport(NOTIFICATION_TEST_PORT);
        eventSinkAddr.setValue(url);
        eventSinkERT.setAddress(eventSinkAddr);
        subscribe.setDelivery(new DeliveryType());
        subscribe.getDelivery().getContent().add(new ObjectFactory().createNotifyTo(eventSinkERT));

        eventSourceClient.subscribeOp(subscribe);

        Server eventSinkServer = createEventSink(url);
        TestingEventSinkImpl.RECEIVED_FIRES.set(0);

        service.start();
        Emitter emitter = new EmitterImpl(service);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        emitter.dispatch(new FireEvent("Canada", 8));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        eventSinkServer.stop();
        if (TestingEventSinkImpl.RECEIVED_FIRES.get() != 0) {
            Assert.fail("TestingEventSinkImpl should not have received any events but received "
                    + TestingEventSinkImpl.RECEIVED_FIRES.get());
        }
    }

}
