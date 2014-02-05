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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

public class SessionFactory {
    private ConnectionFactory connectionFactory;
    private ResourceCloser closer;
    private boolean sessionTransacted;
    private int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
    private String durableSubscriptionClientId;
    
    public SessionFactory(ConnectionFactory connectionFactory, ResourceCloser closer) {
        this.connectionFactory = connectionFactory;
        this.closer = closer;
    }
    
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void setSessionTransacted(boolean sessionTransacted) {
        this.sessionTransacted = sessionTransacted;
    }

    public void setAcknowledgeMode(int acknowledgeMode) {
        this.acknowledgeMode = acknowledgeMode;
    }

    public void setDurableSubscriptionClientId(String durableSubscriptionClientId) {
        this.durableSubscriptionClientId = durableSubscriptionClientId;
    }

    public Session createSession() throws JMSException {
        Connection connection = closer.register(connectionFactory.createConnection());
        if (durableSubscriptionClientId != null) {
            connection.setClientID(durableSubscriptionClientId);
        }
        connection.start();
        return closer.register(connection.createSession(sessionTransacted, acknowledgeMode));
    }

}
