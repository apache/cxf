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
package org.apache.cxf.ws.eventing.backend.notification;

import jakarta.jws.Oneway;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.Action;
import jakarta.xml.ws.soap.Addressing;
import org.apache.cxf.ws.eventing.EventType;
import org.apache.cxf.ws.eventing.shared.EventingConstants;

@WebService(targetNamespace = EventingConstants.EVENTING_2011_03_NAMESPACE,
            name = EventingConstants.WRAPPED_SINK_PORT_TYPE)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@Addressing(enabled = true, required = true)
public interface WrappedSink {

    @Oneway
    @Action(input = EventingConstants.ACTION_NOTIFY_EVENT_WRAPPED_DELIVERY)
    @WebMethod(operationName = EventingConstants.OPERATION_NOTIFY_EVENT)
    void notifyEvent(
        @WebParam(partName = EventingConstants.PARAMETER, name = EventingConstants.NOTIFY,
                  targetNamespace = EventingConstants.EVENTING_2011_03_NAMESPACE)
        EventType parameter
    );
}