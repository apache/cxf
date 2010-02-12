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

package org.apache.cxf.systest.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.transport.jms.JMSUtils;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSJmsTest extends AbstractBusClientServerTestBase {

    protected static boolean serversStarted;
    
    @BeforeClass
    public static void startServers() throws Exception {
        if (serversStarted) {
            return;
        }
        Map<String, String> props = new HashMap<String, String>();                
        if (System.getProperty("activemq.store.dir") != null) {
            props.put("activemq.store.dir", System.getProperty("activemq.store.dir"));
        }
        props.put("java.util.logging.config.file", 
                  System.getProperty("java.util.logging.config.file"));
        
        assertTrue("server did not launch correctly", 
                   launchServer(EmbeddedJMSBrokerLauncher.class, props, null));
        assertTrue("server did not launch correctly",
                   launchServer(JMSServer.class));
        serversStarted = true;
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
            try {
                connection.stop();
                connection.close();
            } catch (JMSException ex) {
                // ignore
            }
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
            try {
                connection.stop();
                connection.close();
            } catch (JMSException ex) {
                // ignore
            }
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
            try {
                connection.stop();
                connection.close();
            } catch (JMSException ex) {
                // ignore
            }
        }
        
    }
    
    
    private void checkBookInResponse(Session session, Destination replyToDestination,
                                     long bookId, String bookName) throws Exception {
        MessageConsumer consumer = session.createConsumer(replyToDestination);
        Message jmsMessage = consumer.receive(300000);
        org.apache.cxf.message.Message cxfMessage = new org.apache.cxf.message.MessageImpl();
        byte[] bytes = JMSUtils.retrievePayload(jmsMessage, null);
        cxfMessage.setContent(InputStream.class, new ByteArrayInputStream(bytes));
        Book b = readBook(cxfMessage.getContent(InputStream.class));
        assertEquals(bookId, b.getId());
        assertEquals(bookName, b.getName());
    }
    
    private Context getContext() throws Exception {
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                          "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61500");
        return new InitialContext(props);
        
    }
    
    private void postGetMessage(Session session, Destination destination, Destination replyTo) 
        throws Exception {
        MessageProducer producer = session.createProducer(destination);
        Message message = session.createMessage();
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
        
        Message message = JMSUtils.createAndSetPayload(
            writeBook(new Book("JMS OneWay", 125L)), session, "text");
        message.setStringProperty("Content-Type", "application/xml");
        message.setStringProperty(org.apache.cxf.message.Message.REQUEST_URI, "/bookstore/oneway");
        message.setStringProperty(org.apache.cxf.message.Message.HTTP_REQUEST_METHOD, "PUT");
                    
        producer.send(message);
        producer.close();
    }
    
    private void postBook(Session session, Destination destination, Destination replyTo) 
        throws Exception {
        MessageProducer producer = session.createProducer(destination);
        
        Message message = JMSUtils.createAndSetPayload(writeBook(new Book("JMS", 3L)), session, "text");
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
    
    private String writeBook(Book b) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Marshaller m = c.createMarshaller();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        m.marshal(b, bos);
        return bos.toString();
    }
    
    
}
