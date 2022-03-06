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

package org.apache.cxf.wsn.services;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.cxf.wsn.AbstractCreatePullPoint;
import org.apache.cxf.wsn.AbstractNotificationBroker;

/**
 * Starts up an instance of a WS-Notification service
 */
public class Service {
    String rootURL = "http://0.0.0.0:9000/wsn";
    String activeMqUrl = "vm:(broker:(tcp://localhost:6000)?persistent=false)";
    String userName;
    String password;

    boolean jmxEnable = true;

    AbstractCreatePullPoint createPullPointServer;
    AbstractNotificationBroker notificationBrokerServer;

    public Service(String[] args) {
        for (int x = 0; x < args.length; x++) {
            if ("-brokerUrl".equals(args[x])) {
                activeMqUrl = args[++x];
            } else if ("-userName".equals(args[x])) {
                userName = args[++x];
            } else if ("-password".equals(args[x])) {
                password = args[++x];
            } else if ("-rootUrl".equals(args[x])) {
                rootURL = args[++x];
            } else if ("-jmxEnable".equals(args[x])) {
                jmxEnable = Boolean.valueOf(args[++x]);
            }
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        new Service(args).start();

    }

    public void start() throws Exception {
        ActiveMQConnectionFactory activemq = new ActiveMQConnectionFactory(userName, password, activeMqUrl);

        notificationBrokerServer = new JaxwsNotificationBroker("WSNotificationBroker", activemq);
        notificationBrokerServer.setAddress(rootURL + "/NotificationBroker");
        notificationBrokerServer.init();

        createPullPointServer = new JaxwsCreatePullPoint("CreatePullPoint", activemq);
        createPullPointServer.setAddress(rootURL + "/CreatePullPoint");
        createPullPointServer.init();

        if (jmxEnable) {

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

            mbs.registerMBean(notificationBrokerServer, notificationBrokerServer.getMBeanName());

            mbs.registerMBean(createPullPointServer, createPullPointServer.getMBeanName());

        }
    }

}
