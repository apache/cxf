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

import org.apache.cxf.binding.corba.wsdl.Abstractanonsequence;
import org.apache.cxf.binding.corba.wsdl.Abstractsequence;
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
        if (handler.members.isEmpty()
            && handler.getSimpleName().equals(handler.getIdlType().getLocalPart() + "_f")) {
            state = states.length;
        }

    }

    @SuppressWarnings("PMD.IdenticalConditionalBranches")
    public int next() {
        int event = states[state];
        if (event != 0) {
            state++;
        } else if (currentEventProducer != null && currentEventProducer.hasNext()) {
            event = currentEventProducer.next();
        } else if (iterator.hasNext()) {
            CorbaObjectHandler obj = iterator.next();
            //Special case for wrapped/unwrapped arrays or sequences
            boolean primitiveSequence = obj instanceof CorbaSequenceHandler
                        && !CorbaHandlerUtils.isOctets(obj.getType());
            boolean primitivearray = obj instanceof CorbaArrayHandler
                        && !CorbaHandlerUtils.isOctets(obj.getType());
            if (primitiveSequence || primitivearray) {
                boolean wrapped = true;
                if (obj.getType() instanceof Abstractanonsequence) {
                    wrapped = ((Abstractanonsequence)obj.getType()).isWrapped();
                } else if (obj.getType() instanceof Abstractsequence) {
                    wrapped = ((Abstractsequence)obj.getType()).isWrapped();
                }
                if (obj instanceof CorbaSequenceHandler) {
                    if (wrapped) {
                        currentEventProducer = new CorbaSequenceEventProducer(obj, serviceInfo, orb);
                    } else {
                        currentEventProducer = new CorbaPrimitiveSequenceEventProducer(obj, serviceInfo, orb);
                    }
                } else {
                    if (wrapped) {
                        currentEventProducer = new CorbaArrayEventProducer(obj, serviceInfo, orb);
                    } else {
                        currentEventProducer = new CorbaPrimitiveArrayEventProducer(obj, serviceInfo, orb);
                    }
                }
            } else if (obj.getSimpleName().equals(obj.getIdlType().getLocalPart() + "_f")) {
                //some "special cases" we need to make sure are mapped correctly

                currentEventProducer =
                    CorbaHandlerUtils.getTypeEventProducer(obj, serviceInfo, orb);

            } else {
                currentEventProducer =
                    CorbaHandlerUtils.getTypeEventProducer(obj, serviceInfo, orb);
            }
            if (currentEventProducer.hasNext()) {
                event = currentEventProducer.next();
            } else {
                currentEventProducer = null;
                return next();
            }
        } else {
            // all done with content, move past state 0
            event = states[++state];
            state++;
            currentEventProducer = null;
        }
        return event;
    }
}
