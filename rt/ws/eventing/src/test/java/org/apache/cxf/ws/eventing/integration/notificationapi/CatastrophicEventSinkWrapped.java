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

package org.apache.cxf.ws.eventing.integration.notificationapi;

import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.Action;
import jakarta.xml.ws.RequestWrapper;
import jakarta.xml.ws.soap.Addressing;
import org.apache.cxf.ws.eventing.shared.EventingConstants;

@WebService
@Addressing(enabled = true, required = true)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public interface CatastrophicEventSinkWrapped {

    @Action(input = "http://www.earthquake.com")
    @RequestWrapper(localName = "NotifyEvent", targetNamespace = EventingConstants.EVENTING_2011_03_NAMESPACE)
    void earthquake(@WebParam(name = "earthquake") EarthquakeEvent ev);

    @Action(input = "http://www.fire.com")
    @RequestWrapper(localName = "NotifyEvent2", targetNamespace = EventingConstants.EVENTING_2011_03_NAMESPACE)
    void fire(@WebParam(name = "fire") FireEvent ev);

}
