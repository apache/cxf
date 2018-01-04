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

package org.apache.cxf.ws.transfer.resourcefactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.CreateResponse;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.Dialect;
import org.apache.cxf.ws.transfer.resourcefactory.resolver.ResourceReference;
import org.apache.cxf.ws.transfer.resourcefactory.resolver.ResourceResolver;
import org.apache.cxf.ws.transfer.shared.TransferConstants;
import org.apache.cxf.ws.transfer.shared.faults.UnknownDialect;
import org.apache.cxf.ws.transfer.validationtransformation.ResourceTypeIdentifier;
import org.apache.cxf.ws.transfer.validationtransformation.ValidAndTransformHelper;

/**
 * ResourceFactory implementation.
 */
public class ResourceFactoryImpl implements ResourceFactory {

    protected ResourceResolver resourceResolver;

    protected List<ResourceTypeIdentifier> resourceTypeIdentifiers;

    protected Map<String, Dialect> dialects;

    public ResourceFactoryImpl() {
        dialects = new HashMap<>();
    }

    @Override
    public CreateResponse create(Create body) {
        if (body.getDialect() != null && !body.getDialect().isEmpty()) {
            if (dialects.containsKey(body.getDialect())) {
                Dialect dialect = dialects.get(body.getDialect());
                Representation representation = dialect.processCreate(body);
                body.setRepresentation(representation);
            } else {
                throw new UnknownDialect();
            }
        }
        ValidAndTransformHelper.validationAndTransformation(
                resourceTypeIdentifiers, body.getRepresentation(), null);

        ResourceReference resourceReference = resourceResolver.resolve(body);
        if (resourceReference.getResourceManager() != null) {
            return createLocally(body, resourceReference);
        }
        return createRemotely(body, resourceReference);
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public void setResourceResolver(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
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
     * @param uri
     * @param dialect
     */
    public void registerDialect(String uri, Dialect dialect) {
        if (dialects.containsKey(uri)) {
            throw new IllegalArgumentException(String.format("URI \"%s\" is already registered", uri));
        }
        dialects.put(uri, dialect);
    }

    /**
     * Unregister dialect URI.
     * @param uri
     */
    public void unregisterDialect(String uri) {
        if (!dialects.containsKey(uri)) {
            throw new IllegalArgumentException(String.format("URI \"%s\" is not registered", uri));
        }
        dialects.remove(uri);
    }

    private CreateResponse createLocally(Create body, ResourceReference ref) {
        Representation representation = body.getRepresentation();
        ReferenceParametersType refParams = ref.getResourceManager().create(representation);

        CreateResponse response = new CreateResponse();
        response.setResourceCreated(new EndpointReferenceType());
        response.getResourceCreated().setAddress(new AttributedURIType());
        response.getResourceCreated()
                .getAddress()
                .setValue(ref.getResourceURL());
        response.getResourceCreated()
                .setReferenceParameters(refParams);
        response.setRepresentation(body.getRepresentation());

        return response;
    }

    private CreateResponse createRemotely(Create body, ResourceReference ref) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(ResourceFactory.class);
        factory.setAddress(ref.getResourceURL()
                + TransferConstants.RESOURCE_REMOTE_SUFFIX);
        ResourceFactory client = (ResourceFactory) factory.create();
        CreateResponse response = client.create(body);
        // Adding of endpoint address to the response.
        response.getResourceCreated().setAddress(new AttributedURIType());
        response.getResourceCreated().getAddress().setValue(ref.getResourceURL());
        return response;
    }
}
