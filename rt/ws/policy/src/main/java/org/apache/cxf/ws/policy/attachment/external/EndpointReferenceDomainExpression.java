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

package org.apache.cxf.ws.policy.attachment.external;

import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * 
 */
public class EndpointReferenceDomainExpression implements DomainExpression {

    private EndpointReferenceType epr;    
    
    public EndpointReferenceType getEndpointReference() {
        return epr;
    }

    public void setEndpointReference(EndpointReferenceType e) {
        epr = e;
    }

    public boolean appliesTo(BindingFaultInfo bfi) {
        return false;
    }

    public boolean appliesTo(BindingMessageInfo bmi) {
        return false;
    }

    public boolean appliesTo(BindingOperationInfo boi) {
        return false;
    }

    public boolean appliesTo(EndpointInfo ei) {
        // TODO what if no address is specified for the EndpointInfo object ...
        return epr.getAddress().getValue().equals(ei.getAddress());
    }

    public boolean appliesTo(ServiceInfo si) {
        return false;
    }

}
