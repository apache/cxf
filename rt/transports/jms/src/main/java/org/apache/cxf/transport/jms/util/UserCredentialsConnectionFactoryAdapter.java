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

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;

public class UserCredentialsConnectionFactoryAdapter implements ConnectionFactory {
    private String userName;
    private String password;
    private ConnectionFactory targetConnectionFactory;

    public void setUsername(String userName2) {
        this.userName = userName2;
    }

    public void setPassword(String password2) {
        this.password = password2;
    }

    public void setTargetConnectionFactory(ConnectionFactory cf) {
        this.targetConnectionFactory = cf;
    }

    @Override
    public Connection createConnection() throws JMSException {
        return createConnection(userName, password);
    }

    @Override
    public Connection createConnection(String userName2, String password2) throws JMSException {
        return targetConnectionFactory.createConnection(userName2, password2);
    }

    @Override
    public JMSContext createContext() {
        return targetConnectionFactory.createContext();
    }

    @Override
    public JMSContext createContext(String userName2, String password2) {
        return targetConnectionFactory.createContext(userName2, password2);
    }

    @Override
    public JMSContext createContext(String userName2, String password2, int sessionMode) {
        return targetConnectionFactory.createContext(userName2, password2, sessionMode);
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        return targetConnectionFactory.createContext(sessionMode);
    }
}
