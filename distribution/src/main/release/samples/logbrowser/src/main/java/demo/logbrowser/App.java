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

package demo.logbrowser;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

import org.apache.cxf.jaxrs.provider.atom.AtomEntryProvider;
import org.apache.cxf.jaxrs.provider.atom.AtomFeedProvider;

import org.apache.cxf.management.web.browser.bootstrapping.BootstrapStorage;
import org.apache.cxf.management.web.browser.bootstrapping.SimpleXMLSettingsStorage;
import org.apache.cxf.management.web.logging.atom.AtomPullServer;

public class App extends Application {


    @Override
    public Set<Object> getSingletons() {
        Set<Object> classes = new HashSet<Object>();

        // The log browser
        classes.add(new BootstrapStorage(new SimpleXMLSettingsStorage()));
        classes.add(new BootstrapStorage.StaticFileProvider());
        classes.add(new BootstrapStorage.SettingsProvider());
        // this provider will have to be discovered via the class-scanning
        classes.add(new org.apache.cxf.jaxrs.ext.search.SearchContextProvider()); 

        // The pull server
        AtomPullServer aps = new AtomPullServer();
        aps.setLoggers("demo.service:DEBUG,org.apache.cxf.interceptor:INFO");
        aps.init();
        classes.add(aps);

        classes.add(new AtomFeedProvider());
        classes.add(new AtomEntryProvider());

        return classes;
    }
}
