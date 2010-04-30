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

import javax.jms.MessageListener;

import org.apache.commons.pool.PoolableObjectFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

public class JMSListenerPoolableObjectFactory implements PoolableObjectFactory {
    private JMSConfiguration jmsConfig;
    private MessageListener handler;
    public JMSListenerPoolableObjectFactory(JMSConfiguration jmsConfig,
                                            MessageListener handler) {
        this.jmsConfig = jmsConfig;
        this.handler = handler;
    }
        
    public void activateObject(Object obj) throws Exception {
        DefaultMessageListenerContainer listener = (DefaultMessageListenerContainer)obj;
        listener.start();
    }

    public void destroyObject(Object obj) throws Exception {
        DefaultMessageListenerContainer listener = (DefaultMessageListenerContainer)obj;
        listener.destroy();
    }

    public Object makeObject() throws Exception {
        Object obj = JMSFactory.createJmsListener(jmsConfig, 
                                                  handler,
                                                  jmsConfig.getReplyDestination(), 
                                                  null,
                                                  false);
        DefaultMessageListenerContainer listener = (DefaultMessageListenerContainer)obj;
        listener.setCacheLevel(DefaultMessageListenerContainer.CACHE_SESSION);

        return listener;
    }

    public void passivateObject(Object obj) throws Exception {
        DefaultMessageListenerContainer listener = (DefaultMessageListenerContainer)obj;
        listener.stop();
    }

    public boolean validateObject(Object obj) {
        DefaultMessageListenerContainer listener = (DefaultMessageListenerContainer)obj;
        
        return listener.isActive();
    }
}
