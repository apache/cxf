/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cxf.wsn.client;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.bw_2.NotificationConsumer;

@WebService(endpointInterface = "org.oasis_open.docs.wsn.bw_2.NotificationConsumer")
public class Consumer implements NotificationConsumer, Referencable {

    public interface Callback {
        void notify(NotificationMessageHolderType message);
    }

    private final Callback callback;
    private final Endpoint endpoint;

    public Consumer(Callback callback, String address) {
        this.callback = callback;
        this.endpoint = Endpoint.create(this);
        this.endpoint.publish(address);
    }

    public void stop() {
        this.endpoint.stop();
    }

    public W3CEndpointReference getEpr() {
        return this.endpoint.getEndpointReference(W3CEndpointReference.class);
    }

    public void notify(@WebParam(partName = "Notify", name = "Notify", targetNamespace = "http://docs.oasis-open.org/wsn/b-2") Notify notify) {
        for (NotificationMessageHolderType message : notify.getNotificationMessage()) {
            this.callback.notify(message);
        }
    }
}
