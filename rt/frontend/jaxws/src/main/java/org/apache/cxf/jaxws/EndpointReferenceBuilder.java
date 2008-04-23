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

package org.apache.cxf.jaxws;

import java.util.logging.Logger;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.binding.soap.SOAPBindingImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;

public class EndpointReferenceBuilder {
    private static final Logger LOG = LogUtils.getL7dLogger(EndpointReferenceBuilder.class);
    private final JaxWsEndpointImpl endpoint;

    public EndpointReferenceBuilder(JaxWsEndpointImpl e) {
        this.endpoint = e;
    }
    public EndpointReference getEndpointReference() {
        String bindingId = endpoint.getJaxwsBinding().getBindingID();   
        
        if (!SOAPBindingImpl.isSoapBinding(bindingId)) {
            throw new UnsupportedOperationException(new Message("GET_ENDPOINTREFERENCE_UNSUPPORTED_BINDING",
                                                                LOG, bindingId).toString());
        }
        
        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        
        builder.address(this.endpoint.getEndpointInfo().getAddress());
        builder.serviceName(this.endpoint.getService().getName());
        builder.endpointName(this.endpoint.getEndpointInfo().getName());
        
        //TODO: get wsdlDocumentLocation
        //builder.wsdlDocumentLocation(endpoint.getService().getServiceInfos().toString());        
        
        return builder.build();
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
        if (clazz != W3CEndpointReference.class) {
            throw new WebServiceException("Unsupported EPR type: " + clazz);
        }
        return clazz.cast(getEndpointReference());
    }
}
