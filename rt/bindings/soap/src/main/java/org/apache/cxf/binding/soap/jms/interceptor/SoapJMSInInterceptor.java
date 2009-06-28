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

package org.apache.cxf.binding.soap.jms.interceptor;

import java.util.List;
import java.util.Map;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

/**
 * 
 */
public class SoapJMSInInterceptor extends AbstractSoapInterceptor {

    public SoapJMSInInterceptor() {
        super(Phase.RECEIVE);
        addAfter(AttachmentInInterceptor.class.getName());
    }

    public void handleMessage(SoapMessage message) throws Fault {
        Map<String, List<String>> headers = CastUtils.cast((Map)message
            .get(Message.PROTOCOL_HEADERS));
        if (headers != null) {
            checkBindingVersion(message, headers);
        }
    }

    /**
     * @param message 
     * @param headers
     */
    private void checkBindingVersion(SoapMessage message, Map<String, List<String>> headers) {
        List<String> bv = headers.get(SoapJMSConstants.BINDINGVERSION);
        if (bv != null && bv.size() > 0) {
            String bindingVersion = bv.get(0);
            if (!"1.0".equals(bindingVersion)) {
                JMSFault jmsFault = JMSFaultFactory
                    .createUnrecognizedBindingVerionFault(bindingVersion);
                Endpoint e = message.getExchange().get(Endpoint.class);
                Binding b = null;
                if (null != e) {
                    b = e.getBinding();
                }
                if (null != b) {
                    SoapFaultFactory sff = new SoapFaultFactory(b);
                    Fault f = sff.createFault(jmsFault);
                    throw f;
                }
            }
        }
    }
}
