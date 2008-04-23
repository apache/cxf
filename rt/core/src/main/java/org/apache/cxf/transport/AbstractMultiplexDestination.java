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
package org.apache.cxf.transport;

import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND;

public abstract class AbstractMultiplexDestination extends AbstractDestination implements
    MultiplexDestination {

    private static final QName MULTIPLEX_ID_QNAME = new QName("http://multiplex.transport.cxf.apache.org",
                                                              "id");

    public AbstractMultiplexDestination(Bus b, EndpointReferenceType ref, EndpointInfo ei) {
        super(b, ref, ei);
    }

    /**
     * Builds an new endpoint reference using the current target reference as a template. 
     * The supplied id is endcoded using a reference parameter.
     * This requires the ws-a interceptors to propagate the reference parameters
     * on subsequent invokes using the returned reference.
     * @param the id to encode in the new reference
     * @return the new reference with the id encoded as a reference parameter
     * @see org.apache.cxf.transport.MultiplexDestination#getAddressWithId(java.lang.String)
      
     */
    public EndpointReferenceType getAddressWithId(String id) {
        EndpointReferenceType epr = EndpointReferenceUtils.duplicate(
            EndpointReferenceUtils.mint(reference, bus));
        ReferenceParametersType newParams = new org.apache.cxf.ws.addressing.ObjectFactory()
            .createReferenceParametersType();
        
        ReferenceParametersType existingParams = epr.getReferenceParameters();
        if (null != existingParams) {
            newParams.getAny().addAll(existingParams.getAny());
        }
        
        newParams.getAny().add(new JAXBElement<String>(MULTIPLEX_ID_QNAME, String.class, id));
        epr.setReferenceParameters(newParams);
        return epr;
    }

    /**
     * Obtain id from reference parameters of the ws-a to address
     * Requires the existance of ws-a interceptors on dispatch path to provide access 
     * to the ws-a headers
     * @param the current invocation or message context
     * @return the id from the reference parameters of the  ws-a-to address or null if not found
     * @see org.apache.cxf.transport.MultiplexDestination#getId(java.util.Map)
     */
    public String getId(Map contextMap) {
        String markedParam = null;
        AddressingProperties maps = (AddressingProperties)contextMap
            .get(SERVER_ADDRESSING_PROPERTIES_INBOUND);
        if (null != maps) {
            EndpointReferenceType toEpr = maps.getToEndpointReference();
            if (null != toEpr) {
                markedParam = extractStringElementFromAny(MULTIPLEX_ID_QNAME, toEpr);
            }
        }
        return markedParam;
    }

    private String extractStringElementFromAny(QName elementQName, EndpointReferenceType epr) {
        String elementStringValue = null;
        if (null != epr.getReferenceParameters()) {
            for (Object o : epr.getReferenceParameters().getAny()) {
                if (o instanceof JAXBElement) {
                    JAXBElement el = (JAXBElement)o;
                    if (el.getName().equals(elementQName)) {
                        elementStringValue = (String)el.getValue();
                    }
                }
            }
        }
        return elementStringValue;
    }
}
