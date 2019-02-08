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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

public class JMSSender {
    private boolean explicitQosEnabled;
    private int deliveryMode;
    private int priority;
    private long timeToLive;

    public void setExplicitQosEnabled(boolean explicitQosEnabled) {
        this.explicitQosEnabled = explicitQosEnabled;
    }

    public void setDeliveryMode(int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    public void sendMessage(Session session, Destination targetDest,
                            javax.jms.Message message) throws JMSException {
        MessageProducer producer = null;
        try {
            producer = session.createProducer(targetDest);
            if (explicitQosEnabled) {
                producer.send(message, deliveryMode, priority, timeToLive);
            } else {
                producer.send(message);
            }
        } finally {
            ResourceCloser.close(producer);
        }

    }
}
