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
package org.apache.cxf.ws.rm.policy;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.RMCaptureInInterceptor;
import org.apache.cxf.ws.rm.RMCaptureOutInterceptor;
import org.apache.cxf.ws.rm.RMDeliveryInterceptor;
import org.apache.cxf.ws.rm.RMInInterceptor;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMOutInterceptor;
import org.apache.cxf.ws.rm.soap.RMSoapInInterceptor;
import org.apache.cxf.ws.rm.soap.RMSoapOutInterceptor;

public class RMPolicyInterceptorProvider extends AbstractPolicyInterceptorProvider {

    private static final long serialVersionUID = -9134254448692671780L;
    private static final Collection<QName> ASSERTION_TYPES;
    private RMInInterceptor rmIn = new RMInInterceptor();
    private RMOutInterceptor rmOut = new RMOutInterceptor();
    private RMCaptureInInterceptor rmCaptureIn = new RMCaptureInInterceptor();
    private RMCaptureOutInterceptor rmCaptureOut = new RMCaptureOutInterceptor();
    private RMSoapOutInterceptor rmOutSoap = new RMSoapOutInterceptor();
    private RMSoapInInterceptor rmInSoap = new RMSoapInInterceptor();
    private RMDeliveryInterceptor rmDelivery = new RMDeliveryInterceptor();

    static {
        Collection<QName> types = new ArrayList<>();
        types.add(RM10Constants.WSRMP_RMASSERTION_QNAME);
        types.add(RM11Constants.WSRMP_RMASSERTION_QNAME);
        types.add(MC11AssertionBuilder.MCSUPPORTED_QNAME);
        types.add(RSPAssertionBuilder.CONFORMANT_QNAME);
        ASSERTION_TYPES = types;
    }

    public RMPolicyInterceptorProvider(Bus bus) {
        super(ASSERTION_TYPES);
        rmIn.setBus(bus);
        rmOut.setBus(bus);
        rmCaptureIn.setBus(bus);
        rmCaptureOut.setBus(bus);
        rmDelivery.setBus(bus);

        getInInterceptors().add(rmIn);
        getInInterceptors().add(rmInSoap);
        getInInterceptors().add(rmDelivery);
        RMManager manager = bus.getExtension(RMManager.class);
        if (null != manager && null != manager.getStore()) {
            getInInterceptors().add(rmCaptureIn);
        }

        getOutInterceptors().add(rmOut);
        getOutInterceptors().add(rmCaptureOut);
        getOutInterceptors().add(rmOutSoap);

        getInFaultInterceptors().add(rmIn);
        getInFaultInterceptors().add(rmInSoap);
        getInFaultInterceptors().add(rmDelivery);

        getOutFaultInterceptors().add(rmOut);
        getOutFaultInterceptors().add(rmCaptureOut);
        getOutFaultInterceptors().add(rmOutSoap);
    }
}
