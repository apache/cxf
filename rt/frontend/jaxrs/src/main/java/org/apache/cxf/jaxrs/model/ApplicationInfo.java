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
package org.apache.cxf.jaxrs.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Application;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;

public class ApplicationInfo extends ProviderInfo<Application> {
    private Map<String, Object> overridingProps = Collections.emptyMap();
    public ApplicationInfo(Application provider, Bus bus) {
        this(provider, null, bus);
    }
    public ApplicationInfo(Application provider,
                        Map<Class<?>, ThreadLocalProxy<?>> constructorProxies,
                        Bus bus) {
        super(provider, constructorProxies, bus, true);
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> appProps = super.getProvider().getProperties();
        if (overridingProps.isEmpty()) {
            return appProps;
        }
        Map<String, Object> props = new HashMap<>(appProps);
        props.putAll(overridingProps);
        return props;
    }
    public void setOverridingProps(Map<String, Object> overridingProps) {
        this.overridingProps = overridingProps;
    }
}
