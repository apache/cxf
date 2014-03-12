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

import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.apache.cxf.transport.websocket.WebSocketServletHolder;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.websocket.WebSocket;
import org.atmosphere.websocket.WebSocketProtocolStream;

/**
 * 
 */
public class AtmosphereWebSocketStreamHandler extends AtmosphereWebSocketHandler implements 
    WebSocketProtocolStream {
    private static final Logger LOG = LogUtils.getL7dLogger(AtmosphereWebSocketStreamHandler.class);

    @Override
    public List<AtmosphereRequest> onTextStream(WebSocket webSocket, Reader r) {
        LOG.info("onTextStream(WebSocket, Reader)");
        //TODO add support for Reader
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public List<AtmosphereRequest> onBinaryStream(WebSocket webSocket, InputStream stream) {
        LOG.info("onBinaryStream(WebSocket, InputStream)");
        
        try {
            WebSocketServletHolder webSocketHolder = new AtmosphereWebSocketServletHolder(webSocket);
            HttpServletRequest request = createServletRequest(webSocketHolder, stream);
            HttpServletResponse response = createServletResponse(webSocketHolder);
            if (destination != null) {
                ((WebSocketDestinationService)destination).invokeInternal(null, 
                    webSocket.resource().getRequest().getServletContext(),
                    request, response);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to invoke service", e);
        }
        return null;
    }



}
