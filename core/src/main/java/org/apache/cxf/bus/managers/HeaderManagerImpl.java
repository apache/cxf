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
package org.apache.cxf.bus.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.headers.HeaderManager;
import org.apache.cxf.headers.HeaderProcessor;

@NoJSR250Annotations(unlessNull = "bus")
public class HeaderManagerImpl implements HeaderManager {
    Map<String, HeaderProcessor> processors = new ConcurrentHashMap<>(4, 0.75f, 2);
    Bus bus;

    public HeaderManagerImpl() {
    }
    public HeaderManagerImpl(Bus b) {
        setBus(b);
    }

    public Bus getBus() {
        return bus;
    }

    @Resource
    public final void setBus(Bus bus) {
        this.bus = bus;
        if (null != bus) {
            bus.setExtension(this, HeaderManager.class);
        }
    }

    public HeaderProcessor getHeaderProcessor(String namespace) {
        return processors.get(namespace != null ? namespace : "");
    }

    public void registerHeaderProcessor(HeaderProcessor processor) {
        processors.put(processor.getNamespace(), processor);
    }

}
