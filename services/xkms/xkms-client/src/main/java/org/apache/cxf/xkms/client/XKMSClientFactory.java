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
package org.apache.cxf.xkms.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.xkms.model.extensions.ResultDetails;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

public final class XKMSClientFactory {
    private XKMSClientFactory() {
        // Util class
    }

    public static XKMSPortType create(String endpointAddress, Bus bus) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(XKMSPortType.class);
        factory.setAddress(endpointAddress);

        Map<String, Object> properties = new HashMap<>();
        properties.put("jaxb.additionalContextClasses",
                       new Class[] {ResultDetails.class});
        factory.setProperties(properties);

        return (XKMSPortType)factory.create();
    }
}
