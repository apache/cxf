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

import java.util.List;

import javax.xml.namespace.QName;


/**
 * Caches all events that do not have a listener associated with them.
 * The events will be stored until the cache limit is reached.
 * After reaching the cache size, events will be discarded using first in,
 * first out semantics.
 */
public interface EventCache {
    /**
     * Add the <code>Event</code> to the cache.
     * If the maximum size of the cache is reached, the first <code>Event</code>
     * added will be removed from the cache(FIFO)
     * @param e The <code>Event</code> to be added to the cache.
     */
    void addEvent(Event e);


    /**
     * Flushes the cache of all the <code>Event</code>s.
     * @return List Containing the cached <code>Event</code>s.
     */
    List<Event> flushEvents();


    /**
     * Flushes the <code>Event</code> from the cache matching the event type.
     * @param eventType the <code>Event</code> type.
     * @return List the list of <code>Event</code>s matching the event type. 
     */
    List<Event> flushEvents(QName eventType);


    /**
     * Flushes the <code>Event</code>s from the cache matching the event type namespace.
     * @param namespaceURI the <code>Event</code> type namespace.
     * @return List the list of <code>Event</code>s matching the event type namespace.
     */
    List<Event> flushEvents(String namespaceURI);

    /**
     * Returns all the events. This method doesn't remove the
     * events from the cache.
     * @return List the list of all events stored in the cache.
     */
    List<Event> getEvents();


    /**
     * Returns all the events matching the event type. This method doesn't
     * remove the events from the cache.
     * @param eventType the <code>Event</code> type.
     * @return the list of <code>Event</code>s matching the event type.
     */
    List<Event> getEvents(QName eventType);

    /**
     * Returns all the events matching the event type namespace. This method doesn't
     * remove the events from the cache.
     * @param namespaceURI the <code>Event</code> type namespace.
     * @return the list of <code>Event</code>s matching the event type namespace.
     */
    List<Event> getEvents(String namespaceURI);


    /**
     * Sets the cache size. This method can be used to dynamically change the
     * cache size from the configured size.
     * @param size Indicates the new size of the cache.
     */
    void setCacheSize(int size);
}
