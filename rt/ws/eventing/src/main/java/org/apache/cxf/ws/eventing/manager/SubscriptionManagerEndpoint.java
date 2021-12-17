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

package org.apache.cxf.ws.eventing.manager;

import jakarta.jws.HandlerChain;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.Action;
import jakarta.xml.ws.soap.Addressing;
import org.apache.cxf.ws.eventing.GetStatus;
import org.apache.cxf.ws.eventing.GetStatusResponse;
import org.apache.cxf.ws.eventing.Renew;
import org.apache.cxf.ws.eventing.RenewResponse;
import org.apache.cxf.ws.eventing.Unsubscribe;
import org.apache.cxf.ws.eventing.UnsubscribeResponse;
import org.apache.cxf.ws.eventing.shared.EventingConstants;

/**
 * The interface definition of a Subscription Manager web service, according to the specification.
 */
@WebService(targetNamespace = EventingConstants.EVENTING_2011_03_NAMESPACE)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@Addressing(enabled = true, required = true)
@HandlerChain(file = "/subscription-reference-parsing-handler-chain.xml")
public interface SubscriptionManagerEndpoint {

    /**
     * The Renew operation of the Subscription Manager
     * See http://www.w3.org/TR/ws-eventing/#Renew
     * @param body JAXB class Renew representing the body of the renew request
     * @return JAXB class RenewResponse representing the response for the requester
     */
    @Action(
            input = EventingConstants.ACTION_RENEW,
            output = EventingConstants.ACTION_RENEW_RESPONSE
    )
    @WebResult(name = EventingConstants.RESPONSE_RENEW)
    RenewResponse renewOp(
            @WebParam(name = EventingConstants.OPERATION_RENEW,
                    targetNamespace = EventingConstants.EVENTING_2011_03_NAMESPACE, partName = "body")
            Renew body
    );

    /**
     * The GetStatus operation of the Subscription Manager
     * See http://www.w3.org/TR/ws-eventing/#GetStatus
     * @param body JAXB class GetStatus representing the body of the GetStatus request
     * @return JAXB class GetStatusResponse representing the response for the requester
     */
    @Action(
            input = EventingConstants.ACTION_GET_STATUS,
            output = EventingConstants.ACTION_GET_STATUS_RESPONSE
    )
    @WebResult(name = EventingConstants.RESPONSE_GET_STATUS)
    GetStatusResponse getStatusOp(
            @WebParam(name = EventingConstants.OPERATION_GET_STATUS,
                    targetNamespace = EventingConstants.EVENTING_2011_03_NAMESPACE, partName = "body")
            GetStatus body
    );

    /**
     * The Unsubscribe operation of the Subscription Manager
     * See http://www.w3.org/TR/ws-eventing/#Unsubscribe
     * @param body JAXB class Unsubscribe representing the body of the Unsubscribe request
     * @return JAXB class UnsubscribeResponse representing the response for the requester
     */
    @Action(
            input = EventingConstants.ACTION_UNSUBSCRIBE,
            output = EventingConstants.ACTION_UNSUBSCRIBE_RESPONSE
    )
    @WebResult(name = EventingConstants.RESPONSE_UNSUBSCRIBE)
    UnsubscribeResponse unsubscribeOp(
            @WebParam(name = EventingConstants.OPERATION_UNSUBSCRIBE,
                    targetNamespace = EventingConstants.EVENTING_2011_03_NAMESPACE, partName = "body")
            Unsubscribe body
    );

}
