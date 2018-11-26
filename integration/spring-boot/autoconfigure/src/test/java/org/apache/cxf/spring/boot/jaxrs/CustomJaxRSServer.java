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
package org.apache.cxf.spring.boot.jaxrs;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.spring.AbstractJaxrsClassesScanServer;
import org.apache.cxf.transport.Destination;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomJaxRSServer extends AbstractJaxrsClassesScanServer {

    @Bean
    public Server jaxRsServer() {
        return new Server() {
            
            @Override
            public void stop() {                
            }
            
            @Override
            public void start() {                
            }
            
            @Override
            public boolean isStarted() {
                return false;
            }
            
            @Override
            public Endpoint getEndpoint() {
                return null;
            }
            
            @Override
            public Destination getDestination() {
                return null;
            }
            
            @Override
            public void destroy() {
                
            }
        };
    }

}
