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

package org.apache.cxf.transport.websocket.atmosphere;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.util.Utils;
import org.atmosphere.websocket.WebSocketProtocol;

/**
 * 
 */
public class AtmosphereWebSocketServletDestination extends ServletDestination implements
    WebSocketDestinationService {
    private AtmosphereFramework framework;

    public AtmosphereWebSocketServletDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei, 
                                                 String path) throws IOException {
        super(bus, registry, ei, ei.toString());
        this.framework = new AtmosphereFramework(false, true);

        framework.setUseNativeImplementation(false);
        framework.addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true");
        //TODO provide a way to switch between the non-stream handler and the stream handler
        framework.addInitParameter(ApplicationConfig.WEBSOCKET_PROTOCOL, 
                                   AtmosphereWebSocketHandler.class.getName());
        framework.init();

        WebSocketProtocol wsp = framework.getWebSocketProtocol();
        if (wsp instanceof AtmosphereWebSocketHandler) {
            ((AtmosphereWebSocketHandler)wsp).setDestination(this);
        }
    }

    @Override
    public void invoke(ServletConfig config, ServletContext context, HttpServletRequest req,
                       HttpServletResponse resp) throws IOException {
        if (Utils.webSocketEnabled(req)) {
            try {
                framework.doCometSupport(AtmosphereRequest.wrap(req), AtmosphereResponse.wrap(resp));
            } catch (ServletException e) {
                throw new IOException(e);
            }
            return;
        }
        super.invoke(config, context, req, resp);
    }

    @Override
    public void invokeInternal(ServletConfig config, ServletContext context, HttpServletRequest req,
                               HttpServletResponse resp) throws IOException {
        super.invoke(config, context, req, resp);
    }

}
