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

import javax.jbi.JBIException;

import org.apache.cxf.jbi.se.state.ServiceEngineStateMachine.SEOperation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceEngineStopTest extends Assert {

    private ServiceEngineStateFactory stateFactory;
    private ServiceEngineStateMachine stop;
    
    @Before
    public void setUp() throws Exception {
        stateFactory = ServiceEngineStateFactory.getInstance();
        stop = stateFactory.getStopState();
    }
    
    @Test
    public void testStartOperation() throws Exception {
        stop.changeState(SEOperation.start, null);
        assertTrue(stateFactory.getCurrentState() instanceof ServiceEngineStart);
    }
    
    @Test
    public void testShutdownOperation() throws Exception {
        stop.changeState(SEOperation.shutdown, null);
        assertTrue(stateFactory.getCurrentState() instanceof ServiceEngineShutdown);
    }
    
    @Test
    public void testStopOperation() throws Exception {
        try {
            stop.changeState(SEOperation.stop, null);
        } catch (JBIException e) {
            return;
        }
        fail();
    }
    
    @Test
    public void testInitOperation() throws Exception {
        try {
            stop.changeState(SEOperation.init, null);
        } catch (JBIException e) {
            return;
        }
        fail();
    }
}
