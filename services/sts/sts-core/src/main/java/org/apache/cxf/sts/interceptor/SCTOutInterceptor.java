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

package org.apache.cxf.sts.interceptor;

import java.util.logging.Logger;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;

public class SCTOutInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    static final Logger LOG = LogUtils.getL7dLogger(SCTOutInterceptor.class);

    public SCTOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    public void handleMessage(SoapMessage message) throws Fault {

        AddressingProperties inProps = (AddressingProperties)message
            .getContextualProperty(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND);
        AddressingProperties outProps = (AddressingProperties)message
            .getContextualProperty(JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND);
        if (inProps != null) {
            if (outProps == null) {
                outProps = new AddressingProperties(inProps.getNamespaceURI());
            }
            AttributedURIType action = new AttributedURIType();
            action.setValue(inProps.getAction().getValue().replace("/RST/", "/RSTR/"));
            outProps.setAction(action);
            message.put(JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND, outProps);
        }
    }

}