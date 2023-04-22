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
package org.apache.cxf.wsn.jms;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.wsn.AbstractPullPoint;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.UnableToGetMessagesFaultType;
import org.oasis_open.docs.wsn.bw_2.UnableToGetMessagesFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

public class JmsPullPoint extends AbstractPullPoint {

    private static final Logger LOGGER = LogUtils.getL7dLogger(JmsPullPoint.class);

    private JAXBContext jaxbContext;

    private Connection connection;

    private Session producerSession;

    private Session consumerSession;

    private Queue queue;

    private MessageProducer producer;

    private MessageConsumer consumer;

    public JmsPullPoint(String name) {
        super(name);
        try {
            jaxbContext = JAXBContext.newInstance(Notify.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create PullEndpoint", e);
        }
    }

    protected synchronized void initSession() throws JMSException {
        if (producerSession == null || consumerSession == null) {
            producerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            consumerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            queue = producerSession.createQueue(getName());
            producer = producerSession.createProducer(queue);
            consumer = consumerSession.createConsumer(queue);
        }
    }

    protected synchronized void closeSession() {
        if (producerSession != null) {
            try {
                producerSession.close();
            } catch (JMSException inner) {
                LOGGER.log(Level.FINE, "Error closing ProducerSession", inner);
            } finally {
                producerSession = null;
            }
        }

        if (consumerSession != null) {
            try {
                consumerSession.close();
            } catch (JMSException inner) {
                LOGGER.log(Level.FINE, "Error closing ConsumerSession", inner);
            } finally {
                consumerSession = null;
            }
        }
    }

    @Override
    protected void store(NotificationMessageHolderType messageHolder) {
        try {
            initSession();
            Notify notify = new Notify();
            notify.getNotificationMessage().add(messageHolder);
            StringWriter writer = new StringWriter();
            jaxbContext.createMarshaller().marshal(notify, writer);
            synchronized (producerSession) {
                Message message = producerSession.createTextMessage(writer.toString());
                producer.send(message);
            }
        } catch (JMSException e) {
            LOGGER.log(Level.WARNING, "Error storing message", e);
            closeSession();

        } catch (JAXBException e) {
            LOGGER.log(Level.WARNING, "Error storing message", e);
        }
    }

    @Override
    protected List<NotificationMessageHolderType> getMessages(int max)
        throws ResourceUnknownFault, UnableToGetMessagesFault {
        try {
            if (max == 0) {
                max = 256;
            }
            initSession();
            List<NotificationMessageHolderType> messages = new ArrayList<>();
            for (int i = 0; i < max; i++) {
                final Message msg;
                synchronized (consumerSession) {
                    msg = consumer.receiveNoWait();
                }
                if (msg == null) {
                    break;
                }
                TextMessage txtMsg = (TextMessage) msg;
                StringReader reader = new StringReader(txtMsg.getText());
                XMLStreamReader xreader = StaxUtils.createXMLStreamReader(reader);
                Notify notify = (Notify) jaxbContext.createUnmarshaller().unmarshal(xreader);
                try {
                    xreader.close();
                } catch (XMLStreamException e) {
                    //ignoreable
                }
                messages.addAll(notify.getNotificationMessage());
            }
            return messages;
        } catch (JMSException e) {
            LOGGER.log(Level.INFO, "Error retrieving messages", e);
            closeSession();
            UnableToGetMessagesFaultType fault = new UnableToGetMessagesFaultType();
            throw new UnableToGetMessagesFault("Unable to retrieve messages", fault, e);
        } catch (JAXBException e) {
            LOGGER.log(Level.INFO, "Error retrieving messages", e);
            UnableToGetMessagesFaultType fault = new UnableToGetMessagesFaultType();
            throw new UnableToGetMessagesFault("Unable to retrieve messages", fault, e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

}
