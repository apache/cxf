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

package demo.wseventing;

import java.util.ArrayList;
import java.util.List;

import demo.wseventing.eventapi.CatastrophicEventSinkImpl;

public final class ApplicationSingleton {

    private static ApplicationSingleton instance;
    private List<CatastrophicEventSinkImpl> eventSinks = new ArrayList<CatastrophicEventSinkImpl>();

    private ApplicationSingleton() {
    }

    public static ApplicationSingleton getInstance() {
        if (instance == null) {
            instance = new ApplicationSingleton();
        }
        return instance;
    }

    public void createEventSink(String url) {
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        eventSinks.add(new CatastrophicEventSinkImpl(url));
    }

    public List<CatastrophicEventSinkImpl> getEventSinks() {
        return this.eventSinks;
    }

    public CatastrophicEventSinkImpl getEventSinkByURL(String url) {
        for (CatastrophicEventSinkImpl eventSink : eventSinks) {
            if (eventSink.getShortURL().equals(url)) {
                return eventSink;
            }
        }
        return null;
    }



}
