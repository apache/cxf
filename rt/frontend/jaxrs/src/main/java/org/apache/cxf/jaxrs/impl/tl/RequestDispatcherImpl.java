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
package org.apache.cxf.jaxrs.impl.tl;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

class RequestDispatcherImpl implements RequestDispatcher {
    private RequestDispatcher rd;

    RequestDispatcherImpl(RequestDispatcher rd) {
        this.rd = rd;
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException,
        IOException {
        rd.forward(request, response);
        JAXRSUtils.getCurrentMessage().getExchange().put("http.request.redirected", Boolean.TRUE);
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException,
        IOException {
        rd.include(request, response);
    }
}
