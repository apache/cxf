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
package org.apache.cxf.microprofile.client;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.Configurable;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.WriterInterceptor;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.jaxrs.impl.ConfigurableImpl;
import org.apache.cxf.jaxrs.impl.ConfigurationImpl;
import org.apache.cxf.microprofile.client.cdi.CDIFacade;
import org.apache.cxf.microprofile.client.config.ConfigFacade;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class MicroProfileClientConfigurableImpl<C extends Configurable<C>>
        extends ConfigurableImpl<C>
    implements Configurable<C> {
    static final Class<?>[] CONTRACTS = new Class<?>[] {ClientRequestFilter.class,
        ClientResponseFilter.class, ReaderInterceptor.class, WriterInterceptor.class,
        MessageBodyWriter.class, MessageBodyReader.class, ResponseExceptionMapper.class};
    private static final String CONFIG_KEY_DISABLE_MAPPER = "microprofile.rest.client.disable.default.mapper";

    private final Instantiator instantiator = CDIFacade.getInstantiator().orElse(super.getInstantiator());

    public MicroProfileClientConfigurableImpl(C configurable) {
        this(configurable, null);
    }

    public MicroProfileClientConfigurableImpl(C configurable, Configuration config) {
        super(configurable, config == null ? new ConfigurationImpl(RuntimeType.CLIENT)
                        : new ConfigurationImpl(config));
    }

    boolean isDefaultExceptionMapperDisabled() {
        Object prop = getConfiguration().getProperty(CONFIG_KEY_DISABLE_MAPPER);
        if (prop != null) {
            return PropertyUtils.isTrue(prop);
        }
        return ConfigFacade.getOptionalValue(CONFIG_KEY_DISABLE_MAPPER,
                                             Boolean.class).orElse(false);
    }

    @Override
    protected Instantiator getInstantiator() {
        return instantiator;
    }
}
