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

package org.apache.cxf.transport.jms.wsdl11;

import jakarta.xml.bind.JAXBException;
import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.transport.jms.wsdl.DeliveryModeType;
import org.apache.cxf.transport.jms.wsdl.JndiConnectionFactoryNameType;
import org.apache.cxf.transport.jms.wsdl.JndiContextParameterType;
import org.apache.cxf.transport.jms.wsdl.JndiInitialContextFactoryType;
import org.apache.cxf.transport.jms.wsdl.JndiURLType;
import org.apache.cxf.transport.jms.wsdl.PriorityType;
import org.apache.cxf.transport.jms.wsdl.ReplyToNameType;
import org.apache.cxf.transport.jms.wsdl.TimeToLiveType;
import org.apache.cxf.transport.jms.wsdl.TopicReplyToNameType;
import org.apache.cxf.wsdl.JAXBExtensionHelper;
import org.apache.cxf.wsdl.WSDLExtensionLoader;
import org.apache.cxf.wsdl.WSDLManager;

/**
 *
 */
@NoJSR250Annotations
public final class JMSWSDLExtensionLoader implements WSDLExtensionLoader {
    private static final Class<?>[] EXTENSORS = new Class[] {
        DeliveryModeType.class,
        JndiConnectionFactoryNameType.class,
        JndiContextParameterType.class,
        JndiInitialContextFactoryType.class,
        JndiURLType.class,
        PriorityType.class,
        ReplyToNameType.class,
        TimeToLiveType.class,
        TopicReplyToNameType.class
    };
    private final Bus bus;

    public JMSWSDLExtensionLoader(Bus b) {
        this.bus = b;
        WSDLManager manager = b.getExtension(WSDLManager.class);
        for (Class<?> extensor : EXTENSORS) {
            addExtensions(manager, javax.wsdl.Binding.class, extensor);
            addExtensions(manager, javax.wsdl.Port.class, extensor);
            addExtensions(manager, javax.wsdl.Service.class, extensor);
        }
    }

    public void addExtensions(WSDLManager manager, Class<?> parentType, Class<?> elementType) {
        try {
            JAXBExtensionHelper.addExtensions(bus, manager.getExtensionRegistry(), parentType, elementType, null,
                                              this.getClass().getClassLoader());
        } catch (JAXBException e) {
            // ignore, won't support XML
        }
    }

}
