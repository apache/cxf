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

import java.util.Map;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;

import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.transport.jms.AddressType;
import org.apache.cxf.transport.jms.DestinationStyleType;
import org.apache.cxf.transport.jms.JMSNamingPropertyType;
import org.apache.cxf.wsdl.AbstractWSDLPlugin;

public class JmsTransportPlugin extends AbstractWSDLPlugin {
    public ExtensibilityElement createExtension(Map<String, Object> args) throws WSDLException {
        AddressType jmsAddress = null;

        jmsAddress = (AddressType)registry.createExtension(Port.class, ToolConstants.JMS_ADDRESS);

        String destType = "queue";
        if (optionSet(args, ToolConstants.JMS_ADDR_DEST_STYLE)) {
            destType = getOption(args, ToolConstants.JMS_ADDR_DEST_STYLE);
        }
        jmsAddress.setDestinationStyle(DestinationStyleType.fromValue(destType));

        String finitValue = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
        JMSNamingPropertyType finit = new JMSNamingPropertyType();
        finit.setName("java.naming.factory.initial");
        if (optionSet(args, ToolConstants.JMS_ADDR_INIT_CTX)) {
            finitValue = getOption(args, ToolConstants.JMS_ADDR_INIT_CTX);
        }
        finit.setValue(finitValue);

        String providerURL = "tcp://localhost:61616";
        JMSNamingPropertyType provider = new JMSNamingPropertyType();
        provider.setName("java.naming.provider.url");
        if (optionSet(args, ToolConstants.JMS_ADDR_JNDI_URL)) {
            providerURL = getOption(args, ToolConstants.JMS_ADDR_JNDI_URL);
        }
        provider.setValue(providerURL);

        String destName = "dynamicQueues/test.cxf.jmstransport.queue";
        if (optionSet(args, ToolConstants.JMS_ADDR_JNDI_DEST)) {
            destName = getOption(args, ToolConstants.JMS_ADDR_JNDI_DEST);
        }
        jmsAddress.setJndiDestinationName(destName);

        String factory = "ConnectionFactory";
        if (optionSet(args, ToolConstants.JMS_ADDR_JNDI_FAC)) {
            factory = getOption(args, ToolConstants.JMS_ADDR_JNDI_FAC);
        }
        jmsAddress.setJndiConnectionFactoryName(factory);

        //         if (optionSet(args, ToolConstants.JMS_ADDR_MSGID_TO_CORRID)) {
        //             jmsAddress.setUseMessageIDAsCorrelationID(getOption(args,
        //                                                             ToolConstants.JMS_ADDR_MSGID_TO_CORRID,
        //                                                                 Boolean.class));
        //         }

        jmsAddress.getJMSNamingProperty().add(finit);
        jmsAddress.getJMSNamingProperty().add(provider);

        //         if (optionSet(args, ToolConstants.JMS_ADDR_SUBSCRIBER_NAME)) {
        //       jmsAddress.setDurableSubscriberName(getOption(args, ToolConstants.JMS_ADDR_SUBSCRIBER_NAME));
        //         }
        return jmsAddress;
    }
}
