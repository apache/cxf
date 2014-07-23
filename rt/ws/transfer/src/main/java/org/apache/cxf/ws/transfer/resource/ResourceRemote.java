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

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.CreateResponse;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.Dialect;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;
import org.apache.cxf.ws.transfer.shared.faults.UnknownDialect;
import org.apache.cxf.ws.transfer.validationtransformation.ValidAndTransformHelper;

/**
 * Implementation of the Resource interface for resources, which are created remotely.
 * @see ResourceResolver
 * 
 * @author Erich Duda
 */
public class ResourceRemote extends ResourceLocal implements ResourceFactory {

    @Override
    public CreateResponse create(Create body) {
        Representation representation = body.getRepresentation();
        if (body.getDialect() != null && !body.getDialect().isEmpty()) {
            if (dialects.containsKey(body.getDialect())) {
                Dialect dialect = dialects.get(body.getDialect());
                representation = dialect.processCreate(body);
            } else {
                throw new UnknownDialect();
            }
        } else {
            ValidAndTransformHelper.validationAndTransformation(
                getValidators(), representation, null);
        }
        ReferenceParametersType refParams = getManager().create(representation);
        CreateResponse response = new CreateResponse();
        response.setResourceCreated(new EndpointReferenceType());
        response.getResourceCreated().setReferenceParameters(refParams);
        response.setRepresentation(body.getRepresentation());
        return response;
    }
    
}
