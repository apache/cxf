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

package org.apache.cxf.ws.eventing.eventsource;

import java.io.IOException;

import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.Action;
import jakarta.xml.ws.soap.Addressing;
import org.apache.cxf.ws.eventing.Subscribe;
import org.apache.cxf.ws.eventing.SubscribeResponse;
import org.apache.cxf.ws.eventing.shared.EventingConstants;


/**
 * The interface definition of an Event Source web service, according to the specification.
 * See http://www.w3.org/TR/ws-eventing/#Subscribe
 */
@WebService(targetNamespace = EventingConstants.EVENTING_2011_03_NAMESPACE)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@Addressing(enabled = true, required = true)
public interface EventSourceEndpoint {


    /**
     * The Subscribe operation of the Event Source.
     * See http://www.w3.org/TR/ws-eventing/#Subscribe
     * @param body JAXB class Subscribe representing the body of the subscription request
     * @return JAXB class SubscribeResponse representing the response for the requester
     */
    @Action(
            input = EventingConstants.ACTION_SUBSCRIBE,
            output = EventingConstants.ACTION_SUBSCRIBE_RESPONSE
    )
    @WebResult(name = EventingConstants.RESPONSE_SUBSCRIBE)
    SubscribeResponse subscribeOp(
            @WebParam(name = EventingConstants.OPERATION_SUBSCRIBE,
                    targetNamespace = EventingConstants.EVENTING_2011_03_NAMESPACE, partName = "body")
            Subscribe body) throws IOException;


}
