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

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.workqueue.SynchronousExecutor;

/**
 * Waits for JMS messages to be received on a JMS Destination. When a message is received it is forwarded to
 * the JMSDestination.incoming() method using an Executor
 */
class JMSListenerThread extends Thread {
    private static final Logger LOG = LogUtils.getL7dLogger(JMSListenerThread.class);
    private MessageListener messageListener;
    private Executor executor;
    
    private Session session;
    private MessageConsumer consumer;

    public JMSListenerThread(Executor executor, MessageListener messageListener) {
        this.executor = executor;
        this.messageListener = messageListener;
    }

    public void start(Connection connection, Destination destination, String messageSelector,
                     String durableName) throws JMSException {
        if (destination instanceof Queue) {
            QueueSession qSession = ((QueueConnection)connection)
                .createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            consumer = qSession.createReceiver((Queue)destination);
            session = qSession;
        } else {
            TopicSession tSession = ((TopicConnection)connection)
                .createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            TopicSubscriber sub = null;
            if (durableName != null) {
                sub = tSession.createDurableSubscriber((Topic)destination, durableName, messageSelector,
                                                       false);
            } else {
                sub = tSession.createSubscriber((Topic)destination, messageSelector, false);
            }
            consumer = sub;
            session = tSession;
        }
        start();
    }

    public void run() {
        try {
            
            while (true) {
                javax.jms.Message message = consumer.receive();
                if (message == null) {
                    LOG.log(Level.WARNING, "Null message received from message consumer.",
                            " Exiting ListenerThread::run().");
                    return;
                }
                while (message != null) {
                    // REVISIT to get the thread pool
                    // Executor executor = jmsDestination.callback.getExecutor();
                    try {
                        executor.execute(new JMSExecutable(message, messageListener,
                                                           executor instanceof SynchronousExecutor));
                        message = null;
                    } catch (RejectedExecutionException ree) {
                        // FIXME - no room left on workqueue, what to do
                        // for now, loop until it WILL fit on the queue,
                        // although we could just dispatch on this thread.
                    }
                    message = null;
                }
            }
        } catch (Throwable jmsex) {
            LOG.log(Level.SEVERE, "Exiting ListenerThread::run(): ", jmsex);
        } finally {
            try {
                if (consumer != null) {
                    consumer.close();
                }
                if (session != null) {
                    session.close();
                }
            } catch (JMSException e) {
                // Do nothing here
            }
        }
    }

    protected class JMSExecutable implements Runnable {
        private Message message;
        private MessageListener messageListener;
        private boolean inThreadPool;

        JMSExecutable(Message message, MessageListener messageListener, boolean inThreadPool) {
            this.message = message;
            this.messageListener = messageListener;
            this.inThreadPool = inThreadPool;
        }

        public void run() {
            String logMessage = "handle the incoming message in "
                                + (inThreadPool ? "the threadpool" : "listener thread");
            LOG.log(Level.INFO, logMessage);
            try {
                messageListener.onMessage(message);
            } catch (RuntimeException ex) {
                // TODO: Decide what to do if we receive the exception.
                LOG.log(Level.WARNING, "Failed to process incoming message : ", ex);
            }
        }

    }

    public void close() {
        try {
            consumer.close();
        } catch (JMSException e) {
            // do nothing
        }
        try {
            join();
        } catch (InterruptedException e) {
            // Do nothing here
        }
    }
}
