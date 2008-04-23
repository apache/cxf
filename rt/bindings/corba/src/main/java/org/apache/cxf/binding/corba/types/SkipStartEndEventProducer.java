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

import java.util.List;

import javax.xml.namespace.QName;
//import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

public class SkipStartEndEventProducer implements CorbaTypeEventProducer {

    private final CorbaTypeEventProducer eventProducer;
    private QName name;   
    private int peekedEvent;
    private boolean hasNext = true;

    public SkipStartEndEventProducer(CorbaTypeEventProducer contentEventProducer, QName n) {
        eventProducer = contentEventProducer;
        name = n;
        // skip start_element
        contentEventProducer.next();
        peekedEvent = contentEventProducer.next();
    }

    public String getLocalName() {
        return name.getLocalPart();
    }

    public QName getName() {
        return name;
    }

    public String getText() {    
        return eventProducer.getText();
    }

    public boolean hasNext() {
        boolean ret = hasNext;
        if (ret) {
            ret = eventProducer.hasNext();
        }
        return ret;
    }

    public int next() {
        int ret = peekedEvent;
        name = eventProducer.getName();
        peekedEvent = eventProducer.next();
        /*
        if (peekedEvent == XMLStreamReader.END_ELEMENT) {
            hasNext = false;
            peekedEvent = 0;
        }
        */
        return ret;
    }

    public List<Attribute> getAttributes() {
        return eventProducer.getAttributes();
    }

    public List<Namespace> getNamespaces() {
        return eventProducer.getNamespaces();
    }

}
