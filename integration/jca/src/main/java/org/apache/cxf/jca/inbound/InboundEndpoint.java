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
package org.apache.cxf.jca.inbound;


import org.apache.cxf.endpoint.Server;

/**
 * An inbound endpoint is a CXF service endpoint facade exposed by
 * the JCA connector.  Its role is to accept service requests from 
 * ordinary CXF clients and forward them to an invoker (running in 
 * the context of the activating message driven bean).  The invoker
 * either contains the service implementation or dispatches the call
 * to a Stateless Session Bean.  This class holds objects that are 
 * needed to accomplish the task and provides a shutdown method to 
 * clean up the endpoint. 
 * 
 */
public class InboundEndpoint {
    
    private Server server;
    private MDBInvoker invoker;
    
    /**
     * @param server the server object that has already been started
     * @param invoker the invoker that invoker an EJB
     */
    InboundEndpoint(Server server, MDBInvoker invoker) {
        this.server = server;
        this.invoker = invoker;
    }
    
    /**
     * @return the invoker
     */
    public MDBInvoker getInvoker() {
        return invoker;
    }

    /**
     * @return the server
     */
    public Server getServer() {
        return server;
    }


    /**
     * @param invoker the invoker to set
     */
    public void setInvoker(MDBInvoker invoker) {
        this.invoker = invoker;
    }


    /**
     * @param server the server to set
     */
    public void setServer(Server server) {
        this.server = server;
    }


    /**
     * Shuts down the endpoint
     * 
     * @throws Exception
     */
    public void shutdown() throws Exception {
        invoker = null;
        
        if (server != null) {
            server.destroy();
            server = null;
        }     
    }
   
}
