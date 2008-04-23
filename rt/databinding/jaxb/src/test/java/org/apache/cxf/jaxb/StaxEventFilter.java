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

package org.apache.cxf.jaxb;

import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class StaxEventFilter implements EventFilter {
    private QName[] tags;

    public StaxEventFilter(QName[] eventsToReject) {
        tags = eventsToReject;
    }

    public boolean accept(XMLEvent event) {
        if (event.isStartDocument() 
            || event.isEndDocument()) {
            return false;
        }

        if (event.isStartElement()) {
            StartElement startEl = event.asStartElement();
            QName elName = startEl.getName();
            for (QName tag : tags) {
                if (elName.equals(tag)) {
                    return false;
                }
            }
        }

        if (event.isEndElement()) {
            EndElement endEl = event.asEndElement();
            QName elName = endEl.getName();
            for (QName tag : tags) {
                if (elName.equals(tag)) {
                    return false;
                }
            }
        }
        return true;
    }
}
