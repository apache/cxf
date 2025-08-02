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
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.ORB;

public abstract class AbstractStartEndEventProducer implements
        CorbaTypeEventProducer {

    protected int state;
    protected final int[] states = {XMLStreamConstants.START_ELEMENT, 0, XMLStreamConstants.END_ELEMENT};

    protected CorbaTypeEventProducer currentEventProducer;
    protected QName name;
    protected Iterator<CorbaObjectHandler> iterator;
    protected Iterator<CorbaTypeEventProducer> producers;
    protected ServiceInfo serviceInfo;
    protected ORB orb;

    public String getLocalName() {
        return getName().getLocalPart();
    }

    public QName getName() {
        QName ret = name;
        if (currentEventProducer != null) {
            ret = currentEventProducer.getName();
        }
        return ret;
    }

    public String getText() {
        return currentEventProducer.getText();
    }

    public boolean hasNext() {
        return state < states.length;
    }

    public int next() {
        int event = states[state];
        if (event != 0) {
            state++;
        } else if (currentEventProducer != null && currentEventProducer.hasNext()) {
            event = currentEventProducer.next();
        } else if (iterator != null && iterator.hasNext()) {
            CorbaObjectHandler obj = iterator.next();
            currentEventProducer = CorbaHandlerUtils.getTypeEventProducer(obj, serviceInfo, orb);
            event = currentEventProducer.next();
        } else if (producers != null && producers.hasNext()) {
            currentEventProducer = producers.next();
            event = currentEventProducer.next();
        } else {
            // all done with content, move past state 0
            event = states[++state];
            state++;
            currentEventProducer = null;
        }
        return event;
    }

    public List<Attribute> getAttributes() {
        List<Attribute> attributes = null;
        if (currentEventProducer != null) {
            attributes = currentEventProducer.getAttributes();
        }
        return attributes;
    }

    public List<Namespace> getNamespaces() {
        List<Namespace> namespaces = null;
        if (currentEventProducer != null) {
            namespaces = currentEventProducer.getNamespaces();
        }
        return namespaces;
    }

    protected Iterator<CorbaObjectHandler> getNestedTypes() {
        return iterator;
    }
}
