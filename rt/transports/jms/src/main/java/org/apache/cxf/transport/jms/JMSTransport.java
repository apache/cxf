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

import javax.jms.Destination;

/**
 * Common accessors between the conduit and destination which are needed for common code.
 *
 */
public interface JMSTransport {

    AddressType getJMSAddress();

    SessionPoolType getSessionPool();

    /**
     * Callback from the JMSProviderHub indicating the ClientTransport has
     * been sucessfully connected.
     *
     * @param targetDestination the target destination
     * @param sessionFactory used to get access to a pooled JMS resources
     */
    void connected(Destination target, Destination reply, JMSSessionFactory factory);
}
