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

package org.apache.cxf.management.web.logging;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.search.SearchContextProvider;
import org.apache.cxf.jaxrs.provider.atom.AtomEntryProvider;
import org.apache.cxf.jaxrs.provider.atom.AtomFeedProvider;
import org.apache.cxf.management.web.browser.bootstrapping.BootstrapStorage;
import org.apache.cxf.management.web.browser.bootstrapping.SimpleXMLSettingsStorage;
import org.apache.cxf.management.web.logging.atom.AtomPullServer;

@Provider
public class MockApp extends Application {
    private static final AtomPullServer LOGS;

    static {
        LOGS = new AtomPullServer();
        LOGS.setLogger("org.apache.cxf.management.web.logging.Generate");
        LOGS.init();
    }

    private static final AtomFeedProvider FEED = new AtomFeedProvider();
    private static final AtomEntryProvider ENTRY = new AtomEntryProvider();

    private static final BootstrapStorage BOOTSTRAP_STORAGE =
        new BootstrapStorage(new SimpleXMLSettingsStorage());

    private static final BootstrapStorage.SettingsProvider SETTINGS =
        new BootstrapStorage.SettingsProvider();
    
    @Override
    public Set<Class<?>> getClasses() {        
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(AtomPullServer.class);
        classes.add(AtomFeedProvider.class);
        classes.add(AtomEntryProvider.class);
        classes.add(BootstrapStorage.class);
        classes.add(BootstrapStorage.SettingsProvider.class);
        classes.add(SearchContextProvider.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> classes = new HashSet<Object>();
        classes.add(LOGS);
        classes.add(FEED);
        classes.add(ENTRY);
        classes.add(BOOTSTRAP_STORAGE);
        classes.add(SETTINGS);
        return classes;
    }
}
