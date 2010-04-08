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
package org.apache.cxf.endpoint;

import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;

public class DummyServer implements Server {
    private ServerRegistryImpl serverRegistry;
    
    public DummyServer(ServerRegistryImpl sri) {
        serverRegistry = sri;
    }

    public Destination getDestination() {
        // TODO Auto-generated method stub
        return null;
    }

    public Endpoint getEndpoint() {
        // TODO Auto-generated method stub
        return null;
    }

    public void start() {
        serverRegistry.register(this);        
    }

    public void stop() {
        serverRegistry.unregister(this);
        
    }
    
    public void destroy() {
        stop();
    }
    
    public MessageObserver getMessageObserver() {
        return null;
    }
}
