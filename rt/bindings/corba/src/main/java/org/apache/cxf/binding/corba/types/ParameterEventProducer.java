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

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.ORB;

public class ParameterEventProducer implements CorbaTypeEventProducer {

    protected CorbaTypeEventProducer currentEventProducer;
    protected Iterator<CorbaObjectHandler> iterator;
    private ServiceInfo serviceInfo;
    private ORB orb;

    public ParameterEventProducer(HandlerIterator paramIterator,
                                  ServiceInfo service,
                                  ORB orbRef) {
        iterator = paramIterator;
        serviceInfo = service;
        orb = orbRef;
    }

    public String getLocalName() {
        return currentEventProducer != null ? currentEventProducer.getLocalName() : null;
    }

    public QName getName() {
        return currentEventProducer != null ? currentEventProducer.getName() : null;
    }

    public String getText() {
        return currentEventProducer != null ? currentEventProducer.getText() : null;
    }

    public boolean hasNext() {
        return (currentEventProducer != null && currentEventProducer.hasNext())
            || (iterator != null && iterator.hasNext());
    }

    public int next() {
        final int event;
        if (currentEventProducer != null && currentEventProducer.hasNext()) {
            event = currentEventProducer.next();
        } else if (iterator != null && iterator.hasNext()) {
            CorbaObjectHandler obj = iterator.next();
            currentEventProducer = CorbaHandlerUtils.getTypeEventProducer(obj, serviceInfo, orb);
            event = currentEventProducer.next();
        } else {
            throw new RuntimeException("hasNext reported in error as there is no next event");
        }
        return event;
    }

    public List<Attribute> getAttributes() {
        return null;
    }

    public List<Namespace> getNamespaces() {
        return null;
    }

}
