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

package org.apache.cxf.transport.http.netty.server;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.transport.http.HttpUrlUtil;


public class NettyHttpTestHandler extends NettyHttpHandler {
    private boolean contextMatchExact;
    private String response;

    public NettyHttpTestHandler(String s, boolean cmExact) {
        super(null, cmExact);
        contextMatchExact = cmExact;
        response = s;
    }

    @Override
    public void handle(String target,
                       HttpServletRequest request,
                       HttpServletResponse resp) throws IOException, ServletException {

        if (contextMatchExact) {
            // just return the response for testing
            resp.getOutputStream().write(response.getBytes());
            resp.flushBuffer();

        } else {
            if (target.equals(getName()) || HttpUrlUtil.checkContextPath(getName(), target)) {
                resp.getOutputStream().write(response.getBytes());
                resp.flushBuffer();
            }
        }
    }



}
