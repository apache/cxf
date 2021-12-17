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

package org.apache.cxf.ws.eventing.integration.eventsink;

import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Node;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.ws.eventing.EventType;
import org.apache.cxf.ws.eventing.backend.notification.WrappedSink;
import org.apache.cxf.ws.eventing.integration.notificationapi.EarthquakeEvent;
import org.apache.cxf.ws.eventing.integration.notificationapi.FireEvent;

public class TestingWrappedEventSinkImpl implements WrappedSink {

    public static final AtomicInteger RECEIVED_EARTHQUAKES = new AtomicInteger(0);
    public static final AtomicInteger RECEIVED_FIRES = new AtomicInteger(0);

    private static JAXBContext jaxbContext;
    static {
        try {
            jaxbContext = JAXBContext.newInstance(FireEvent.class, EarthquakeEvent.class);
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    public void notifyEvent(EventType parameter) {
        if (parameter != null) {
            for (Object obj : parameter.getContent()) {
                try {
                    Object event = jaxbContext.createUnmarshaller().unmarshal((Node)obj);
                    if (event instanceof FireEvent) {
                        RECEIVED_FIRES.incrementAndGet();
                    } else if (event instanceof EarthquakeEvent) {
                        RECEIVED_EARTHQUAKES.incrementAndGet();
                    }
                } catch (JAXBException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
