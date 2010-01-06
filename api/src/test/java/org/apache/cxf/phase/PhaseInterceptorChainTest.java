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

package org.apache.cxf.phase;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.cxf.common.util.SortedArraySet;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PhaseInterceptorChainTest extends Assert {

    private IMocksControl control;

    private PhaseInterceptorChain chain;

    private Message message;

    @Before
    public void setUp() {

        control = EasyMock.createNiceControl();
        message = control.createMock(Message.class);

        Phase phase1 = new Phase("phase1", 1);
        Phase phase2 = new Phase("phase2", 2);
        Phase phase3 = new Phase("phase3", 3);
        SortedSet<Phase> phases = new TreeSet<Phase>();
        phases.add(phase1);
        phases.add(phase2);
        phases.add(phase3);

        chain = new PhaseInterceptorChain(phases);
    }

    @After
    public void tearDown() {
        control.verify();
    }

    @Test
    public void testState() throws Exception {
        AbstractPhaseInterceptor p = setUpPhaseInterceptor("phase1", "p1");
        control.replay();
        chain.add(p);
        
        assertSame("Initial state is State.EXECUTING",
                   InterceptorChain.State.EXECUTING, chain.getState());
        chain.pause();
        assertSame("Pausing chain should lead to State.PAUSED",
                   InterceptorChain.State.PAUSED, chain.getState());
        chain.resume();
        assertSame("Resuming chain should lead to State.COMPLETE",
                   InterceptorChain.State.COMPLETE, chain.getState());
        chain.abort();
        assertSame("Aborting chain should lead to State.ABORTED",
                   InterceptorChain.State.ABORTED, chain.getState());
    }
    
    @Test
    public void testSuspendedException() throws Exception {
        CountingPhaseInterceptor p1 = 
            new CountingPhaseInterceptor("phase1", "p1");
        SuspendedInvocationInterceptor p2 = 
            new SuspendedInvocationInterceptor("phase2", "p2");
        
        message.getInterceptorChain();
        EasyMock.expectLastCall().andReturn(chain).anyTimes();

        control.replay();
        
        chain.add(p1);
        chain.add(p2);
        try {
            chain.doIntercept(message);
            fail("Suspended invocation swallowed");
        } catch (SuspendedInvocationException ex) {
            // ignore
        }
        
        assertSame("No previous interceptor selected", p1, chain.iterator().next());
        assertSame("Suspended invocation should lead to State.PAUSED",
                   InterceptorChain.State.PAUSED, chain.getState());
    }
    
    @Test
    public void testAddOneInterceptor() throws Exception {
        AbstractPhaseInterceptor p = setUpPhaseInterceptor("phase1", "p1");
        control.replay();
        chain.add(p);
        Iterator<Interceptor<? extends Message>> it = chain.iterator();
        assertSame(p, it.next());
        assertTrue(!it.hasNext());
    }
    
    @Test
    public void testForceAddSameInterceptor() throws Exception {

        AbstractPhaseInterceptor p = setUpPhaseInterceptor("phase1", "p1");
        control.replay();
        chain.add(p, false);
        chain.add(p, false);
        Iterator<Interceptor<? extends Message>> it = chain.iterator();
        assertSame(p, it.next());
        assertTrue(!it.hasNext());
        chain.add(p, true);
        it = chain.iterator();
        assertSame(p, it.next());
        assertSame(p, it.next());
        assertTrue(!it.hasNext()); 
    }
    
    @Test
    public void testForceAddSameInterceptorType() throws Exception {

        AbstractPhaseInterceptor p1 = setUpPhaseInterceptor("phase1", "p1");
        AbstractPhaseInterceptor p2 = setUpPhaseInterceptor("phase1", "p1");
        control.replay();
        chain.add(p1, false);
        chain.add(p2, false);
        Iterator<Interceptor<? extends Message>> it = chain.iterator();
        assertSame(p1, it.next());
        assertTrue(!it.hasNext());
        chain.add(p2, true);
        it = chain.iterator();
        assertSame(p1, it.next());
        assertSame(p2, it.next());
        assertTrue(!it.hasNext()); 
    }

    @Test
    public void testAddTwoInterceptorsSamePhase() throws Exception {
        AbstractPhaseInterceptor p1 = setUpPhaseInterceptor("phase1", "p1");
        Set<String> after = new HashSet<String>();
        after.add("p1");
        AbstractPhaseInterceptor p2 = setUpPhaseInterceptor("phase1", "p2", null, after);
        control.replay();
        chain.add(p1);
        chain.add(p2);
        Iterator<Interceptor<? extends Message>> it = chain.iterator();

        assertSame("Unexpected interceptor at this position.", p1, it.next());
        assertSame("Unexpected interceptor at this position.", p2, it.next());
        assertTrue(!it.hasNext());
    }
        
    @Test
    public void testThreeInterceptorSamePhaseWithOrder() throws Exception {
        AbstractPhaseInterceptor p1 = setUpPhaseInterceptor("phase1", "p1");
        Set<String> before = new HashSet<String>();
        before.add("p1");
        AbstractPhaseInterceptor p2 = setUpPhaseInterceptor("phase1", "p2", before, null);
        Set<String> before1 = new HashSet<String>();
        before1.add("p2");
        AbstractPhaseInterceptor p3 = setUpPhaseInterceptor("phase1", "p3", before1, null);
        control.replay();
        chain.add(p3);
        chain.add(p1);
        chain.add(p2);
        
        Iterator<Interceptor<? extends Message>> it = chain.iterator();
        assertSame("Unexpected interceptor at this position.", p3, it.next());
        assertSame("Unexpected interceptor at this position.", p2, it.next());
        assertSame("Unexpected interceptor at this position.", p1, it.next());
        assertTrue(!it.hasNext());
    }

    @Test
    public void testSingleInterceptorPass() throws Exception {
        AbstractPhaseInterceptor p = setUpPhaseInterceptor("phase1", "p1");
        setUpPhaseInterceptorInvocations(p, false, false);
        control.replay();
        chain.add(p);
        chain.doIntercept(message);
    }

    @Test
    public void testSingleInterceptorFail() throws Exception {
        AbstractPhaseInterceptor p = setUpPhaseInterceptor("phase1", "p1");
        setUpPhaseInterceptorInvocations(p, true, true);
        control.replay();
        chain.add(p);
        chain.doIntercept(message);
    }

    @Test
    public void testSingleInterceptorFailWithCustomLogger() throws Exception {
        AbstractPhaseInterceptor p = setUpPhaseInterceptor("phase1", "p1");
        setUpPhaseInterceptorInvocations(p, true, true);
        setUpCustomLogger(true, true, false);
        control.replay();
        chain.add(p);
        chain.doIntercept(message);
    }

    @Test
    public void testSingleInterceptorFailWithCustomLoggerAndDefaultLogging() throws Exception {
        AbstractPhaseInterceptor p = setUpPhaseInterceptor("phase1", "p1");
        setUpPhaseInterceptorInvocations(p, true, true);
        setUpCustomLogger(true, true, true);
        control.replay();
        chain.add(p);
        chain.doIntercept(message);
    }

    @Test
    public void testSingleInterceptorFailWithoutCustomLogger() throws Exception {
        AbstractPhaseInterceptor p = setUpPhaseInterceptor("phase1", "p1");
        setUpPhaseInterceptorInvocations(p, true, true);
        setUpCustomLogger(false, true, false);
        control.replay();
        chain.add(p);
        chain.doIntercept(message);
    }

    @Test
    public void testTwoInterceptorsInSamePhasePass() throws Exception {
        AbstractPhaseInterceptor p1 = setUpPhaseInterceptor("phase1", "p1");
        setUpPhaseInterceptorInvocations(p1, false, false);
        AbstractPhaseInterceptor p2 = setUpPhaseInterceptor("phase1", "p2");
        setUpPhaseInterceptorInvocations(p2, false, false);
        control.replay();
        chain.add(p2);
        chain.add(p1);
        chain.doIntercept(message);
    }

    @Test
    public void testThreeInterceptorsInSamePhaseSecondFail() throws Exception {
        AbstractPhaseInterceptor p1 = setUpPhaseInterceptor("phase1", "p1");
        setUpPhaseInterceptorInvocations(p1, false, true);
        AbstractPhaseInterceptor p2 = setUpPhaseInterceptor("phase1", "p2");
        setUpPhaseInterceptorInvocations(p2, true, true);
        AbstractPhaseInterceptor p3 = setUpPhaseInterceptor("phase1", "p3");
        control.replay();
        chain.add(p1);
        chain.add(p2);
        chain.add(p3);
        chain.doIntercept(message);
    }

    @Test
    public void testTwoInterceptorsInSamePhaseSecondFail() throws Exception {
        AbstractPhaseInterceptor p1 = setUpPhaseInterceptor("phase1", "p1");
        setUpPhaseInterceptorInvocations(p1, false, true);
        AbstractPhaseInterceptor p2 = setUpPhaseInterceptor("phase1", "p2");
        setUpPhaseInterceptorInvocations(p2, true, true);
        control.replay();
        chain.add(p1);
        chain.add(p2);
        chain.doIntercept(message);
    }

    @Test
    public void testTwoInterceptorsInDifferentPhasesPass() throws Exception {
        AbstractPhaseInterceptor p1 = setUpPhaseInterceptor("phase1", "p1");
        setUpPhaseInterceptorInvocations(p1, false, false);
        AbstractPhaseInterceptor p2 = setUpPhaseInterceptor("phase2", "p2");
        setUpPhaseInterceptorInvocations(p2, false, false);
        control.replay();
        chain.add(p1);
        chain.add(p2);
        chain.doIntercept(message);
    }

    @Test
    public void testTwoInterceptorsInDifferentPhasesSecondFail() throws Exception {
        AbstractPhaseInterceptor p1 = setUpPhaseInterceptor("phase1", "p1");
        setUpPhaseInterceptorInvocations(p1, false, true);
        AbstractPhaseInterceptor p2 = setUpPhaseInterceptor("phase2", "p2");
        setUpPhaseInterceptorInvocations(p2, true, true);
        control.replay();
        chain.add(p1);
        chain.add(p2);
        chain.doIntercept(message);
    }

    @Test
    public void testInsertionInDifferentPhasePass() throws Exception {

        AbstractPhaseInterceptor p2 = setUpPhaseInterceptor("phase2", "p2");
        setUpPhaseInterceptorInvocations(p2, false, false);
        AbstractPhaseInterceptor p3 = setUpPhaseInterceptor("phase3", "p3");
        setUpPhaseInterceptorInvocations(p3, false, false);
        InsertingPhaseInterceptor p1 = new InsertingPhaseInterceptor(chain, p2,
                "phase1", "p1");
        control.replay();
        chain.add(p3);
        chain.add(p1);
        chain.doIntercept(message);
        assertEquals(1, p1.invoked);
        assertEquals(0, p1.faultInvoked);
    }

    @Test
    public void testInsertionInSamePhasePass() throws Exception {

        AbstractPhaseInterceptor p2 = setUpPhaseInterceptor("phase1", "p2");
        setUpPhaseInterceptorInvocations(p2, false, false);
        Set<String> after3 = new HashSet<String>();
        after3.add("p2");
        AbstractPhaseInterceptor p3 = setUpPhaseInterceptor("phase1", "p3", null, after3);
        setUpPhaseInterceptorInvocations(p3, false, false);
        InsertingPhaseInterceptor p1 = new InsertingPhaseInterceptor(chain, p3,
                "phase1", "p1");
        p1.addBefore("p2");
        control.replay();
        chain.add(p1);
        chain.add(p2);
        chain.doIntercept(message);
        assertEquals(1, p1.invoked);
        assertEquals(0, p1.faultInvoked);
    }

    @Test
    public void testWrappedInvocation() throws Exception {
        CountingPhaseInterceptor p1 = new CountingPhaseInterceptor("phase1",
                "p1");
        WrapperingPhaseInterceptor p2 = new WrapperingPhaseInterceptor(
                "phase2", "p2");
        CountingPhaseInterceptor p3 = new CountingPhaseInterceptor("phase3",
                "p3");

        message.getInterceptorChain();
        EasyMock.expectLastCall().andReturn(chain).anyTimes();

        control.replay();
        chain.add(p1);
        chain.add(p2);
        chain.add(p3);
        chain.doIntercept(message);
        assertEquals(1, p1.invoked);
        assertEquals(1, p2.invoked);
        assertEquals(1, p3.invoked);
    }

    @Test
    public void testChainInvocationStartFromSpecifiedInterceptor() throws Exception {
        CountingPhaseInterceptor p1 = new CountingPhaseInterceptor("phase1",
                "p1");
        CountingPhaseInterceptor p2 = new CountingPhaseInterceptor(
                "phase2", "p2");
        CountingPhaseInterceptor p3 = new CountingPhaseInterceptor("phase3",
                "p3");

        message.getInterceptorChain();
        EasyMock.expectLastCall().andReturn(chain).anyTimes();

        control.replay();
        chain.add(p1);
        chain.add(p2);
        chain.add(p3);
        chain.doInterceptStartingAfter(message, p2.getId());
        assertEquals(0, p1.invoked);
        assertEquals(0, p2.invoked);
        assertEquals(1, p3.invoked);
    }
    
    AbstractPhaseInterceptor setUpPhaseInterceptor(String phase, String id) throws Exception {
        return setUpPhaseInterceptor(phase, id, null, null);
    }

    AbstractPhaseInterceptor setUpPhaseInterceptor(final String phase, 
                                                   final String id,
                                                   Set<String> b,
                                                   Set<String> a) throws Exception {
        
        AbstractPhaseInterceptor p = control
            .createMock(AbstractPhaseInterceptor.class);
        
        if (a == null) {
            a = new SortedArraySet<String>();
        }
        if (b == null) {
            b = new SortedArraySet<String>();
        }
        Field f = AbstractPhaseInterceptor.class.getDeclaredField("before");
        f.setAccessible(true);
        f.set(p, b);
        
        f = AbstractPhaseInterceptor.class.getDeclaredField("after");
        f.setAccessible(true);
        f.set(p, a);

        f = AbstractPhaseInterceptor.class.getDeclaredField("phase");
        f.setAccessible(true);
        f.set(p, phase);

        f = AbstractPhaseInterceptor.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(p, id);

        return p;
    }
    
    
    @SuppressWarnings("unchecked")
    void setUpPhaseInterceptorInvocations(AbstractPhaseInterceptor p,
            boolean fail, boolean expectFault) {
        p.handleMessage(message);
        if (fail) {
            EasyMock.expectLastCall().andThrow(new RuntimeException());
            message.setContent(EasyMock.isA(Class.class),
                               EasyMock.isA(Exception.class));
            EasyMock.expectLastCall();
        } else {
            EasyMock.expectLastCall();
        }
        if (expectFault) {
            p.handleFault(message);
            EasyMock.expectLastCall();
        }
    }

    private void setUpCustomLogger(boolean useCustomLogger, 
                                   boolean expectFault, 
                                   boolean returnFromCustomLogger) {
        if (useCustomLogger) {
            FaultListener customLogger = control.createMock(FaultListener.class);
            message.getContextualProperty(FaultListener.class.getName());
            EasyMock.expectLastCall().andReturn(customLogger);
            if (expectFault) {
                customLogger.faultOccurred(EasyMock.isA(Exception.class),
                                 EasyMock.isA(String.class), 
                                 EasyMock.isA(Message.class));
                EasyMock.expectLastCall().andReturn(returnFromCustomLogger);
                if (returnFromCustomLogger) {
                    //default logging should also be invoked
                    //not too beautiful way to verify that defaultLogging was invoked.
                    message.get(FaultMode.class);
                    EasyMock.expectLastCall().andReturn(FaultMode.RUNTIME_FAULT);
                }
            }
        } else {
            message.getContextualProperty(FaultListener.class.getName());
            EasyMock.expectLastCall().andReturn(null);
        }

    }


    public class InsertingPhaseInterceptor extends
            AbstractPhaseInterceptor<Message> {
        int invoked;

        int faultInvoked;

        private final PhaseInterceptorChain insertionChain;

        private final AbstractPhaseInterceptor insertionInterceptor;

        public InsertingPhaseInterceptor(PhaseInterceptorChain c,
                AbstractPhaseInterceptor i, String phase, String id) {
            super(id, phase);
            insertionChain = c;
            insertionInterceptor = i;
        }

        public void handleMessage(Message m) {
            insertionChain.add(insertionInterceptor);
            invoked++;
        }

        public void handleFault(Message m) {
            faultInvoked++;
        }
    }

    public class CountingPhaseInterceptor extends
            AbstractPhaseInterceptor<Message> {
        int invoked;

        public CountingPhaseInterceptor(String phase, String id) {
            super(id, phase);
        }

        public void handleMessage(Message m) {
            invoked++;
        }
    }
    
    public class WrapperingPhaseInterceptor extends CountingPhaseInterceptor {
        public WrapperingPhaseInterceptor(String phase, String id) {
            super(phase, id);
        }

        public void handleMessage(Message m) {
            super.handleMessage(m);
            m.getInterceptorChain().doIntercept(m);
        }
    }

    public class SuspendedInvocationInterceptor extends AbstractPhaseInterceptor<Message> {
        
        public SuspendedInvocationInterceptor(String phase, String id) {
            super(id, phase);
        }
        
        public void handleMessage(Message m) {
            throw new SuspendedInvocationException(new Throwable());
        }
    }

}
