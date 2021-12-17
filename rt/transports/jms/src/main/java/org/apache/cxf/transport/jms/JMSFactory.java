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
import java.util.concurrent.Executors;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.apache.cxf.Bus;
import org.apache.cxf.transport.jms.util.JMSSender;
import org.apache.cxf.workqueue.AutomaticWorkQueue;
import org.apache.cxf.workqueue.WorkQueue;
import org.apache.cxf.workqueue.WorkQueueManager;

/**
 * Factory to create jms helper objects from configuration and context information
 */
public final class JMSFactory {
    public static final String JMS_DESTINATION_EXECUTOR = "org.apache.cxf.extensions.jms.destination.executor";
    public static final String JMS_CONDUIT_EXECUTOR = "org.apache.cxf.extensions.jms.conduit.executor";

    static final String MESSAGE_ENDPOINT_FACTORY = "MessageEndpointFactory";
    static final String MDB_TRANSACTED_METHOD = "MDBTransactedMethod";

    //private static final Logger LOG = LogUtils.getL7dLogger(JMSFactory.class);

    private JMSFactory() {
    }

    /**
     * Create JmsSender from configuration information. Most settings are taken from jmsConfig. The QoS
     * settings in messageProperties override the settings from jmsConfig
     *
     * @param jmsConfig configuration information
     * @param messageProperties context headers override config settings
     * @return
     */
    public static JMSSender createJmsSender(JMSConfiguration jmsConfig,
                                            JMSMessageHeadersType messageProperties) {
        JMSSender sender = new JMSSender();
        long timeToLive = (messageProperties != null && messageProperties.isSetTimeToLive())
            ? messageProperties.getTimeToLive() : jmsConfig.getTimeToLive();
        sender.setTimeToLive(timeToLive);
        int priority = (messageProperties != null && messageProperties.isSetJMSPriority())
            ? messageProperties.getJMSPriority() : jmsConfig.getPriority();
        sender.setPriority(priority);
        int deliveryMode = (messageProperties != null && messageProperties.isSetJMSDeliveryMode())
            ? messageProperties.getJMSDeliveryMode() : jmsConfig.getDeliveryMode();
        sender.setDeliveryMode(deliveryMode);
        sender.setExplicitQosEnabled(jmsConfig.isExplicitQosEnabled());
        return sender;
    }

    static String getMessageSelector(JMSConfiguration jmsConfig, String conduitId) {
        String staticSelectorPrefix = jmsConfig.getConduitSelectorPrefix();
        String conduitIdSt = jmsConfig.isUseConduitIdSelector() && conduitId != null ? conduitId : "";
        String correlationIdPrefix = staticSelectorPrefix + conduitIdSt;
        return correlationIdPrefix.isEmpty() ? null : "JMSCorrelationID LIKE '" + correlationIdPrefix + "%'";
    }

    public static Connection createConnection(final JMSConfiguration jmsConfig) throws JMSException {
        String username = jmsConfig.getUserName();
        ConnectionFactory cf = jmsConfig.getConnectionFactory();
        Connection connection = username != null
            ? cf.createConnection(username, jmsConfig.getPassword())
            : cf.createConnection();
        if (jmsConfig.getDurableSubscriptionClientId() != null) {
            connection.setClientID(jmsConfig.getDurableSubscriptionClientId());
        }
        return connection;
    }

    /**
     * Get workqueue from workqueue manager. Return an executor that will never reject messages and
     * instead block when all threads are used.
     *
     * @param bus
     * @param name
     * @return
     */
    public static Executor createWorkQueueExecutor(Bus bus, String name) {
        WorkQueueManager manager = bus.getExtension(WorkQueueManager.class);
        if (manager != null) {
            AutomaticWorkQueue workQueue1 = manager.getNamedWorkQueue(name);
            final WorkQueue workQueue = (workQueue1 == null) ? manager.getAutomaticWorkQueue() : workQueue1;
            return new Executor() {

                @Override
                public void execute(Runnable command) {
                    workQueue.execute(command, 0);
                }
            };
        }
        return Executors.newFixedThreadPool(20);
    }
}
