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

package org.apache.cxf.ws.eventing.backend.notification;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.eventing.EventType;
import org.apache.cxf.ws.eventing.backend.database.SubscriptionTicket;
import org.apache.cxf.ws.eventing.shared.handlers.ReferenceParametersAddingHandler;

/**
 * Represents the task to send a notification about a particular event to a particular subscribed client.
 * Dispatch is performed according to a provided endpoint interface.
 */
class EventSinkInterfaceNotificationTask implements Runnable {

    protected static final Logger LOG = LogUtils.getLogger(EventSinkInterfaceNotificationTask.class);

    SubscriptionTicket target;
    Object event;
    Class<?> endpointInterface;

    EventSinkInterfaceNotificationTask(SubscriptionTicket ticket, Object event, Class<?> endpointInterface) {
        this.target = ticket;
        this.event = event;
        this.endpointInterface = endpointInterface;

    }

    /**
     * Logic needed to actually send the notification to the subscribed client.
     */
    @Override
    public void run() {
        try {
            LOG.info("Starting notification task for subscription UUID " + target.getUuid());

            Method method = null;
            final Object proxy;
            final Object param;
            final Class<?> eventClass = event.getClass();
            final Class<?>[] eventClassArray = new Class<?>[] {eventClass};
            if (target.isWrappedDelivery()) {
                proxy = getProxy(WrappedSink.class, eventClassArray);
                param = new EventType();
                ((EventType)param).getContent().add(eventClass.isAnnotationPresent(XmlRootElement.class)
                                               ? event : convertToJAXBElement(event));
                method = WrappedSink.class.getMethod("notifyEvent", EventType.class);
            } else {
                proxy = getProxy(endpointInterface);
                // find the method to use
                Method[] methods = endpointInterface.getMethods();
                for (int i = 0; i < methods.length && method == null; i++) {
                    if (Arrays.equals(methods[i].getParameterTypes(), eventClassArray)) {
                        method = methods[i];
                    }
                }
                if (method == null) {
                    LOG.severe("Couldn't find corresponding method for event of type "
                               + eventClass.getCanonicalName() + " in event sink interface"
                               + endpointInterface.getCanonicalName());
                    return;
                }
                param = event;
            }

            method.invoke(proxy, param);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    protected JAXBElement<?> convertToJAXBElement(Object evt) {
        final Class<?> eventClass = evt.getClass();
        String tns = endpointInterface.getAnnotation(WebService.class).targetNamespace();
        return new JAXBElement(new QName(tns, eventClass.getName()), eventClass, evt);
    }

    protected Object getProxy(Class<?> sinkInterface, Class<?>... extraClasses) {
        //needed SOAP handlers
        ReferenceParametersAddingHandler handler = new
                ReferenceParametersAddingHandler(
                target.getNotificationReferenceParams());

        JaxWsProxyFactoryBean service = new JaxWsProxyFactoryBean();
        service.getOutInterceptors().add(new LoggingOutInterceptor());
        service.setServiceClass(sinkInterface);
        service.setAddress(target.getTargetURL());
        service.getHandlers().add(handler);

        // do we need to apply a filter?
        if (target.getFilter() != null && target.getFilter().getContent().size() > 0) {
            service.getOutInterceptors().add(new FilteringInterceptor(target.getFilter()));
        }

        if (extraClasses != null && extraClasses.length > 0) {
            Map<String, Object> props = new HashMap<>();
            props.put("jaxb.additionalContextClasses", extraClasses);
            service.getClientFactoryBean().getServiceFactory().setProperties(props);
        }

        return service.create();
    }

}
