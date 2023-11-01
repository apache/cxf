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
package org.apache.cxf.cdi;

import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.common.util.ClassUnwrapper;
import org.apache.cxf.common.util.SystemPropertyAction;

final class CdiBusBean extends AbstractCXFBean< ExtensionManagerBus > {
    static final String CXF = "cxf";

    private final InjectionTarget<ExtensionManagerBus> injectionTarget;

    CdiBusBean(final InjectionTargetFactory<ExtensionManagerBus> injectionTargetFactory) {
        this.injectionTarget = injectionTargetFactory.createInjectionTarget(this);
    }

    @Override
    public Class< ? > getBeanClass() {
        return Bus.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return injectionTarget.getInjectionPoints();
    }

    @Override
    public String getName() {
        return CXF;
    }

    @Override
    public Set<Type> getTypes() {
        final Set< Type > types = super.getTypes();
        types.add(Bus.class);
        return types;
    }

    @Override
    public ExtensionManagerBus create(final CreationalContext< ExtensionManagerBus > ctx) {
        final ExtensionManagerBus instance = injectionTarget.produce(ctx);
        if ("true".equals(SystemPropertyAction.getProperty("org.apache.cxf.cdi.unwrap.proxies", "true"))) {
            instance.setProperty(ClassUnwrapper.class.getName(), new CdiClassUnwrapper());
        }
        BusFactory.possiblySetDefaultBus(instance);
        instance.initialize();

        injectionTarget.inject(instance, ctx);
        injectionTarget.postConstruct(instance);
        return instance;
    }

    @Override
    public void destroy(final ExtensionManagerBus instance,
            final CreationalContext< ExtensionManagerBus > ctx) {
        injectionTarget.preDestroy(instance);
        injectionTarget.dispose(instance);
        instance.shutdown();
        ctx.release();
    }
}
