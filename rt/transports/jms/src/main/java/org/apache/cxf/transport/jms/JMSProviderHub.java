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

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;


/**
 * This class acts as the hub of JMS provider usage, creating shared
 * JMS Connections and providing access to a pool of JMS Sessions.
 * <p>
 * A new JMS connection is created for each each port based
 * <jms:address> - however its likely that in practice the same JMS
 * provider will be specified for each port, and hence the connection
 * resources could be shared accross ports.
 * <p>
 * For the moment this class is realized as just a container for
 * static methods, but the intention is to support in future sharing
 * of JMS resources accross compatible ports.
 *
 * @author Eoghan Glynn
 */
public final class JMSProviderHub {

    /**
     * Constructor.
     */
    private JMSProviderHub() {
    }

  
    public String toString() {
        return "JMSProviderHub";
    }

    protected static void connect(JMSTransport jmsTransport) throws JMSException, NamingException {
        connect(jmsTransport, null, null);
    }
    
    protected static void connect(JMSTransport jmsTransport, 
                                  ServerConfig jmsDestConfigBean,
                                  ServerBehaviorPolicyType runtimePolicy)
        throws JMSException, NamingException {

        AddressType  addrDetails = jmsTransport.getJMSAddress();
      
        // get JMS connection resources and destination
        //
        Context context = JMSUtils.getInitialContext(addrDetails);
        Connection connection = null;
        
        if (JMSConstants.JMS_QUEUE.equals(addrDetails.getDestinationStyle().value())) {
            QueueConnectionFactory qcf =
                (QueueConnectionFactory)context.lookup(addrDetails.getJndiConnectionFactoryName());
            if (addrDetails.isSetConnectionUserName()) {
                connection = qcf.createQueueConnection(addrDetails.getConnectionUserName(), 
                                                       addrDetails.getConnectionPassword());
            } else {
                connection = qcf.createQueueConnection();
            }
        } else {
            TopicConnectionFactory tcf =
                (TopicConnectionFactory)context.lookup(addrDetails.getJndiConnectionFactoryName());
            if (addrDetails.isSetConnectionUserName()) {
                connection = tcf.createTopicConnection(addrDetails.getConnectionUserName(), 
                                                       addrDetails.getConnectionPassword());
            } else {
                connection = tcf.createTopicConnection();
            }
        }
        
        if (null != jmsDestConfigBean) {
            String clientID = jmsDestConfigBean.getDurableSubscriptionClientId();
            
            if  (clientID != null) {
                connection.setClientID(clientID);
            }
        }
        connection.start();

        Destination requestDestination = 
                (Destination)context.lookup(addrDetails.getJndiDestinationName());

        Destination replyDestination = (null != addrDetails.getJndiReplyDestinationName())
            ? (Destination)context.lookup(addrDetails.getJndiReplyDestinationName()) : null;

        // create session factory to manage session, reply destination,
        // producer and consumer pooling
        //
            
        JMSSessionFactory sf =
            new JMSSessionFactory(connection,
                                  replyDestination,
                                  context,
                                  jmsTransport,
                                  runtimePolicy);

        // notify transport that connection is complete        
        jmsTransport.connected(requestDestination, replyDestination, sf);
    }
}
