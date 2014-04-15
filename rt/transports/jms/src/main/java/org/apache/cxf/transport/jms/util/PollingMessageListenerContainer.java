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
package org.apache.cxf.transport.jms.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.cxf.common.logging.LogUtils;

public class PollingMessageListenerContainer extends AbstractMessageListenerContainer {
    private static final Logger LOG = LogUtils.getL7dLogger(PollingMessageListenerContainer.class);

    private ExecutorService pollers;

    private int numListenerThreads = 1;

    public PollingMessageListenerContainer(Connection connection, Destination destination,
                                           MessageListener listenerHandler) {
        this.connection = connection;
        this.destination = destination;
        this.listenerHandler = listenerHandler;
    }

    class Poller implements Runnable {

        @Override
        public void run() {
            ResourceCloser closer = new ResourceCloser();
            while (running) {
                try {
                    if (transactionManager != null) {
                        transactionManager.begin();
                    }
                    Session session = closer.register(connection.createSession(transacted, acknowledgeMode));

                    MessageConsumer consumer = closer.register(session.createConsumer(destination,
                                                                                      messageSelector));
                    Message message = consumer.receive(1000);
                    try {
                        if (message != null) {
                            listenerHandler.onMessage(message);
                        }
                        if (transactionManager != null) {
                            transactionManager.commit();
                        } else {
                            session.commit();
                        }
                    } catch (Exception e) {
                        safeRollBack(session, e);
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Unexpected exception", e);
                } finally {
                    closer.close();
                }
            }

        }

    }

    private void safeRollBack(Session session, Exception e) {
        LOG.log(Level.WARNING, "Exception while processing jms message in cxf. Rolling back", e);
        try {
            if (transactionManager != null) {
                transactionManager.rollback();
            } else {
                session.rollback();
            }
        } catch (Exception e1) {
            LOG.log(Level.WARNING, "Rollback of Local transaction failed", e1);
        }
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        pollers = Executors.newFixedThreadPool(numListenerThreads);
        for (int c = 0; c < numListenerThreads; c++) {
            pollers.execute(new Poller());
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        pollers.shutdown();
        try {
            pollers.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignore
        }
        pollers = null;
    }

    @Override
    public void shutdown() {
        stop();
        ResourceCloser.close(connection);
    }
}
