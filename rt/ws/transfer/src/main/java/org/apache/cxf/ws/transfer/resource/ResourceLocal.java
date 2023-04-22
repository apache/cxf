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

package org.apache.cxf.ws.transfer.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Delete;
import org.apache.cxf.ws.transfer.DeleteResponse;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.GetResponse;
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.PutResponse;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.Dialect;
import org.apache.cxf.ws.transfer.dialect.fragment.FragmentDialect;
import org.apache.cxf.ws.transfer.dialect.fragment.FragmentDialectConstants;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.shared.faults.UnknownDialect;
import org.apache.cxf.ws.transfer.validationtransformation.ResourceTypeIdentifier;
import org.apache.cxf.ws.transfer.validationtransformation.ValidAndTransformHelper;

/**
 * Implementation of the Resource interface for resources, which are created locally.
 * @see org.apache.cxf.ws.transfer.resourcefactory.resolver.ResourceResolver
 */
public class ResourceLocal implements Resource {

    @jakarta.annotation.Resource
    protected WebServiceContext context;

    protected ResourceManager manager;

    protected List<ResourceTypeIdentifier> resourceTypeIdentifiers;

    protected Map<String, Dialect> dialects;

    public ResourceLocal() {
        dialects = new HashMap<>();
        dialects.put(FragmentDialectConstants.FRAGMENT_2011_03_IRI, new FragmentDialect());
    }

    public ResourceManager getManager() {
        return manager;
    }

    public void setManager(ResourceManager manager) {
        this.manager = manager;
    }

    public List<ResourceTypeIdentifier> getResourceTypeIdentifiers() {
        if (resourceTypeIdentifiers == null) {
            resourceTypeIdentifiers = new ArrayList<>();
        }
        return resourceTypeIdentifiers;
    }

    public void setResourceTypeIdentifiers(List<ResourceTypeIdentifier> resourceTypeIdentifiers) {
        this.resourceTypeIdentifiers = resourceTypeIdentifiers;
    }

    /**
     * Register Dialect object for URI.
     * @param iri
     * @param dialect
     */
    public void registerDialect(String iri, Dialect dialect) {
        if (dialects.containsKey(iri)) {
            throw new IllegalArgumentException(String.format("IRI \"%s\" is already registered", iri));
        }
        dialects.put(iri, dialect);
    }

    /**
     * Unregister dialect URI.
     * @param iri
     */
    public void unregisterDialect(String iri) {
        if (!dialects.containsKey(iri)) {
            throw new IllegalArgumentException(String.format("IRI \"%s\" is not registered", iri));
        }
        dialects.remove(iri);
    }

    @Override
    public GetResponse get(Get body) {
        // Getting reference paramaters
        AddressingProperties addrProps = (AddressingProperties) ((WrappedMessageContext) context
                .getMessageContext()).getWrappedMessage()
                .getContextualProperty(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND);
        ReferenceParametersType refParams = addrProps
                .getToEndpointReference()
                .getReferenceParameters();
        GetResponse response = new GetResponse();
        // Getting representation from the ResourceManager
        Representation representation = manager.get(refParams);
        // Dialect processing
        if (body.getDialect() != null && !body.getDialect().isEmpty()) {
            if (dialects.containsKey(body.getDialect())) {
                Dialect dialect = dialects.get(body.getDialect());
                // Send fragment of resource instead it's representation.
                response.getAny().add(dialect.processGet(body, representation));
            } else {
                throw new UnknownDialect();
            }
        } else {
            // Send representation obtained from ResourceManager.
            response.setRepresentation(representation);
        }
        return response;
    }

    @Override
    public DeleteResponse delete(Delete body) {
        // Getting reference paramaters
        AddressingProperties addrProps = (AddressingProperties) ((WrappedMessageContext) context
                .getMessageContext()).getWrappedMessage()
                .getContextualProperty(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND);
        ReferenceParametersType refParams = addrProps
                .getToEndpointReference()
                .getReferenceParameters();
        boolean delete = true;
        // Dialect processing
        if (body.getDialect() != null && !body.getDialect().isEmpty()) {
            if (dialects.containsKey(body.getDialect())) {
                Dialect dialect = dialects.get(body.getDialect());
                delete = dialect.processDelete(body, manager.get(refParams));
            } else {
                throw new UnknownDialect();
            }
        }
        if (delete) {
            manager.delete(refParams);
        }
        return new DeleteResponse();
    }

    @Override
    public PutResponse put(Put body) {
        // Getting reference paramaters
        AddressingProperties addrProps = (AddressingProperties) ((WrappedMessageContext) context
                .getMessageContext()).getWrappedMessage()
                .getContextualProperty(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND);
        ReferenceParametersType refParams = addrProps
                .getToEndpointReference()
                .getReferenceParameters();
        // Getting representation from the ResourceManager
        Representation storedRepresentation = manager.get(refParams);
        // Getting representation from the incoming SOAP message. This representation will be stored.
        Representation putRepresentation = body.getRepresentation();
        // Dialect processing
        if (body.getDialect() != null && !body.getDialect().isEmpty()) {
            if (dialects.containsKey(body.getDialect())) {
                Dialect dialect = dialects.get(body.getDialect());
                putRepresentation = dialect.processPut(body, storedRepresentation);
            } else {
                throw new UnknownDialect();
            }
        }
        ValidAndTransformHelper.validationAndTransformation(
                resourceTypeIdentifiers, putRepresentation, storedRepresentation);
        manager.put(refParams, putRepresentation);
        PutResponse response = new PutResponse();
        response.setRepresentation(putRepresentation);
        return response;
    }

}
