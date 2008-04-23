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
package org.apache.cxf.binding.corba.types;

import org.apache.cxf.service.model.ServiceInfo;

import org.omg.CORBA.ORB;

public class CorbaStructEventProducer extends AbstractStartEndEventProducer {

    public CorbaStructEventProducer(CorbaObjectHandler h,
                                    ServiceInfo service,
                                    ORB orbRef) {
        CorbaStructHandler handler = (CorbaStructHandler) h;
        name = handler.getName();
        iterator = handler.members.iterator();
        serviceInfo = service;
        orb = orbRef;
    }

    public int next() { 
        int event = states[state];
        if (event != 0) {
            state++;
        } else if (currentEventProducer != null && currentEventProducer.hasNext()) {
            event = currentEventProducer.next();
        } else if (iterator.hasNext()) {
            CorbaObjectHandler obj = iterator.next();
            //Special case for primitive sequence inside struct
            if ((obj instanceof CorbaSequenceHandler)
                && (CorbaHandlerUtils.isPrimitiveIDLTypeSequence(obj))
                && (!((CorbaSequenceHandler)obj).getElements().isEmpty())
                && (!CorbaHandlerUtils.isOctets(obj.getType()))) {
                currentEventProducer =
                    new CorbaPrimitiveSequenceEventProducer(obj, serviceInfo, orb);
            } else {
                currentEventProducer =
                    CorbaHandlerUtils.getTypeEventProducer(obj, serviceInfo, orb);
            }
            event = currentEventProducer.next();
        } else {
            // all done with content, move past state 0
            event = states[++state];
            state++;
            currentEventProducer = null;
        }
        return event;
    }
}
