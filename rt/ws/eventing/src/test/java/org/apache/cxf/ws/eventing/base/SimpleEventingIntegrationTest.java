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

package org.apache.cxf.ws.eventing.base;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.eventing.ObjectFactory;
import org.apache.cxf.ws.eventing.backend.database.SubscriptionTicket;
import org.apache.cxf.ws.eventing.backend.manager.SubscriptionManagerInterfaceForNotificators;
import org.apache.cxf.ws.eventing.backend.notification.EventSinkInterfaceNotificatorService;
import org.apache.cxf.ws.eventing.backend.notification.NotificatorService;
import org.apache.cxf.ws.eventing.base.services.TestingEventSource;
import org.apache.cxf.ws.eventing.base.services.TestingSubscriptionManager;
import org.apache.cxf.ws.eventing.eventsource.EventSourceEndpoint;
import org.apache.cxf.ws.eventing.integration.eventsink.TestingEndToEndpointImpl;
import org.apache.cxf.ws.eventing.integration.eventsink.TestingEventSinkImpl;
import org.apache.cxf.ws.eventing.integration.eventsink.TestingWrappedEventSinkImpl;
import org.apache.cxf.ws.eventing.integration.notificationapi.CatastrophicEventSink;
import org.apache.cxf.ws.eventing.integration.notificationapi.assertions.ReferenceParametersAssertingHandler;
import org.apache.cxf.ws.eventing.integration.notificationapi.assertions.WSAActionAssertingHandler;
import org.apache.cxf.ws.eventing.manager.SubscriptionManagerEndpoint;
import org.apache.cxf.ws.eventing.shared.EventingConstants;
import org.apache.cxf.ws.eventing.shared.handlers.ReferenceParametersAddingHandler;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Prepared class for simple development of integration tests for WS-Eventing. Extend it and you will get:
 * - Event Source web service available at local://SimpleEventSource
 * - Subscription Manager web service available at local://SimpleSubscriptionManager
 * - These two services are connected together, using an IN-VM backend/database instance
 * - JAX-WS client for the Event Source [the eventSourceClient property]
 * - ability to create a JAX-WS client for the Subscription Manager (the createSubscriptionManagerClient method)
 */
public abstract class SimpleEventingIntegrationTest {

    public static final String URL_EVENT_SOURCE = "local://SimpleEventSource";
    public static final String URL_SUBSCRIPTION_MANAGER = "local://SimpleSubscriptionManager";

    static Server eventSource;
    static Server subscriptionManager;
    static Bus bus;
    protected EventSourceEndpoint eventSourceClient;

    protected NotificatorService createNotificatorService() {
        return new EventSinkInterfaceNotificatorService() {
            @Override
            protected SubscriptionManagerInterfaceForNotificators obtainManager() {
                return SingletonSubscriptionManagerContainer.getInstance();
            }

            @Override
            protected Class<?> getEventSinkInterface() {
                return CatastrophicEventSink.class;
            }
        };
    }

    protected Server createEndToEndpoint(String address) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestingEndToEndpointImpl());
        factory.setAddress(address);
        factory.getHandlers().add(new WSAActionAssertingHandler(EventingConstants.ACTION_SUBSCRIPTION_END));
        return factory.create();
    }

    protected Server createEndToEndpointWithReferenceParametersAssertion(String address,
                                                                         ReferenceParametersType params) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestingEndToEndpointImpl());
        factory.setAddress(address);
        factory.getHandlers().add(new ReferenceParametersAssertingHandler(params));
        factory.getHandlers().add(new WSAActionAssertingHandler(EventingConstants.ACTION_SUBSCRIPTION_END));
        return factory.create();
    }

    protected Server createEventSink(String address) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestingEventSinkImpl());
        factory.setAddress(address);
        return factory.create();
    }

    protected Server createWrappedEventSink(String address) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestingWrappedEventSinkImpl());
        factory.setAddress(address);
        return factory.create();
    }

    protected Server createEventSinkWithWSAActionAssertion(String address, String action) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestingEventSinkImpl());
        factory.setAddress(address);
        factory.getHandlers().add(new WSAActionAssertingHandler(action));
        return factory.create();
    }

    protected Server createEventSinkWithReferenceParametersAssertion(String address, ReferenceParametersType params) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestingEventSinkImpl());
        factory.setAddress(address);
        factory.getHandlers().add(new ReferenceParametersAssertingHandler(params));
        return factory.create();
    }


    /**
     * Prepares the Event Source and Subscription Manager services
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        bus = BusFactory.getDefaultBus();
        // create and publish event source
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestingEventSource());
        factory.setAddress(URL_EVENT_SOURCE);
        factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        eventSource = factory.create();

        // create and publish subscription manager
        factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestingSubscriptionManager());
        factory.setAddress(URL_SUBSCRIPTION_MANAGER);
        factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        subscriptionManager = factory.create();
        new LoggingFeature().initialize(subscriptionManager, bus);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        eventSource.destroy();
        subscriptionManager.destroy();
        bus.shutdown(true);
        bus = null;
    }

    @Before
    public void createClient() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        factory.setServiceClass(EventSourceEndpoint.class);
        factory.setAddress(URL_EVENT_SOURCE);
        eventSourceClient = (EventSourceEndpoint)factory.create();
    }

    @After
    public void after() {
        // remove all subscriptions from the database
        for (SubscriptionTicket ticket : SingletonSubscriptionManagerContainer.getInstance().getTickets()) {
            SingletonSubscriptionManagerContainer.getInstance().unsubscribeTicket(ticket.getUuid());
        }
    }

    /**
     * Convenience method to create a client for the testing Subscription Manager
     * which is located at local://SimpleSubscriptionManager.
     * You have to specify the reference parameters you obtained from the Event Source
     * when your subscription was created.
     *
     * @return a JAX-WS client set up for managing the subscription you had created using the Event Source
     */
    public SubscriptionManagerEndpoint createSubscriptionManagerClient(ReferenceParametersType refs) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(SubscriptionManagerEndpoint.class);
        factory.setAddress(URL_SUBSCRIPTION_MANAGER);
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        ReferenceParametersAddingHandler handler = new ReferenceParametersAddingHandler(refs);
        factory.getHandlers().add(handler);
        return (SubscriptionManagerEndpoint)factory.create();
    }

    protected JAXBElement<EndpointReferenceType> createDummyNotifyTo() {
        EndpointReferenceType eventSinkERT = new EndpointReferenceType();
        AttributedURIType eventSinkAddr = new AttributedURIType();
        eventSinkAddr.setValue("local://dummy-sink");
        eventSinkERT.setAddress(eventSinkAddr);
        return new ObjectFactory().createNotifyTo(eventSinkERT);
    }

    protected static String allocatePort(Class<?> cls) {
        return org.apache.cxf.testutil.common.TestUtil.getPortNumber(cls);
    }

}
