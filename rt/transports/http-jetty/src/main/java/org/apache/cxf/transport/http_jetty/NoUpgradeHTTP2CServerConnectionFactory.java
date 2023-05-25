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
package org.apache.cxf.transport.http_jetty;

import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;

class NoUpgradeHTTP2CServerConnectionFactory extends HTTP2CServerConnectionFactory {
    NoUpgradeHTTP2CServerConnectionFactory(HttpConfiguration httpConfiguration) {
        super(httpConfiguration);
    }
    @Override
    public Connection upgradeConnection(Connector c, EndPoint endPoint,
                                        org.eclipse.jetty.http.MetaData.Request request,
                                        org.eclipse.jetty.http.HttpFields.Mutable response101)
        throws BadMessageException {
        if (request.getContentLength() > 0 
            || request.getFields().contains("Transfer-Encoding")) {
            // if there is a body, we cannot upgrade
            return null;
        }
        return super.upgradeConnection(c, endPoint, request, response101);
    }
}