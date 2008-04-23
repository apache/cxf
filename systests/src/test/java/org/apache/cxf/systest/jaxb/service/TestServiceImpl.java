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

package org.apache.cxf.systest.jaxb.service;

import javax.jws.WebService;

import org.apache.cxf.systest.jaxb.model.ExtendedWidget;
import org.apache.cxf.systest.jaxb.model.Widget;
/**
 * @author shade
 */
@WebService(endpointInterface = "org.apache.cxf.systest.jaxb.service.TestService")
public class TestServiceImpl implements TestService {

    public Widget getWidgetById(long id) {
        return new ExtendedWidget(id, "blah", "blah", true, true);
    }

    public void testExceptionMarshalling() throws TestServiceException {
        throw new TestServiceException("Your hovercraft is full of eels.");
    }

    

}
