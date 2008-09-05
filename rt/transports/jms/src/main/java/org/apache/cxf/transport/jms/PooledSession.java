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

package org.apache.cxf.transport.jms;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TopicSession;

/**
 * Encapsulates pooled session, unidentified producer, destination & associated consumer (certain elements may
 * be null depending on the context).
 * <p>
 * Currently only the point-to-point domain is supported, though the intention is to genericize this to the
 * pub-sub domain also.
 */
public class PooledSession {
    private Session theSession;
    private MessageProducer theProducer;
    private MessageConsumer theConsumer;
    private Queue replyDestination;
    private String correlationID;
    private boolean isQueueStyle;

    /**
     * Constructor.
     */
    PooledSession(Session session, boolean isQueueStyle) {
        this.theSession = session;
        this.isQueueStyle = isQueueStyle;
        this.theProducer = null;
        this.theConsumer = null;
        this.replyDestination = null;
    }

    /**
     * @return the pooled JMS Session
     */
    Session session() {
        return theSession;
    }

    /**
     * @return the unidentified producer
     */
    MessageProducer producer() {
        if (theProducer == null) {
            try {
                if (isQueueStyle) {
                    theProducer = ((QueueSession)theSession).createSender(null);
                } else {
                    theProducer = ((TopicSession)theSession).createPublisher(null);
                }
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }
        return theProducer;
    }

    private String generateUniqueSelector() {
        String host = "localhost";

        try {
            InetAddress addr = InetAddress.getLocalHost();
            host = addr.getHostName();
        } catch (UnknownHostException ukex) {
            // Default to localhost.
        }

        long time = Calendar.getInstance().getTimeInMillis();
        return host + "_" + System.getProperty("user.name") + "_" + this + time;
    }
    
    MessageConsumer consumer() {
        return theConsumer;
    }

    /**
     * @return the per-destination consumer
     */
    public void initConsumerAndReplyDestination(Queue destination) {
        if (!(theSession instanceof QueueSession)) {
            throw new RuntimeException("session should be Queuesession expected");
        }
        if (theConsumer == null) {
            try {
                String selector = null;
                if (null != destination) {
                    replyDestination = destination;
                    selector = "JMSCorrelationID = '" + generateUniqueSelector() + "'";
                } else {
                    replyDestination = theSession.createTemporaryQueue();
                }
                theConsumer = ((QueueSession)theSession).createReceiver(replyDestination, selector);
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return messageSelector if any set.
     */

    String getCorrelationID() throws JMSException {
        if (correlationID == null && theConsumer != null) {
            // Must be request/reply
            String selector = theConsumer.getMessageSelector();

            if (selector != null && selector.startsWith("JMSCorrelationID")) {
                int i = selector.indexOf('\'');
                correlationID = selector.substring(i + 1, selector.length() - 1);
            }
        }

        return correlationID;
    }

    void close() throws JMSException {
        if (theProducer != null) {
            theProducer.close();
        }

        if (theConsumer != null) {
            theConsumer.close();
        }

        if (replyDestination instanceof TemporaryQueue) {
            ((TemporaryQueue)replyDestination).delete();
        }

        if (theSession != null) {
            theSession.close();
        }
    }

    public Queue getReplyDestination() {
        return replyDestination;
    }
}
