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

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

/**
 * 
 */
public final class JMSFaultFactory {

    static final Logger LOG = LogUtils.getL7dLogger(JMSFaultFactory.class);

    private JMSFaultFactory() {
    }

    public static JMSFault createUnrecognizedBindingVerionFault(String bindingVersion) {
        JMSFaultType jmsFaultType = new JMSFaultType();
        jmsFaultType.setFaultCode(SoapJMSConstants.getUnrecognizedBindingVersionQName());

        String m = new org.apache.cxf.common.i18n.Message("UNRECOGNIZED_BINDINGVERSION", LOG,
                                                          new Object[] {
                                                              bindingVersion
                                                          }).toString();
        JMSFault jmsFault = new JMSFault(m);
        jmsFault.setJmsFaultType(jmsFaultType);
        jmsFault.setSender(true);
        return jmsFault;
    }
}
