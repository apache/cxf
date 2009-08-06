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

package org.apache.cxf.service.factory;

import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.annotations.WSDLDocumentation;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.InterfaceInfo;

/**
 * 
 */
public class AnnotationsFactoryBeanListener implements FactoryBeanListener {

    /** {@inheritDoc}*/
    public void handleEvent(Event ev, AbstractServiceFactoryBean factory, Object... args) {
        switch (ev) {
        case INTERFACE_CREATED: {
            Class<?> cls = (Class<?>)args[1];
            WSDLDocumentation doc = cls.getAnnotation(WSDLDocumentation.class);
            if (doc != null) {
                InterfaceInfo info = (InterfaceInfo)args[0];
                info.setDocumentation(doc.value());
            }
            break;
        }
        case ENDPOINT_SELECTED: {
            Class<?> cls = (Class<?>)args[2];
            SchemaValidation val = cls.getAnnotation(SchemaValidation.class);
            if (val != null && val.enabled()) {
                ((Endpoint)args[1]).put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);
            }
            break;
        }
        case SERVER_CREATED: {
            Class<?> cls = (Class<?>)args[2];
            SchemaValidation val = cls.getAnnotation(SchemaValidation.class);
            if (val != null && val.enabled()) {
                ((Server)args[0]).getEndpoint().put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);
            }
            WSDLDocumentation doc = cls.getAnnotation(WSDLDocumentation.class);
            if (doc != null) {
                ((Server)args[0]).getEndpoint().getService().getServiceInfos()
                    .get(0).setDocumentation(doc.value());
            }
            break;
        }
        default:
            //do nothing
        }
    }

}
