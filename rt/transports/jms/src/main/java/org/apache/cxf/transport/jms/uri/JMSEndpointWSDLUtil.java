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
package org.apache.cxf.transport.jms.uri;

import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.jms.wsdl.DeliveryModeType;
import org.apache.cxf.transport.jms.wsdl.JndiConnectionFactoryNameType;
import org.apache.cxf.transport.jms.wsdl.JndiContextParameterType;
import org.apache.cxf.transport.jms.wsdl.JndiInitialContextFactoryType;
import org.apache.cxf.transport.jms.wsdl.JndiURLType;
import org.apache.cxf.transport.jms.wsdl.PriorityType;
import org.apache.cxf.transport.jms.wsdl.ReplyToNameType;
import org.apache.cxf.transport.jms.wsdl.TimeToLiveType;
import org.apache.cxf.transport.jms.wsdl.TopicReplyToNameType;

public final class JMSEndpointWSDLUtil {
    private JMSEndpointWSDLUtil() {
    }

    /**
     * Retrieve JMS spec configs from wsdl and write them to the JMSEndpoint
     * If a property is already set on the JMSEndpoint it will not be overwritten
     *
     * @param endpoint
     * @param ei
     */
    static void retrieveWSDLInformation(JMSEndpoint endpoint, EndpointInfo ei) {
        // TODO We could have more than one parameter
        JndiContextParameterType jndiContextParameterType =
            getWSDLExtensor(ei, JndiContextParameterType.class);
        if (jndiContextParameterType != null) {
            endpoint.putJndiParameter(jndiContextParameterType.getName().trim(),
                                      jndiContextParameterType.getValue().trim());
        }

        JndiConnectionFactoryNameType jndiConnectionFactoryNameType
            = getWSDLExtensor(ei, JndiConnectionFactoryNameType.class);
        if (jndiConnectionFactoryNameType != null) {
            endpoint.setJndiConnectionFactoryName(jndiConnectionFactoryNameType.getValue().trim());
        }

        JndiInitialContextFactoryType jndiInitialContextFactoryType
            = getWSDLExtensor(ei, JndiInitialContextFactoryType.class);
        if (jndiInitialContextFactoryType != null) {
            endpoint.setJndiInitialContextFactory(jndiInitialContextFactoryType.getValue().trim());
        }

        JndiURLType jndiURLType = getWSDLExtensor(ei, JndiURLType.class);
        if (jndiURLType != null) {
            endpoint.setJndiURL(jndiURLType.getValue().trim());
        }

        DeliveryModeType deliveryModeType = getWSDLExtensor(ei, DeliveryModeType.class);
        if (deliveryModeType != null) {
            String deliveryMode = deliveryModeType.getValue().trim();
            endpoint.setDeliveryMode(org.apache.cxf.transport.jms.uri.JMSEndpoint.DeliveryModeType
                                     .valueOf(deliveryMode));
        }

        PriorityType priorityType = getWSDLExtensor(ei, PriorityType.class);
        if (priorityType != null) {
            endpoint.setPriority(priorityType.getValue());
        }

        TimeToLiveType timeToLiveType = getWSDLExtensor(ei, TimeToLiveType.class);
        if (timeToLiveType != null) {
            endpoint.setTimeToLive(timeToLiveType.getValue());
        }

        ReplyToNameType replyToNameType = getWSDLExtensor(ei, ReplyToNameType.class);
        if (replyToNameType != null) {
            endpoint.setReplyToName(replyToNameType.getValue());
        }

        TopicReplyToNameType topicReplyToNameType = getWSDLExtensor(ei, TopicReplyToNameType.class);
        if (topicReplyToNameType != null) {
            endpoint.setTopicReplyToName(topicReplyToNameType.getValue());
        }
    }

    public static <T> T getWSDLExtensor(EndpointInfo ei, Class<T> cls) {
        ServiceInfo si = ei.getService();
        BindingInfo bi = ei.getBinding();

        Object o = ei.getExtensor(cls);
        if (o == null && si != null) {
            o = si.getExtensor(cls);
        }
        if (o == null && bi != null) {
            o = bi.getExtensor(cls);
        }

        if (o == null) {
            return null;
        }
        if (cls.isInstance(o)) {
            return cls.cast(o);
        }
        return null;
    }
}
