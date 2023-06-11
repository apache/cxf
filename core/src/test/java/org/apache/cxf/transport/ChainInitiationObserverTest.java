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

package org.apache.cxf.transport;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChainInitiationObserverTest {

    private TestChain chain;
    private Message message;
    private ChainInitiationObserver observer;

    @Before
    public void setUp() {

        message = mock(Message.class);

        Phase phase1 = new Phase("phase1", 1);
        SortedSet<Phase> phases = new TreeSet<>();
        phases.add(phase1);
        chain = new TestChain(phases);
        observer = new ChainInitiationObserver(null, null);
    }

    @Test
    public void testPausedChain() {
        when(message.getInterceptorChain()).thenReturn(chain);

        observer.onMessage(message);
        assertTrue(chain.isInvoked());

        verify(message, times(2)).getInterceptorChain();
    }

    private static class TestChain extends PhaseInterceptorChain {

        private boolean invoked;

        TestChain(SortedSet<Phase> ps) {
            super(ps);
        }

        @Override
        public void resume() {
            invoked = true;
        }

        @Override
        public State getState() {
            return State.PAUSED;
        }

        public boolean isInvoked() {
            return invoked;
        }
    }
}