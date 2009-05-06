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

package org.apache.cxf.transport.jms.continuations;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

public class JMSContinuation implements Continuation {

    static final String BOGUS_MESSAGE_SELECTOR = "orgApacheCxfTransportsJmsContinuations='too-many'";
    
    private Bus bus;
    private Message inMessage;
    private MessageObserver incomingObserver;
    private Collection<JMSContinuation> continuations;
    private DefaultMessageListenerContainer jmsListener;
    private JMSConfiguration jmsConfig;
    
    private String currentMessageSelector = BOGUS_MESSAGE_SELECTOR;
    
    private Object userObject;
    
    private boolean isNew = true;
    private boolean isPending;
    private boolean isResumed;
    private Timer timer = new Timer();
    
    public JMSContinuation(Bus b, Message m, MessageObserver observer,
                           Collection<JMSContinuation> cList, 
                           DefaultMessageListenerContainer jmsListener,
                           JMSConfiguration jmsConfig) {
        bus = b;
        inMessage = m;    
        incomingObserver = observer;
        continuations = cList;
        this.jmsListener = jmsListener;
        this.jmsConfig = jmsConfig;
    }    
    
    public Object getObject() {
        return userObject;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isPending() {
        return isPending;
    }

    public boolean isResumed() {
        return isResumed;
    }

    public void reset() {
        cancelTimerTask();
        isNew = true;
        isPending = false;
        isResumed = false;
    }

    public void resume() {
        if (isResumed || !isPending) {
            return;
        }
        isResumed = true;
        cancelTimerTask();
        doResume();
    }
    
    protected void doResume() {
        
        updateContinuations(true);
        
        BusFactory.setThreadDefaultBus(bus);
        try {
            incomingObserver.onMessage(inMessage);
        } finally {
            isPending = false;
            BusFactory.setThreadDefaultBus(null);
        }
    }

    public void setObject(Object o) {
        userObject = o;
    }

    public boolean suspend(long timeout) {
        
        if (isPending) {
            return false;
        }
        
        updateContinuations(false);
                
        isNew = false;
        isResumed = false;
        isPending = true;
        
        if (timeout > 0) {
            createTimerTask(timeout);
        }
        
        throw new SuspendedInvocationException();
    }

    protected void createTimerTask(long timeout) {
        timer.schedule(new TimerTask() {
            public void run() {
                synchronized (JMSContinuation.this) { 
                    if (isPending) {
                        doResume();
                    }
                }
            }
        }, timeout);
    }
    
    protected void cancelTimerTask() {
        timer.cancel();
    }
    
    protected void updateContinuations(boolean remove) {

        if (jmsConfig.getMaxSuspendedContinuations() < 0
            || jmsListener.getCacheLevel() >= DefaultMessageListenerContainer.CACHE_CONSUMER) {
            modifyList(remove);
            return;
        }
        
        // throttle the flow if there're too many continuation instances in memory
        synchronized (continuations) {
            modifyList(remove);
            if (remove && !BOGUS_MESSAGE_SELECTOR.equals(currentMessageSelector)) {
                jmsListener.setMessageSelector(currentMessageSelector);
                currentMessageSelector = BOGUS_MESSAGE_SELECTOR;
            } else if (!remove && continuations.size() >= jmsConfig.getMaxSuspendedContinuations()) {
                currentMessageSelector = jmsListener.getMessageSelector();
                if (!BOGUS_MESSAGE_SELECTOR.equals(currentMessageSelector)) {
                    jmsListener.setMessageSelector(BOGUS_MESSAGE_SELECTOR);
                    
                }
            }
        }

    }
    
    protected void modifyList(boolean remove) {
        if (remove) {
            continuations.remove(this);
        } else {
            continuations.add(this);
        }
    }
    
    String getCurrentMessageSelector() {
        return currentMessageSelector;
    }
    
    
}
