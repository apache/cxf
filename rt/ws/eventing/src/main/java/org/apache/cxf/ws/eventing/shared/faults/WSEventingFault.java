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

package org.apache.cxf.ws.eventing.shared.faults;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.ws.addressing.FaultAction;
import org.apache.cxf.ws.eventing.shared.EventingConstants;

/**
 * The parent for all WS-Eventing-specific faults.
 */
@FaultAction(EventingConstants.ACTION_FAULT)
public abstract class WSEventingFault extends SoapFault {

    private static final long serialVersionUID = 1L;

    public WSEventingFault(String reason, Element detail, QName faultCode) {
        super(reason, faultCode);
        if (detail != null) {
            setDetail(detail);
        }
    }

}
