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
package org.apache.cxf.wsn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import junit.framework.TestCase;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.cxf.wsn.client.Consumer;
import org.apache.cxf.wsn.client.CreatePullPoint;
import org.apache.cxf.wsn.client.NotificationBroker;
import org.apache.cxf.wsn.client.Publisher;
import org.apache.cxf.wsn.client.PullPoint;
import org.apache.cxf.wsn.client.Registration;
import org.apache.cxf.wsn.client.Subscription;
import org.apache.cxf.wsn.services.JaxwsCreatePullPoint;
import org.apache.cxf.wsn.services.JaxwsNotificationBroker;
import org.apache.cxf.wsn.util.WSNHelper;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;

public abstract class WsnBrokerTest extends TestCase {
    private boolean useExternal;
    
    
    private ActiveMQConnectionFactory activemq;
    private JaxwsNotificationBroker notificationBrokerServer;
    private JaxwsCreatePullPoint createPullPointServer;
    private NotificationBroker notificationBroker;
    private CreatePullPoint createPullPoint;

    private int port1 = 8182;
    private int port2;
    
    protected abstract String getProviderImpl();
    

    @Override
    public void setUp() throws Exception {
        String impl = getProviderImpl();
        Thread.currentThread()
            .setContextClassLoader(new FakeClassLoader(impl));
    
        System.setProperty("javax.xml.ws.spi.Provider", impl);

        port2 = getFreePort();
        if (!useExternal) {
            port1 = getFreePort();
            
            activemq = new ActiveMQConnectionFactory("vm:(broker:(tcp://localhost:6000)?persistent=false)");

            notificationBrokerServer = new JaxwsNotificationBroker("WSNotificationBroker", activemq);
            notificationBrokerServer.setAddress("http://localhost:" + port1 + "/wsn/NotificationBroker");
            notificationBrokerServer.init();

            createPullPointServer = new JaxwsCreatePullPoint("CreatePullPoint", activemq);
            createPullPointServer.setAddress("http://localhost:" + port1 + "/wsn/CreatePullPoint");
            createPullPointServer.init();
        }


        notificationBroker = new NotificationBroker("http://localhost:" + port1 + "/wsn/NotificationBroker");
        createPullPoint = new CreatePullPoint("http://localhost:" + port1 + "/wsn/CreatePullPoint");
    }

    private int getFreePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }

    @Override
    public void tearDown() throws Exception {
        if (!useExternal) {
            notificationBrokerServer.destroy();
            createPullPointServer.destroy();
        }
        System.clearProperty("javax.xml.ws.spi.Provider");
    }

    public void testBroker() throws Exception {
        TestConsumer callback = new TestConsumer();
        Consumer consumer = new Consumer(callback, "http://localhost:" + port2 + "/test/consumer");

        Subscription subscription = notificationBroker.subscribe(consumer, "myTopic");

        synchronized (callback.notifications) {
            notificationBroker.notify("myTopic", 
                                      new JAXBElement<String>(new QName("urn:test:org", "foo"),
                                          String.class, "bar"));
            callback.notifications.wait(1000000);
        }
        assertEquals(1, callback.notifications.size());
        NotificationMessageHolderType message = callback.notifications.get(0);
        assertEquals(WSNHelper.getWSAAddress(subscription.getEpr()), 
                     WSNHelper.getWSAAddress(message.getSubscriptionReference()));

        subscription.unsubscribe();
        consumer.stop();
    }

    public void testPullPoint() throws Exception {
        PullPoint pullPoint = createPullPoint.create();
        Subscription subscription = notificationBroker.subscribe(pullPoint, "myTopic");
        notificationBroker.notify("myTopic",
                                  new JAXBElement<String>(new QName("urn:test:org", "foo"), 
                                                  String.class, "bar"));

        boolean received = false;
        for (int i = 0; i < 50; i++) {
            List<NotificationMessageHolderType> messages = pullPoint.getMessages(10);
            if (!messages.isEmpty()) {
                received = true;
                break;
            }
            Thread.sleep(100);
        }
        assertTrue(received);

        subscription.unsubscribe();
        pullPoint.destroy();
    }

    public void testPublisher() throws Exception {
        TestConsumer consumerCallback = new TestConsumer();
        Consumer consumer = new Consumer(consumerCallback, "http://localhost:" + port2 + "/test/consumer");

        Subscription subscription = notificationBroker.subscribe(consumer, "myTopic");

        PublisherCallback publisherCallback = new PublisherCallback();
        Publisher publisher = new Publisher(publisherCallback, "http://localhost:" + port2 
                                            + "/test/publisher");
        Registration registration = notificationBroker.registerPublisher(publisher, "myTopic");

        synchronized (consumerCallback.notifications) {
            notificationBroker.notify(publisher, "myTopic",
                                      new JAXBElement<String>(new QName("urn:test:org", "foo"),
                                                      String.class, "bar"));
            consumerCallback.notifications.wait(1000000);
        }
        assertEquals(1, consumerCallback.notifications.size());
        NotificationMessageHolderType message = consumerCallback.notifications.get(0);
        assertEquals(WSNHelper.getWSAAddress(subscription.getEpr()),
                     WSNHelper.getWSAAddress(message.getSubscriptionReference()));
        assertEquals(WSNHelper.getWSAAddress(publisher.getEpr()),
                     WSNHelper.getWSAAddress(message.getProducerReference()));

        subscription.unsubscribe();
        registration.destroy();
        publisher.stop();
        consumer.stop();
    }
    public void testNullPublisherReference() throws Exception {
        TestConsumer consumerCallback = new TestConsumer();
        Consumer consumer = new Consumer(consumerCallback, "http://localhost:" + port2 + "/test/consumer");

        Subscription subscription = notificationBroker.subscribe(consumer, "myTopicNullEPR");

        Publisher publisher = new Publisher(null, null);
        Registration registration = notificationBroker.registerPublisher(publisher, "myTopicNullEPR", false);

        synchronized (consumerCallback.notifications) {
            notificationBroker.notify(publisher, "myTopicNullEPR",
                                      new JAXBElement<String>(new QName("urn:test:org", "foo"),
                                                      String.class, "bar"));
            consumerCallback.notifications.wait(1000000);
        }
        assertEquals(1, consumerCallback.notifications.size());
        NotificationMessageHolderType message = consumerCallback.notifications.get(0);
        assertEquals(WSNHelper.getWSAAddress(subscription.getEpr()),
                     WSNHelper.getWSAAddress(message.getSubscriptionReference()));

        subscription.unsubscribe();
        registration.destroy();
        publisher.stop();
        consumer.stop();
    }
    public void testPublisherOnDemand() throws Exception {
        TestConsumer consumerCallback = new TestConsumer();
        Consumer consumer = new Consumer(consumerCallback, "http://localhost:" + port2 + "/test/consumer");

        PublisherCallback publisherCallback = new PublisherCallback();
        Publisher publisher = new Publisher(publisherCallback, "http://localhost:" 
            + port2 + "/test/publisher");
        Registration registration = notificationBroker.registerPublisher(publisher, 
                                                                         Arrays.asList("myTopic1", 
                                                                                       "myTopic2"), true);

        Subscription subscription = notificationBroker.subscribe(consumer, "myTopic1");
        assertTrue(publisherCallback.subscribed.await(5, TimeUnit.SECONDS));

        synchronized (consumerCallback.notifications) {
            notificationBroker.notify(publisher, "myTopic1",
                                      new JAXBElement<String>(new QName("urn:test:org", "foo"),
                                                      String.class, "bar"));
            consumerCallback.notifications.wait(1000000);
        }

        subscription.unsubscribe();

        assertTrue(publisherCallback.unsubscribed.await(5, TimeUnit.SECONDS));

        registration.destroy();
        publisher.stop();
        consumer.stop();
    }

    public static class TestConsumer implements Consumer.Callback {

        final List<NotificationMessageHolderType> notifications 
            = new ArrayList<NotificationMessageHolderType>();

        public void notify(NotificationMessageHolderType message) {
            synchronized (notifications) {
                notifications.add(message);
                notifications.notify();
            }
        }
    }

    public static class PublisherCallback implements Publisher.Callback {
        final CountDownLatch subscribed = new CountDownLatch(1);
        final CountDownLatch unsubscribed = new CountDownLatch(1);

        public void subscribe(TopicExpressionType topic) {
            subscribed.countDown();
        }

        public void unsubscribe(TopicExpressionType topic) {
            unsubscribed.countDown();
        }
    }

    protected static class FakeClassLoader extends URLClassLoader {
        private final String provider;
        public FakeClassLoader(String provider) {
            super(new URL[0], FakeClassLoader.class.getClassLoader());
            this.provider = provider;
        }
        @Override
        public InputStream getResourceAsStream(String name) {
            if ("META-INF/services/javax.xml.ws.spi.Provider".equals(name)) {
                return provider != null ? new ByteArrayInputStream(provider.getBytes()) : null;
            } else {
                return super.getResourceAsStream(name);
            }
        }
    }

}
