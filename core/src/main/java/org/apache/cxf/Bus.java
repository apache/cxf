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

package org.apache.cxf;

import java.util.Collection;
import java.util.Map;

import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.InterceptorProvider;

/**
 * The Bus is the central place in CXF. Its primary responsibility is
 * providing access to the different extensions (such as the DestinationFactoryManager,
 * ConduitFactoryManager, BindingFactoryManager, etc). Depending on the implementation
 * of the Bus it may also be responsible for wiring up the CXF internals.
 */
public interface Bus extends InterceptorProvider {
    enum BusState {
        INITIAL, INITIALIZING, RUNNING, SHUTTING_DOWN, SHUTDOWN;
    }

    String DEFAULT_BUS_ID = "cxf";

    <T> T getExtension(Class<T> extensionType);

    <T> void setExtension(T extension, Class<T> extensionType);

    boolean hasExtensionByName(String name);

    String getId();
    void setId(String i);

    void shutdown(boolean wait);

    void setProperty(String s, Object o);
    Object getProperty(String s);
    void setProperties(Map<String, Object> properties);
    Map<String, Object> getProperties();

    Collection<Feature> getFeatures();
    void setFeatures(Collection<? extends Feature> features);

    BusState getState();

}
