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

package org.apache.cxf.microprofile.client.cdi;

import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.cxf.jaxrs.impl.ConfigurableImpl.Instantiator;


public final class CDIInstantiator implements Instantiator {

    static final CDIInstantiator INSTANCE = new CDIInstantiator();

    private final Map<Object, Instance<?>> cdiInstances = new IdentityHashMap<>();

    private CDIInstantiator() {
    }

    @Override
    public <T> Object create(Class<T> cls) {
        try {
            return CDIFacade.getInstanceFromCDI(cls)
                            .map(i -> {
                                Object instance = i.getValue();
                                if (instance != null) {
                                    cdiInstances.put(instance, i);
                                }
                                return instance;
                            })
                            .orElse(cls.getDeclaredConstructor().newInstance());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void release(Object instance) {
        Instance<?> cdiInstance = cdiInstances.remove(instance);
        if (cdiInstance != null) {
            cdiInstance.release();
        }
    }
}