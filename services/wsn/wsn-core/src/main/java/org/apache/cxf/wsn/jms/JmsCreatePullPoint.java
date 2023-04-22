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

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import org.apache.cxf.wsn.AbstractCreatePullPoint;
import org.apache.cxf.wsn.AbstractPullPoint;
import org.apache.cxf.wsn.client.NotificationBroker;
import org.oasis_open.docs.wsn.b_2.CreatePullPoint;

public class JmsCreatePullPoint extends AbstractCreatePullPoint {

    protected ConnectionFactory connectionFactory;

    protected Connection connection;

    public JmsCreatePullPoint(String name) {
        super(name);
    }

    public JmsCreatePullPoint(String name, ConnectionFactory connectionFactory) {
        super(name);
        this.connectionFactory = connectionFactory;
    }

    public void init() throws Exception {
        if (connection == null) {
            connection = connectionFactory.createConnection();
            connection.start();
        }
        super.init();
    }

    public void destroy() throws Exception {
        if (connection != null) {
            connection.close();
        }
        super.destroy();
    }

    @Override
    protected String createPullPointName(CreatePullPoint createPullPointRequest) {
        String name;
        if (createPullPointRequest.getOtherAttributes().get(
            NotificationBroker.QNAME_PULLPOINT_QUEUE_NAME) != null) {
            //try use the sepcified pullpoint queue instead a generated one
            //so that we can reuse this durable pullpoint queue between the
            //broker restarts
            name = createPullPointRequest.getOtherAttributes().get(
                              NotificationBroker.QNAME_PULLPOINT_QUEUE_NAME);
        } else {
            name = super.createPullPointName(createPullPointRequest);
        }
        // For JMS, avoid using dashes in the pullpoint name (which is also the queue name,
        // as it will lead to problems with some JMS providers

        name = name.replace("-", "");
        return name;
    }

    @Override
    protected AbstractPullPoint createPullPoint(String name) {
        JmsPullPoint pullPoint = new JmsPullPoint(name);
        pullPoint.setManager(getManager());
        pullPoint.setConnection(connection);
        return pullPoint;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

}
