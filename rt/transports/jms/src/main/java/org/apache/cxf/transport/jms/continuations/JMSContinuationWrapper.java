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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.continuations.ContinuationWrapper;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.MessageObserver;

public class JMSContinuationWrapper implements ContinuationWrapper {

    private Bus bus;
    private Message inMessage;
    private MessageObserver incomingObserver;
    private List<JMSContinuationWrapper> continuations;
    
    private Object userObject;
    
    private boolean isNew = true;
    private boolean isPending;
    private boolean isResumed;
    private Timer timer = new Timer();
    
    public JMSContinuationWrapper(Bus b,
                                  Message m, 
                                  MessageObserver observer,
                                  List<JMSContinuationWrapper> cList) {
        bus = b;
        inMessage = m;    
        incomingObserver = observer;
        continuations = cList;
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
        cancelTimerTask();
        doResume();
    }
    
    protected void doResume() {
        if (isResumed) {
            return;
        }
        
        synchronized (continuations) {
            continuations.remove(this);
        }
        
        isResumed = true;
        isPending = false;
        isNew = false;

        BusFactory.setThreadDefaultBus(bus);
        try {
            incomingObserver.onMessage(inMessage);
        } finally {
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
        
        synchronized (continuations) {
            continuations.add(this);
        }
        
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
                doResume();
            }
        }, timeout);
    }
    
    protected void cancelTimerTask() {
        timer.cancel();
    }
}
