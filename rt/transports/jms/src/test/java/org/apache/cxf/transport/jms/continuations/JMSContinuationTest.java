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

import java.util.LinkedList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jms.JmsException;
import org.springframework.jms.listener.DefaultMessageListenerContainer;



public class JMSContinuationTest extends Assert {

    private Message m;
    private List<JMSContinuation> continuations;
    private Bus b;
    private MessageObserver observer;
    
    @Before
    public void setUp() {
        m = new MessageImpl();
        continuations = new LinkedList<JMSContinuation>();
        b = BusFactory.getDefaultBus();
        observer = EasyMock.createMock(MessageObserver.class);
    }
    
    @Test
    public void testInitialStatus() {
        JMSContinuation cw = 
            new JMSContinuation(b, m, observer, continuations, null, null);
        assertTrue(cw.isNew());
        assertFalse(cw.isPending());
        assertFalse(cw.isResumed());
    }
    
    @Test
    public void testSuspendResume() {
        TestJMSContinuationWrapper cw = 
            new TestJMSContinuationWrapper(b, m, observer, continuations, null, new JMSConfiguration());
        try {
            cw.suspend(5000);
            fail("SuspendInvocation exception expected");
        } catch (SuspendedInvocationException ex) {
            // ignore
        }
        assertFalse(cw.isNew());
        assertTrue(cw.isPending());
        assertFalse(cw.isResumed());
        
        assertTrue(cw.isTaskCreated());
        assertFalse(cw.isTaskCancelled());
        assertEquals(continuations.size(), 1);
        assertSame(continuations.get(0), cw);
        
        assertFalse(cw.suspend(1000));
        
        observer.onMessage(m);
        EasyMock.expectLastCall();
        EasyMock.replay(observer);
        
        cw.resume();
        
        assertFalse(cw.isNew());
        assertFalse(cw.isPending());
        assertTrue(cw.isResumed());
        
        assertFalse(cw.isTaskCreated());
        assertTrue(cw.isTaskCancelled());
        assertEquals(continuations.size(), 0);
        EasyMock.verify(observer);
    }
    
    @Test
    public void testThrottleWithJmsStartAndStop() {
        
        DefaultMessageListenerContainerStub springContainer = new DefaultMessageListenerContainerStub();
        springContainer.setCacheLevel(2);
        JMSConfiguration config = new JMSConfiguration();
        config.setMaxSuspendedContinuations(1);
        
        TestJMSContinuationWrapper cw = 
            new TestJMSContinuationWrapper(b, m, observer, continuations,
                                           springContainer, config);
        
        assertFalse(springContainer.isStart());
        assertFalse(springContainer.isStop());
        
        suspendResumeCheckStartAndStop(cw, config, springContainer);
        EasyMock.reset(observer);
        suspendResumeCheckStartAndStop(cw, config, springContainer);
        
    }
    
    private void suspendResumeCheckStartAndStop(JMSContinuation cw, JMSConfiguration config,
                                            DefaultMessageListenerContainerStub springContainer) {
        try {
            cw.suspend(5000);
            fail("SuspendInvocation exception expected");
        } catch (SuspendedInvocationException ex) {
            // ignore
        }
        assertEquals(continuations.size(), 1);
        assertSame(continuations.get(0), cw);
        assertTrue(springContainer.isStop());
        
        assertFalse(cw.suspend(1000));
        
        observer.onMessage(m);
        EasyMock.expectLastCall();
        EasyMock.replay(observer);
        
        cw.resume();
        
        assertEquals(continuations.size(), 0);
        assertTrue(springContainer.isStart());
        EasyMock.verify(observer);
    }
    
    @Test
    public void testUserObject() {
        JMSContinuation cw = new JMSContinuation(b, m, observer, continuations, null, null);
        assertNull(cw.getObject());
        Object userObject = new Object();
        cw.setObject(userObject);
        assertSame(userObject, cw.getObject());
    }
    
    private static class TestJMSContinuationWrapper extends JMSContinuation {
        
        private boolean taskCreated;
        private boolean taskCancelled;
        
        public TestJMSContinuationWrapper(Bus b,
                                          Message m, 
                                          MessageObserver observer,
                                          List<JMSContinuation> cList,
                                          DefaultMessageListenerContainer jmsListener,
                                          JMSConfiguration jmsConfig) {
            super(b, m, observer, cList, jmsListener, jmsConfig);
        }
        
        public void createTimerTask(long timeout) {
            taskCreated = true;
        }
        
        public void cancelTimerTask() {
            taskCancelled = true;
        }
        
        public boolean isTaskCreated() {
            boolean result = taskCreated;
            taskCreated = false;
            return result;
        }
        
        public boolean isTaskCancelled() {
            boolean result = taskCancelled;
            taskCancelled = false;
            return result;
        }
    }
    
    private class DefaultMessageListenerContainerStub extends DefaultMessageListenerContainer {
        private boolean start;
        private boolean stop;

        public void start() throws JmsException {
            this.start = true;
            this.stop = false;
        }

        public void stop() throws JmsException {
            this.stop = true;
            this.start = false;
        }

        public boolean isStart() {
            return this.start;
        }

        public boolean isStop() {
            return this.stop;
        }
    }
}
