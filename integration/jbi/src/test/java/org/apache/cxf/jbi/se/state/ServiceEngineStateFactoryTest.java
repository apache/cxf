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

package org.apache.cxf.jbi.se.state;


import org.apache.cxf.jbi.se.state.ServiceEngineStateMachine.SEOperation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceEngineStateFactoryTest extends Assert {
    private ServiceEngineStateFactory stateFactory;
    private ServiceEngineStateMachine state;
        
    @Before
    public void setUp() throws Exception {
        stateFactory = ServiceEngineStateFactory.getInstance();
        state = stateFactory.getShutdownState();
    }
    
    @Test
    public void testSinglton() throws Exception {
        assertSame(stateFactory, 
                   ServiceEngineStateFactory.getInstance());
    }
    
    @Test
    public void testLifeCycle() throws Exception {
        stateFactory.setCurrentState(state);
        assertSame(state, stateFactory.getCurrentState());
        assertTrue(stateFactory.getCurrentState() instanceof ServiceEngineShutdown);
        state.changeState(SEOperation.init, null);
        state = stateFactory.getCurrentState();
        assertTrue(state instanceof ServiceEngineStop);
        
        state.changeState(SEOperation.start, null);
        state = stateFactory.getCurrentState();
        assertTrue(state instanceof ServiceEngineStart);
        

        state.changeState(SEOperation.stop, null);
        state = stateFactory.getCurrentState();
        assertTrue(state instanceof ServiceEngineStop);
        
        state.changeState(SEOperation.shutdown, null);
        state = stateFactory.getCurrentState();
        assertTrue(state instanceof ServiceEngineShutdown);
    }

}
