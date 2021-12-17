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

import org.w3c.dom.Element;

import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import org.apache.cxf.wsn.util.WSNHelper;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.bw_2.NotificationConsumer;

@WebService(endpointInterface = "org.oasis_open.docs.wsn.bw_2.NotificationConsumer",
    targetNamespace = "http://cxf.apache.org/wsn/jaxws",
    serviceName = "NotificationConsumerService",
    portName = "NotificationConsumerPort"
)
public class Consumer implements NotificationConsumer, Referencable {

    public interface Callback {
        void notify(NotificationMessageHolderType message);
    }

    private final Callback callback;
    private final Endpoint endpoint;
    private final JAXBContext context;

    public Consumer(Callback callback, String address, Class<?> ... extraClasses) {
        this.callback = callback;
        WSNHelper helper = WSNHelper.getInstance();
        if (helper.supportsExtraClasses()) {
            this.endpoint = helper.publish(address, this, extraClasses);
            this.context = null;
        } else {
            this.endpoint = helper.publish(address, this);
            if (extraClasses != null && extraClasses.length > 0) {
                try {
                    this.context = JAXBContext.newInstance(extraClasses);
                } catch (JAXBException e) {
                    throw new RuntimeException(e);
                }
            } else {
                this.context = null;
            }
        }
    }

    public void stop() {
        this.endpoint.stop();
    }

    public W3CEndpointReference getEpr() {
        return this.endpoint.getEndpointReference(W3CEndpointReference.class);
    }

    public void notify(
        @WebParam(partName = "Notify",
                  name = "Notify",
                  targetNamespace = "http://docs.oasis-open.org/wsn/b-2") Notify notify) {
        for (NotificationMessageHolderType message : notify.getNotificationMessage()) {
            if (context != null) {
                Object o = message.getMessage().getAny();
                if (o instanceof Element) {
                    try {
                        o = context.createUnmarshaller().unmarshal((Element)o);
                        message.getMessage().setAny(o);
                    } catch (JAXBException e) {
                        //ignore, leave as a DOM
                    }
                }
            }
            this.callback.notify(message);
        }
    }
}
