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
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.wsn.AbstractSubscription;
import org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.PauseFailedFaultType;
import org.oasis_open.docs.wsn.b_2.ResumeFailedFaultType;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType;
import org.oasis_open.docs.wsn.b_2.UnableToDestroySubscriptionFaultType;
import org.oasis_open.docs.wsn.bw_2.InvalidFilterFault;
import org.oasis_open.docs.wsn.bw_2.InvalidMessageContentExpressionFault;
import org.oasis_open.docs.wsn.bw_2.InvalidProducerPropertiesExpressionFault;
import org.oasis_open.docs.wsn.bw_2.InvalidTopicExpressionFault;
import org.oasis_open.docs.wsn.bw_2.PauseFailedFault;
import org.oasis_open.docs.wsn.bw_2.ResumeFailedFault;
import org.oasis_open.docs.wsn.bw_2.SubscribeCreationFailedFault;
import org.oasis_open.docs.wsn.bw_2.TopicExpressionDialectUnknownFault;
import org.oasis_open.docs.wsn.bw_2.TopicNotSupportedFault;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroySubscriptionFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableInitialTerminationTimeFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableTerminationTimeFault;
import org.oasis_open.docs.wsn.bw_2.UnrecognizedPolicyRequestFault;
import org.oasis_open.docs.wsn.bw_2.UnsupportedPolicyRequestFault;

public abstract class JmsSubscription extends AbstractSubscription implements MessageListener {

    private static final Logger LOGGER = LogUtils.getL7dLogger(JmsSubscription.class);

    private Connection connection;

    private Session session;

    private JmsTopicExpressionConverter topicConverter;

    private Topic jmsTopic;

    private JAXBContext jaxbContext;

    private boolean checkTermination = true;

    private boolean isSessionActive = true;

    private Thread terminationThread;

    public JmsSubscription(String name) {
        super(name);
        topicConverter = new JmsTopicExpressionConverter();
        try {
            jaxbContext = JAXBContext.newInstance(Notify.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXB context", e);
        }
    }

    protected void start() throws SubscribeCreationFailedFault {
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(jmsTopic);
            consumer.setMessageListener(this);
            checkTermination = true;
            isSessionActive = true;
            if (getTerminationTime() != null) {
                terminationThread = new TerminationThread();
                terminationThread.start();
            }
        } catch (JMSException e) {
            SubscribeCreationFailedFaultType fault = new SubscribeCreationFailedFaultType();
            throw new SubscribeCreationFailedFault("Error starting subscription", fault, e);
        }
    }

    @Override
    protected void validateSubscription(Subscribe subscribeRequest)
        //CHECKSTYLE:OFF - WS-Notification spec throws a lot of faults
        throws InvalidFilterFault,
            InvalidMessageContentExpressionFault, InvalidProducerPropertiesExpressionFault,
            InvalidTopicExpressionFault, SubscribeCreationFailedFault, TopicExpressionDialectUnknownFault,
            TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault,
            UnsupportedPolicyRequestFault, UnrecognizedPolicyRequestFault {
        //CHECKSTYLE:ON
        super.validateSubscription(subscribeRequest);
        try {
            jmsTopic = topicConverter.toActiveMQTopic(topic);
        } catch (InvalidTopicException e) {
            InvalidTopicExpressionFaultType fault = new InvalidTopicExpressionFaultType();
            throw new InvalidTopicExpressionFault(e.getMessage(), fault);
        }
    }

    @Override
    protected void pause() throws PauseFailedFault {
        if (session == null) {
            PauseFailedFaultType fault = new PauseFailedFaultType();
            throw new PauseFailedFault("Subscription is already paused", fault);
        }
        try {
            session.close();
            isSessionActive = false;
        } catch (JMSException e) {
            PauseFailedFaultType fault = new PauseFailedFaultType();
            throw new PauseFailedFault("Error pausing subscription", fault, e);
        } finally {
            session = null;
        }
    }

    @Override
    protected void resume() throws ResumeFailedFault {
        if (session != null) {
            ResumeFailedFaultType fault = new ResumeFailedFaultType();
            throw new ResumeFailedFault("Subscription is already running", fault);
        }
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(jmsTopic);
            consumer.setMessageListener(this);
            isSessionActive = true;
        } catch (JMSException e) {
            ResumeFailedFaultType fault = new ResumeFailedFaultType();
            throw new ResumeFailedFault("Error resuming subscription", fault, e);
        }
    }

    @Override
    protected void renew(XMLGregorianCalendar terminationTime) throws UnacceptableTerminationTimeFault {
        try {
            this.resume();
            if (this.terminationThread == null) {
                terminationThread = new TerminationThread();
                terminationThread.start();
            }
        } catch (ResumeFailedFault e) {
            LOGGER.log(Level.WARNING, "renew failed", e);
        }
    }

    @Override
    protected void unsubscribe() throws UnableToDestroySubscriptionFault {
        super.unsubscribe();
        if (session != null) {
            try {
                session.close();
                checkTermination = false;
            } catch (JMSException e) {
                UnableToDestroySubscriptionFaultType fault = new UnableToDestroySubscriptionFaultType();
                throw new UnableToDestroySubscriptionFault("Unable to unsubscribe", fault, e);
            } finally {
                session = null;
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void onMessage(Message jmsMessage) {
        try {
            TextMessage text = (TextMessage) jmsMessage;
            XMLStreamReader reader = StaxUtils.createXMLStreamReader(new StringReader(text.getText()));
            Notify notify = (Notify) jaxbContext.createUnmarshaller()
                    .unmarshal(reader);
            reader.close();
            for (Iterator<NotificationMessageHolderType> ith = notify.getNotificationMessage().iterator();
                ith.hasNext();) {
                NotificationMessageHolderType h = ith.next();
                Object content = h.getMessage().getAny();
                if (!(content instanceof Element)) {
                    DocumentFragment doc = DOMUtils.getEmptyDocument().createDocumentFragment();
                    jaxbContext.createMarshaller().marshal(content, doc);
                    content = DOMUtils.getFirstElement(doc);
                }
                if (!doFilter((Element) content)) {
                    ith.remove();
                } else {
                    h.setTopic(topic);
                    h.setSubscriptionReference(getEpr());
                }
            }
            if (!notify.getNotificationMessage().isEmpty()) {
                doNotify(notify);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error notifying consumer", e);
        }
    }

    protected boolean doFilter(Element content) {
        if (contentFilter != null) {
            if (!contentFilter.getDialect().equals(XPATH1_URI)) {
                throw new IllegalStateException("Unsupported dialect: " + contentFilter.getDialect());
            }
            try {
                XPathFactory xpfactory = XPathFactory.newInstance();
                try {
                    xpfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
                } catch (Throwable t) {
                    //possibly old version, though doesn't really matter as content is already parsed as an Element
                }
                XPath xpath = xpfactory.newXPath();
                XPathExpression exp = xpath.compile(contentFilter.getContent().get(0).toString());
                Boolean ret = (Boolean) exp.evaluate(content, XPathConstants.BOOLEAN);
                return ret.booleanValue();
            } catch (XPathExpressionException e) {
                LOGGER.log(Level.WARNING, "Could not filter notification", e);
            }
            return false;
        }
        return true;
    }

    protected abstract void doNotify(Notify notify);

    class TerminationThread extends Thread {
        public void run() {
            while (checkTermination) {
                XMLGregorianCalendar tt = getTerminationTime();
                if (tt != null && isSessionActive) {
                    XMLGregorianCalendar ct = getCurrentTime();
                    int c = tt.compare(ct);
                    if (c == DatatypeConstants.LESSER || c == DatatypeConstants.EQUAL) {
                        LOGGER.log(Level.INFO, "Need Pause this subscribe");
                        try {
                            pause();
                        } catch (PauseFailedFault e) {
                            LOGGER.log(Level.WARNING, "Pause failed", e);
                        }
                    }
                }
                try {
                    Thread.sleep(2000); // check if should terminate every 2 sec
                } catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, "TerminationThread sleep interrupted", e);
                }
            }
        }
    }

}
