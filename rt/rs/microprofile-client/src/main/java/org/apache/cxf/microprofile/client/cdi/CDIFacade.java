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

import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.impl.ConfigurableImpl.Instantiator;


public final class CDIFacade {

    private static final boolean CDI_AVAILABLE;

    private CDIFacade() {
    }

    static {
        boolean b;
        try {
            Class.forName("jakarta.enterprise.inject.spi.BeanManager");
            b = true;
        } catch (Throwable t) {
            b = false;
        }
        CDI_AVAILABLE = b;
    }

    public static Optional<Object> getBeanManager(Bus b) {
        return nullableOptional(() -> CDIUtils.getCurrentBeanManager(b));
    }

    public static Optional<Object> getBeanManager() {
        try {
            return nullableOptional(() -> CDIUtils.getCurrentBeanManager());
        } catch (Throwable t) {
            t.printStackTrace();
            return Optional.ofNullable(null);
        }
    }

    public static <T> Optional<Instance<T>> getInstanceFromCDI(Class<T> clazz, Bus b) {
        return nullableOptional(() -> CDIUtils.getInstanceFromCDI(clazz, b));
    }

    public static <T> Optional<Instance<T>> getInstanceFromCDI(Class<T> clazz) {
        return nullableOptional(() -> CDIUtils.getInstanceFromCDI(clazz));
    }

    public static Optional<Instantiator> getInstantiator() {
        return Optional.ofNullable(CDI_AVAILABLE ? CDIInstantiator.INSTANCE : null);
    }

    private static <T> Optional<T> nullableOptional(Callable<T> callable) {
        if (!CDI_AVAILABLE) {
            return Optional.empty();
        }

        T t;
        try {
            t = callable.call();
        } catch (Throwable ex) {
            // expected if no CDI implementation is available
            t = null;
        }
        return Optional.ofNullable(t);
    }
}