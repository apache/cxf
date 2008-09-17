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

import javax.resource.spi.endpoint.MessageEndpoint;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.AbstractInvoker;

/**
 * An invoker that supports direct Message Driven Bean invocation.  
 * It get invoked in the context of the Message Driven Bean that 
 * activates the CXF service endpoint facade.  Applications that put 
 * service implementation inside the Message Driven Bean that activates 
 * the inbound endpoint facade should choose this invoker.  It is 
 * more straightforward and faster than {@link DispatchMBDInvoker} but 
 * it requires to modify resource adapter's deployment descriptor (ra.xml)
 * as the <messaging-type> needs to be set to the Service Endpoint Interface
 * (SEI) class.
 */
public class MDBInvoker extends AbstractInvoker {

    private MessageEndpoint messageEndpoint;

    /**
     * @param messageEndpoint
     */
    public MDBInvoker(MessageEndpoint messageEndpoint) {
        this.messageEndpoint = messageEndpoint;
    }

    /**
     * @return the messageEndpoint
     */
    public MessageEndpoint getMessageEndpoint() {
        return messageEndpoint;
    }

    @Override
    public Object getServiceObject(Exchange context) {
        return messageEndpoint;
    }

}
