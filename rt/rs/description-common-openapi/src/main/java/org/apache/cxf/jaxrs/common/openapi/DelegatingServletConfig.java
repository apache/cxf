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
package org.apache.cxf.jaxrs.common.openapi;

import java.util.Enumeration;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

public class DelegatingServletConfig implements ServletConfig {
    private final ServletConfig delegate;

    public DelegatingServletConfig(final ServletConfig sc) {
        this.delegate = sc;
    }

    @Override
    public String getServletName() {
        return delegate.getServletName();
    }

    @Override
    public ServletContext getServletContext() {
        return delegate.getServletContext();
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return delegate.getInitParameterNames();
    }

    @Override
    public String getInitParameter(String name) {
        return delegate.getInitParameter(name);
    }
}
