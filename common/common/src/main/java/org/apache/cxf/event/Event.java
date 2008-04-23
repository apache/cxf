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

package org.apache.cxf.event;

import java.util.EventObject;

import javax.xml.namespace.QName;


/**
 * Base class for all the CXF Events.
 */
public class Event extends EventObject {

    
    /*public static final String BUS_EVENT = "org.apache.cxf.bus.event";
    public static final String COMPONENT_CREATED_EVENT = "COMPONENT_CREATED_EVENT";
    public static final String COMPONENT_REMOVED_EVENT = "COMPONENT_REMOVED_EVENT";*/
    
    
    private QName eventId;

    /**
     * Constructs a <code>Event</code> with the event source and a unique event id.
     * This id is used to identify the event type.
     * @param source The <code>Object</code> representing the event information.
     * @param id the QName identifying the event type
     */
    public Event(Object source, QName id) {
        super(source);
        eventId = id;
    }

    /**
     * Returns the unique event id for this particular bus event.
     * @return String The event id.
     */
    public QName getID() {
        return eventId;
    }
}
