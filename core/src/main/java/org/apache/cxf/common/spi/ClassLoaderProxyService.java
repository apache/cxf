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

package org.apache.cxf.common.spi;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;

public class ClassLoaderProxyService implements ClassLoaderService {
    private static final Logger LOG = LogUtils.getL7dLogger(ClassLoaderProxyService.class);
    private final NamespaceClassCreator srv;
    public ClassLoaderProxyService(Bus bus) {
        this(new NamespaceClassGenerator(bus));
    }
    public ClassLoaderProxyService(NamespaceClassCreator srv) {
        this.srv = srv;
    }

    @Override
    public Object createNamespaceWrapperInstance(Class<?> mcls, Map<String, String> map) {
        Class<?> cls = srv.createNamespaceWrapperClass(mcls, map);
        try {
            return cls.getConstructor(Map.class).newInstance(map);
        } catch (Throwable e) {
            LOG.warning("NamespaceWrapper not found : " + e.toString());
            return null;
        }
    }
    public class LoadFirst extends ClassLoaderProxyService {
        public LoadFirst(Bus bus) {
            super(new GeneratedNamespaceClassLoader(bus));
        }
    }
    public class GenerateJustInTime extends ClassLoaderProxyService {
        public GenerateJustInTime(Bus bus) {
            super(new NamespaceClassGenerator(bus));
        }
    }
}
