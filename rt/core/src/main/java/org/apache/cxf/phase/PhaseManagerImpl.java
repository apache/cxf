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

import java.util.SortedSet;

import org.apache.cxf.common.util.SortedArraySet;
import org.apache.cxf.extension.BusExtension;

public class PhaseManagerImpl implements PhaseManager, BusExtension {

    private SortedSet<Phase> inPhases;
    private SortedSet<Phase> outPhases;

    public PhaseManagerImpl() {
        createInPhases();
        createOutPhases();
    }

    public PhaseManagerImpl(SortedSet<Phase> in, SortedSet<Phase> out) {
        inPhases = in;
        outPhases = out;
    }

    public Class<?> getRegistrationType() {
        return PhaseManager.class;
    }

    public SortedSet<Phase> getInPhases() {
        return inPhases;
    }

    public SortedSet<Phase> getOutPhases() {
        return outPhases;
    }

    final void createInPhases() {
        int i = 0;

        inPhases = new SortedArraySet<Phase>();
        inPhases.add(new Phase(Phase.RECEIVE, ++i * 1000));
        inPhases.add(new Phase(Phase.PRE_STREAM, ++i * 1000));
        inPhases.add(new Phase(Phase.USER_STREAM, ++i * 1000));
        inPhases.add(new Phase(Phase.POST_STREAM, ++i * 1000));
        inPhases.add(new Phase(Phase.READ, ++i * 1000));
        inPhases.add(new Phase(Phase.PRE_PROTOCOL, ++i * 1000));
        inPhases.add(new Phase(Phase.USER_PROTOCOL, ++i * 1000));
        inPhases.add(new Phase(Phase.POST_PROTOCOL, ++i * 1000));
        inPhases.add(new Phase(Phase.UNMARSHAL, ++i * 1000));
        inPhases.add(new Phase(Phase.PRE_LOGICAL, ++i * 1000));
        inPhases.add(new Phase(Phase.USER_LOGICAL, ++i * 1000));
        inPhases.add(new Phase(Phase.POST_LOGICAL, ++i * 1000));
        inPhases.add(new Phase(Phase.PRE_INVOKE, ++i * 1000));
        inPhases.add(new Phase(Phase.INVOKE, ++i * 1000));
        inPhases.add(new Phase(Phase.POST_INVOKE, ++i * 1000));
    }

    final void createOutPhases() {

        outPhases = new SortedArraySet<Phase>();
        int i = 0;

        outPhases.add(new Phase(Phase.SETUP, ++i * 1000));
        outPhases.add(new Phase(Phase.PRE_LOGICAL, ++i * 1000));
        outPhases.add(new Phase(Phase.USER_LOGICAL, ++i * 1000));
        outPhases.add(new Phase(Phase.POST_LOGICAL, ++i * 1000));
        outPhases.add(new Phase(Phase.PREPARE_SEND, ++i * 1000));

        outPhases.add(new Phase(Phase.PRE_STREAM, ++i * 1000));

        outPhases.add(new Phase(Phase.PRE_PROTOCOL, ++i * 1000));

        outPhases.add(new Phase(Phase.WRITE, ++i * 1000));
        outPhases.add(new Phase(Phase.PRE_MARSHAL, ++i * 1000));
        outPhases.add(new Phase(Phase.MARSHAL, ++i * 1000));
        outPhases.add(new Phase(Phase.POST_MARSHAL, ++i * 1000));

        outPhases.add(new Phase(Phase.USER_PROTOCOL, ++i * 1000));
        outPhases.add(new Phase(Phase.POST_PROTOCOL, ++i * 1000));

        outPhases.add(new Phase(Phase.USER_STREAM, ++i * 1000));
        outPhases.add(new Phase(Phase.POST_STREAM, ++i * 1000));

        outPhases.add(new Phase(Phase.SEND, ++i * 1000));

        //Make sure ending interceptors are put in positions symmetric
        // to their starting interceptors
        outPhases.add(new Phase(Phase.SEND_ENDING, ++i * 1000));
        
        outPhases.add(new Phase(Phase.POST_STREAM_ENDING, ++i * 1000));
        outPhases.add(new Phase(Phase.USER_STREAM_ENDING, ++i * 1000));

        outPhases.add(new Phase(Phase.POST_PROTOCOL_ENDING, ++i * 1000));
        outPhases.add(new Phase(Phase.USER_PROTOCOL_ENDING, ++i * 1000));

        outPhases.add(new Phase(Phase.MARSHAL_ENDING, ++i * 1000));
        outPhases.add(new Phase(Phase.WRITE_ENDING, ++i * 1000));

        outPhases.add(new Phase(Phase.PRE_PROTOCOL_ENDING, ++i * 1000));
        
        outPhases.add(new Phase(Phase.PRE_STREAM_ENDING, ++i * 1000));

        outPhases.add(new Phase(Phase.PREPARE_SEND_ENDING, ++i * 1000));
        outPhases.add(new Phase(Phase.POST_LOGICAL_ENDING, ++i * 1000));
        outPhases.add(new Phase(Phase.USER_LOGICAL_ENDING, ++i * 1000));
        outPhases.add(new Phase(Phase.PRE_LOGICAL_ENDING, ++i * 1000));
        outPhases.add(new Phase(Phase.SETUP_ENDING, ++i * 1000));

    }





}
