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
package org.apache.cxf.wsn.client;

import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import org.apache.cxf.wsn.util.WSNHelper;
import org.oasis_open.docs.wsn.br_2.DestroyRegistration;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationManager;
import org.oasis_open.docs.wsn.brw_2.ResourceNotDestroyedFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

public class Registration implements Referencable {

    private final PublisherRegistrationManager registration;
    private final W3CEndpointReference epr;

    public Registration(String address) {
        this(WSNHelper.getInstance().createWSA(address));
    }

    public Registration(W3CEndpointReference epr) {
        this.registration = WSNHelper.getInstance().getPort(epr, PublisherRegistrationManager.class);
        this.epr = epr;
    }

    public PublisherRegistrationManager getRegistration() {
        return registration;
    }

    public W3CEndpointReference getEpr() {
        return epr;
    }

    public void destroy() throws ResourceUnknownFault, ResourceNotDestroyedFault {
        registration.destroyRegistration(new DestroyRegistration());
    }

}
