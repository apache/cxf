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
import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.core.Application;

@Vetoed
class DefaultApplicationBean extends AbstractCXFBean<DefaultApplication> {
    private final DefaultApplication application = new DefaultApplication();

    @Override
    public Class<?> getBeanClass() {
        return DefaultApplication.class;
    }

    @Override
    public DefaultApplication create(CreationalContext<DefaultApplication> creationalContext) {
        return application;
    }

    @Override
    public Set<Type> getTypes() {
        final Set<Type> types = super.getTypes();
        types.add(DefaultApplication.class);
        types.add(Application.class);
        return types;
    }

    @Override
    public String getName() {
        return DefaultApplication.class.getName();
    }

}