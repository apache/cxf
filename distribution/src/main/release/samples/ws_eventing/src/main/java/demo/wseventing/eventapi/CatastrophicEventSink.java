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

package demo.wseventing.eventapi;

import javax.jws.Oneway;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Action;
import javax.xml.ws.soap.Addressing;

@WebService(targetNamespace = "http://www.events.com")
@Addressing(enabled = true, required = true)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface CatastrophicEventSink {

    @Action(input = "http://www.earthquake.com")
    @Oneway
    void earthquake(@WebParam(name = "earthquake") EarthquakeEvent ev);

    @Action(input = "http://www.fire.com")
    @Oneway
    void fire(@WebParam(name = "fire") FireEvent ev);

}
