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

package demo.wseventing.eventapi;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

public class CatastrophicEventSinkImpl implements CatastrophicEventSink {

    private String url;

    private Server server;

    private List<Object> receivedEvents = new ArrayList<Object>();

    public CatastrophicEventSinkImpl(String url) {
        JaxWsServerFactoryBean bean = new JaxWsServerFactoryBean();
        bean.setServiceBean(this);
        bean.setAddress(url);
        this.url = url;
        server = bean.create();
    }

    @Override
    public void earthquake(EarthquakeEvent ev) {
        System.out.println("Event sink received an earthquake notification: " + ev.toString());
        receivedEvents.add(ev);
    }

    @Override
    public void fire(FireEvent ev) {
        System.out.println("Event sink received an fire notification: " + ev.toString());
        receivedEvents.add(ev);
    }

    public void stop() {
        server.stop();
    }

    public boolean isRunning() {
        return server.isStarted();
    }

    public String getFullURL() {
        return "services" + url;
    }

    public String getShortURL() {
        return url;
    }

    public List<Object> getReceivedEvents() {
        return receivedEvents;
    }

}
