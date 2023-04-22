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
package org.apache.cxf.wsn.services;

import jakarta.jws.WebService;
import org.apache.cxf.wsn.jms.JmsPullPoint;

@WebService(endpointInterface = "org.oasis_open.docs.wsn.bw_2.PullPoint",
    targetNamespace = "http://cxf.apache.org/wsn/jaxws",
    serviceName = "PullPointService",
    portName = "PullPointPort"
)
public class JaxwsPullPoint extends JmsPullPoint {

    public JaxwsPullPoint(String name) {
        super(name);
    }

}
