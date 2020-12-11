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
package org.apache.cxf.jaxws.spi;

import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.WrapperClassGenerator;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.model.InterfaceInfo;

public class WrapperClassCreatorProxyService implements WrapperClassCreator {
    private final WrapperClassCreator srv;
    public WrapperClassCreatorProxyService(final Bus bus) {
        this(new WrapperClassGenerator(bus));
    }
    public WrapperClassCreatorProxyService(WrapperClassCreator srv) {
        super();
        this.srv = srv;
    }

    @Override
    public Set<Class<?>> generate(JaxWsServiceFactoryBean fact, InterfaceInfo inf, boolean q) {
        return srv.generate(fact, inf, q);
    }

    public class LoadFirst extends WrapperClassCreatorProxyService {
        public LoadFirst(Bus bus) {
            super(new WrapperClassLoader(bus));
        }
    }
    public class GenerateJustInTime extends WrapperClassCreatorProxyService {
        public GenerateJustInTime(final Bus bus) {
            super(new WrapperClassGenerator(bus));
        }
    }
}
