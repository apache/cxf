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
package org.apache.cxf.endpoint.dynamic;

import org.apache.cxf.Bus;

public class ExceptionClassCreatorProxyService implements ExceptionClassCreator {
    private final ExceptionClassCreator srv;
    public ExceptionClassCreatorProxyService(Bus bus) {
        this(new ExceptionClassGenerator(bus));
    }
    public ExceptionClassCreatorProxyService(ExceptionClassCreator srv) {
        super();
        this.srv = srv;
    }

    @Override
    public Class<?> createExceptionClass(Class<?> bean) {
        return srv.createExceptionClass(bean);
    }

    public class LoadFirst extends ExceptionClassCreatorProxyService {
        public LoadFirst(Bus bus) {
            super(new ExceptionClassLoader(bus));
        }
    }
    public class GenerateJustInTime extends ExceptionClassCreatorProxyService {
        public GenerateJustInTime(Bus bus) {
            super(new ExceptionClassGenerator(bus));
        }
    }
}