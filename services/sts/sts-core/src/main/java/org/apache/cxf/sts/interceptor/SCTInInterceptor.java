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

import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.security.trust.STSUtils;

public class SCTInInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    static final Logger LOG = LogUtils.getL7dLogger(SCTInInterceptor.class);

    public SCTInInterceptor() {
        super(Phase.POST_PROTOCOL);
    }

    public void handleMessage(SoapMessage message) throws Fault {

        String s = (String)message.get(SoapBindingConstants.SOAP_ACTION);
        AddressingProperties inProps = (AddressingProperties)message
            .getContextualProperty(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND);
        if (inProps != null && s == null) {
            //MS/WCF doesn't put a soap action out for this, must check the headers
            s = inProps.getAction().getValue();
        }

        if (s != null
            && s.contains("/RST/SCT")
            && (s.startsWith(STSUtils.WST_NS_05_02)
                || s.startsWith(STSUtils.WST_NS_05_12))) {
            message.put(org.apache.cxf.ws.addressing.MAPAggregator.ACTION_VERIFIED, Boolean.TRUE);
        }
    }

}