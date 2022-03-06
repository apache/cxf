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

package org.apache.cxf.systest.jaxrs.jms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.JMSBookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JAXRSJmsTest extends AbstractBusClientServerTestBase {
    static final String JMS_PORT = EmbeddedJMSBrokerLauncher.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        Map<String, String> props = new HashMap<>();
        if (System.getProperty("org.apache.activemq.default.directory.prefix") != null) {
            props.put("org.apache.activemq.default.directory.prefix",
                      System.getProperty("org.apache.activemq.default.directory.prefix"));
        }
        props.put("java.util.logging.config.file",
                  System.getProperty("java.util.logging.config.file"));

        assertTrue("server did not launch correctly",
                   launchServer(EmbeddedJMSBrokerLauncher.class, props, null, false));
        assertTrue("server did not launch correctly",
                   launchServer(JMSServer.class, true));
    }

    @Test
    public void testGetBookFromWebClient() throws Exception {
        // setup the the client
        String endpointAddressUrlEncoded = "jms:jndi:dynamicQueues/test.jmstransport.text"
             + "?replyToName=dynamicQueues/test.jmstransport.response"
             + "&jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
             + "&jndiURL=tcp://localhost:" + JMS_PORT;

        WebClient client = WebClient.create(endpointAddressUrlEncoded);
        WebClient.getConfig(client).getInInterceptors().add(new LoggingInInterceptor());
        WebClient.getConfig(client).getRequestContext()
            .put(org.apache.cxf.message.Message.REQUEST_URI, "/bookstore/books/123");

        Book book = client.get(Book.class);
        assertEquals("Get a wrong response code.", 200, client.getResponse().getStatus());
        assertEquals("Get a wrong book id.", 123, book.getId());
    }

    @Test
    public void testGetBookFromWebClientWithTextJMSMessage() throws Exception {
        // setup the the client
        String endpointAddressUrlEncoded = "jms:jndi:dynamicQueues/test.jmstransport.text"
             + "?replyToName=dynamicQueues/test.jmstransport.response"
             + "&jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
             + "&jndiURL=tcp://localhost:" + JMS_PORT
             + "&messageType=text";

        WebClient client = WebClient.create(endpointAddressUrlEncoded);
        WebClient.getConfig(client).getInInterceptors().add(new LoggingInInterceptor());
        WebClient.getConfig(client).getRequestContext()
            .put(org.apache.cxf.message.Message.REQUEST_URI, "/bookstore/books/123");

        Book book = client.get(Book.class);
        assertEquals("Get a wrong response code.", 200, client.getResponse().getStatus());
        assertEquals("Get a wrong book id.", 123, book.getId());
    }

    @Test
    public void testPutBookOneWayWithWebClient() throws Exception {
        // setup the the client
        String endpointAddressUrlEncoded = "jms:jndi:dynamicQueues/test.jmstransport.text"
             + "?replyToName=dynamicQueues/test.jmstransport.response"
             + "&jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
             + "&jndiURL=tcp://localhost:" + JMS_PORT;

        WebClient client = WebClient.create(endpointAddressUrlEncoded);
        WebClient.getConfig(client).getRequestContext()
            .put(org.apache.cxf.message.Message.REQUEST_URI, "/bookstore/oneway");
        client.header("OnewayRequest", "true");
        Response r = client.type("application/xml").put(new Book("OneWay From WebClient", 129L));
        assertEquals(202, r.getStatus());
        assertFalse(r.hasEntity());

        Context ctx = getContext();
        ConnectionFactory factory = (ConnectionFactory)ctx.lookup("ConnectionFactory");

        Destination replyToDestination = (Destination)ctx.lookup("dynamicQueues/test.jmstransport.response");

        Connection connection = null;
        try {
            connection = factory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            checkBookInResponse(session, replyToDestination, 129L, "OneWay From WebClient");
            session.close();
        } finally {
            close(connection);
        }
    }


    @Test
    public void testGetBookFromWebClientWithPath() throws Exception {
        // setup the the client
        String endpointAddressUrlEncoded = "jms:jndi:dynamicQueues/test.jmstransport.text"
             + "?jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
             + "&replyToName=dynamicQueues/test.jmstransport.response"
             + "&jndiURL=tcp://localhost:" + JMS_PORT
             + "&jndiConnectionFactoryName=ConnectionFactory";

        WebClient client = WebClient.create(endpointAddressUrlEncoded);
        client.path("bookstore").path("books").path("123");

        Book book = client.get(Book.class);
        assertEquals("Get a wrong response code.", 200, client.getResponse().getStatus());
        assertEquals("Get a wrong book id.", 123, book.getId());
    }

    @Test
    public void testGetBookFromWebClientWithPathWithTextJMSMessage() throws Exception {
        // setup the the client
        String endpointAddressUrlEncoded = "jms:jndi:dynamicQueues/test.jmstransport.text"
             + "?jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
             + "&replyToName=dynamicQueues/test.jmstransport.response"
             + "&jndiURL=tcp://localhost:" + JMS_PORT
             + "&jndiConnectionFactoryName=ConnectionFactory"
             + "&messageType=text";

        WebClient client = WebClient.create(endpointAddressUrlEncoded);
        client.path("bookstore").path("books").path("123");

        Book book = client.get(Book.class);
        assertEquals("Get a wrong response code.", 200, client.getResponse().getStatus());
        assertEquals("Get a wrong book id.", 123, book.getId());
    }


    @Test
    public void testGetBookFromProxyClient() throws Exception {
        // setup the the client
        String endpointAddressUrlEncoded = "jms:jndi:dynamicQueues/test.jmstransport.text"
             + "?jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
             + "&replyToName=dynamicQueues/test.jmstransport.response"
             + "&jndiURL=tcp://localhost:" + JMS_PORT
             + "&jndiConnectionFactoryName=ConnectionFactory";

        JMSBookStore client = JAXRSClientFactory.create(endpointAddressUrlEncoded, JMSBookStore.class);
        Book book = client.getBook("123");
        assertEquals("Get a wrong response code.", 200, WebClient.client(client).getResponse().getStatus());
        assertEquals("Get a wrong book id.", 123, book.getId());
    }

    @Test
    public void testGetBookFromProxyClientWithTextJMSMessage() throws Exception {
        // setup the the client
        String endpointAddressUrlEncoded = "jms:jndi:dynamicQueues/test.jmstransport.text"
             + "?jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
             + "&replyToName=dynamicQueues/test.jmstransport.response"
             + "&jndiURL=tcp://localhost:" + JMS_PORT
             + "&jndiConnectionFactoryName=ConnectionFactory"
             + "&messageType=text";

        JMSBookStore client = JAXRSClientFactory.create(endpointAddressUrlEncoded, JMSBookStore.class);
        Book book = client.getBook("123");
        assertEquals("Get a wrong response code.", 200, WebClient.client(client).getResponse().getStatus());
        assertEquals("Get a wrong book id.", 123, book.getId());
    }

    @Test
    public void testGetBookFromSubresourceProxyClient() throws Exception {
        // setup the the client
        String endpointAddressUrlEncoded = "jms:jndi:dynamicQueues/test.jmstransport.text"
             + "?jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
             + "&replyToName=dynamicQueues/test.jmstransport.response"
             + "&jndiURL=tcp://localhost:" + JMS_PORT
             + "&jndiConnectionFactoryName=ConnectionFactory";

        JMSBookStore client = JAXRSClientFactory.create(endpointAddressUrlEncoded, JMSBookStore.class);
        Book bookProxy = client.getBookSubResource("123");
        Book book = bookProxy.retrieveState();
        assertEquals("Get a wrong response code.", 200, WebClient.client(bookProxy).getResponse().getStatus());
        assertEquals("Get a wrong book id.", 123, book.getId());
    }

    @Test
    public void testGetBookFromSubresourceProxyClientWithTextJMSMessage() throws Exception {
        // setup the the client
        String endpointAddressUrlEncoded = "jms:jndi:dynamicQueues/test.jmstransport.text"
             + "?jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
             + "&replyToName=dynamicQueues/test.jmstransport.response"
             + "&jndiURL=tcp://localhost:" + JMS_PORT
             + "&jndiConnectionFactoryName=ConnectionFactory"
             + "&messageType=text";

        JMSBookStore client = JAXRSClientFactory.create(endpointAddressUrlEncoded, JMSBookStore.class);
        Book bookProxy = client.getBookSubResource("123");
        Book book = bookProxy.retrieveState();
        assertEquals("Get a wrong response code.", 200, WebClient.client(bookProxy).getResponse().getStatus());
        assertEquals("Get a wrong book id.", 123, book.getId());
    }

    @Test
    public void testGetBookFromProxyClientWithQuery() throws Exception {
        // setup the the client
        String endpointAddressUrlEncoded = "jms:jndi:dynamicQueues/test.jmstransport.text"
             + "?jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
             + "&replyToName=dynamicQueues/test.jmstransport.response"
             + "&jndiURL=tcp://localhost:" + JMS_PORT
             + "&jndiConnectionFactoryName=ConnectionFactory";

        JMSBookStore client = JAXRSClientFactory.create(endpointAddressUrlEncoded, JMSBookStore.class);
        Book book = client.getBookByURLQuery(new String[] {"1", "2", "3"});
        assertEquals("Get a wrong response code.", 200, WebClient.client(client).getResponse().getStatus());
        assertEquals("Get a wrong book id.", 123, book.getId());
    }


    @Test
    public void testGetBookFromProxyClientWithQueryWithTextJMSMessage() throws Exception {
        // setup the the client
        String endpointAddressUrlEncoded = "jms:jndi:dynamicQueues/test.jmstransport.text"
             + "?jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
             + "&replyToName=dynamicQueues/test.jmstransport.response"
             + "&jndiURL=tcp://localhost:" + JMS_PORT
             + "&jndiConnectionFactoryName=ConnectionFactory"
             + "&messageType=text";

        JMSBookStore client = JAXRSClientFactory.create(endpointAddressUrlEncoded, JMSBookStore.class);
        Book book = client.getBookByURLQuery(new String[] {"1", "2", "3"});
        assertEquals("Get a wrong response code.", 200, WebClient.client(client).getResponse().getStatus());
        assertEquals("Get a wrong book id.", 123, book.getId());
    }


    @Test
    public void testGetBook() throws Exception {
        Context ctx = getContext();
        ConnectionFactory factory = (ConnectionFactory)ctx.lookup("ConnectionFactory");

        Destination destination = (Destination)ctx.lookup("dynamicQueues/test.jmstransport.text");
        Destination replyToDestination = (Destination)ctx.lookup("dynamicQueues/test.jmstransport.response");

        Connection connection = null;
        try {
            connection = factory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            postGetMessage(session, destination, replyToDestination);
            checkBookInResponse(session, replyToDestination, 123L, "CXF JMS Rocks");
            session.close();
        } finally {
            close(connection);
        }

    }


    @Test
    public void testAddGetBook() throws Exception {
        Context ctx = getContext();
        ConnectionFactory factory = (ConnectionFactory)ctx.lookup("ConnectionFactory");

        Destination destination = (Destination)ctx.lookup("dynamicQueues/test.jmstransport.text");
        Destination replyToDestination = (Destination)ctx.lookup("dynamicQueues/test.jmstransport.response");

        Connection connection = null;
        try {
            connection = factory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            postBook(session, destination, replyToDestination);
            checkBookInResponse(session, replyToDestination, 124L, "JMS");
            session.close();
        } finally {
            close(connection);
        }

    }

    @Test
    public void testOneWayBook() throws Exception {
        Context ctx = getContext();
        ConnectionFactory factory = (ConnectionFactory)ctx.lookup("ConnectionFactory");

        Destination destination = (Destination)ctx.lookup("dynamicQueues/test.jmstransport.text");
        Destination replyToDestination = (Destination)ctx.lookup("dynamicQueues/test.jmstransport.response");

        Connection connection = null;
        try {
            connection = factory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            postOneWayBook(session, destination);
            checkBookInResponse(session, replyToDestination, 125L, "JMS OneWay");
            session.close();
        } finally {
            close(connection);
        }

    }


    private void checkBookInResponse(Session session, Destination replyToDestination,
                                     long bookId, String bookName) throws Exception {
        MessageConsumer consumer = session.createConsumer(replyToDestination);
        BytesMessage jmsMessage = (BytesMessage)consumer.receive(5000);
        if (jmsMessage == null) {
            throw new RuntimeException("No response recieved on " + replyToDestination);
        }
        byte[] bytes = new byte[(int)jmsMessage.getBodyLength()];
        jmsMessage.readBytes(bytes);
        InputStream is = new ByteArrayInputStream(bytes);
        Book b = readBook(is);
        assertEquals(bookId, b.getId());
        assertEquals(bookName, b.getName());
    }

    private Context getContext() throws Exception {
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                          "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
        props.setProperty(Context.PROVIDER_URL, "tcp://localhost:" + JMS_PORT);
        return new InitialContext(props);

    }

    private void postGetMessage(Session session, Destination destination, Destination replyTo)
        throws Exception {
        MessageProducer producer = session.createProducer(destination);
        Message message = session.createBytesMessage();
        message.setJMSReplyTo(replyTo);
        message.setStringProperty("Accept", "application/xml");
        message.setStringProperty(org.apache.cxf.message.Message.REQUEST_URI, "/bookstore/books/123");
        message.setStringProperty(org.apache.cxf.message.Message.HTTP_REQUEST_METHOD, "GET");
        producer.send(message);
        producer.close();
    }

    private void postOneWayBook(Session session, Destination destination)
        throws Exception {
        MessageProducer producer = session.createProducer(destination);

        byte[] payload = writeBook(new Book("JMS OneWay", 125L));
        BytesMessage message = session.createBytesMessage();
        message.writeBytes(payload);
        message.setStringProperty("Content-Type", "application/xml");
        message.setStringProperty(org.apache.cxf.message.Message.REQUEST_URI, "/bookstore/oneway");
        message.setStringProperty(org.apache.cxf.message.Message.HTTP_REQUEST_METHOD, "PUT");

        producer.send(message);
        producer.close();
    }

    private void postBook(Session session, Destination destination, Destination replyTo)
        throws Exception {
        MessageProducer producer = session.createProducer(destination);
        byte[] payload = writeBook(new Book("JMS", 3L));
        BytesMessage message = session.createBytesMessage();
        message.writeBytes(payload);
        message.setJMSReplyTo(replyTo);
        // or, if oneway,
        // message.setStringProperty("OnewayRequest", "true");
        // we could've set this header in JMSDestination if no replyTo were set
        // but in CXF one could also provide the replyTo in the configuration
        // so it is just simpler to set this header if needed to avoid some
        // complex logic on the server side

        // all these properties are optional
        // CXF JAXRS and JMS Transport will default to
        // Content-Type : text/xml
        // Accept : */*
        // POST
        // Message.REQUEST_URI : "/"

        message.setStringProperty("Content-Type", "application/xml");
        message.setStringProperty("Accept", "text/xml");
        message.setStringProperty(org.apache.cxf.message.Message.REQUEST_URI, "/bookstore/books");
        message.setStringProperty(org.apache.cxf.message.Message.HTTP_REQUEST_METHOD, "POST");
        message.setStringProperty("custom.protocol.header", "custom.value");

        producer.send(message);
        producer.close();
    }

    private Book readBook(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        return (Book)u.unmarshal(is);
    }

    private byte[] writeBook(Book b) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Marshaller m = c.createMarshaller();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        m.marshal(b, bos);
        return bos.toByteArray();
    }

    private void close(Connection connection) {
        try {
            if (connection != null) {
                connection.stop();
                connection.close();
            }
        } catch (JMSException ex) {
            // ignore
        }
    }

}
