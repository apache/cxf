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
package org.apache.cxf.transport.jms;

import jakarta.jms.ConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;

/**
 * Allows to configure the JMSConfiguration directly at the Client or Server. Simply add this class to the
 * Features and reference a JMSConfiguration. The configuration inside this class takes precedence over a
 * configuration that is generated from the old configuration style.
 */
@NoJSR250Annotations
public class ConnectionFactoryFeature extends DelegatingFeature<ConnectionFactoryFeature.Portable> {
    public ConnectionFactoryFeature(ConnectionFactory cf) {
        super(new Portable(cf));
    }

    public static class Portable implements AbstractPortableFeature {
        private ConnectionFactory connectionFactory;

        public Portable(ConnectionFactory cf) {
            this.connectionFactory = cf;
        }

        @Override
        public void initialize(Client client, Bus bus) {
            client.getEndpoint().getOutInterceptors().add(new JMSConduitConfigOutInterceptor());
        }
        @Override
        public void initialize(InterceptorProvider provider, Bus bus) {
            provider.getOutInterceptors().add(new JMSConduitConfigOutInterceptor());
        }

        @Override
        public void initialize(Server server, Bus bus) {
            Destination destination = server.getDestination();
            if (destination instanceof JMSDestination) {
                JMSDestination jmsDestination = (JMSDestination)destination;
                jmsDestination.getJmsConfig().setConnectionFactory(connectionFactory);
            }
        }
        private class JMSConduitConfigOutInterceptor extends AbstractPhaseInterceptor<Message> {
            JMSConduitConfigOutInterceptor() {
                super(Phase.PREPARE_SEND);
            }

            @Override
            public void handleMessage(Message message) throws Fault {
                Conduit conduit = message.getExchange().getConduit(message);
                if (conduit instanceof JMSConduit) {
                    JMSConduit jmsConduit = (JMSConduit)conduit;
                    jmsConduit.getJmsConfig().setConnectionFactory(connectionFactory);
                }
            }
        }
    }
}
