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
package org.apache.cxf.ws.security.wss4j;

import java.security.Principal;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;

/**
 * A test interceptor to check that the Principal is not null + that the name is equal to a
 * given name.
 */
public class WSS4JPrincipalInterceptor extends AbstractPhaseInterceptor<SoapMessage> {

    private String principalName;

    public WSS4JPrincipalInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        SecurityContext context = message.get(SecurityContext.class);
        if (context == null) {
            throw new SoapFault("No Security Context", Fault.FAULT_CODE_SERVER);
        }

        Principal principal = context.getUserPrincipal();
        if (principal == null) {
            throw new SoapFault("No Security Principal", Fault.FAULT_CODE_SERVER);
        }

        if (principalName != null && !principalName.equals(principal.getName())) {
            throw new SoapFault("Security Principal does not match", Fault.FAULT_CODE_SERVER);
        }
    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }


}
