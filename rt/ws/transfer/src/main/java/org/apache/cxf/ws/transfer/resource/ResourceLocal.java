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
import java.util.List;
import javax.xml.ws.WebServiceContext;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Delete;
import org.apache.cxf.ws.transfer.DeleteResponse;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.GetResponse;
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.PutResponse;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.shared.TransferConstants;
import org.apache.cxf.ws.transfer.validationtransformation.ResourceValidator;
import org.apache.cxf.ws.transfer.validationtransformation.ValidAndTransformHelper;

/**
 * Implementation of the Resource interface for resources, which are created locally.
 * @see ResourceResolver
 * 
 * @author Erich Duda
 */
public class ResourceLocal implements Resource {
    
    @javax.annotation.Resource
    protected WebServiceContext context;
    
    protected ResourceManager manager;
    
    protected List<ResourceValidator> validators;

    public ResourceManager getManager() {
        return manager;
    }

    public void setManager(ResourceManager manager) {
        this.manager = manager;
    }

    public List<ResourceValidator> getValidators() {
        if (validators == null) {
            validators = new ArrayList<ResourceValidator>();
        }
        return validators;
    }

    public void setValidators(List<ResourceValidator> validators) {
        this.validators = validators;
    }
    
    @Override
    public GetResponse get(Get body) {
        ReferenceParametersType refParams = (ReferenceParametersType) ((WrappedMessageContext) context
                .getMessageContext()).getWrappedMessage()
                .getContextualProperty(TransferConstants.REF_PARAMS_CONTEXT_KEY);
        GetResponse response = new GetResponse();
        response.setRepresentation(manager.get(refParams));
        return response;
    }

    @Override
    public DeleteResponse delete(Delete body) {
        ReferenceParametersType refParams = (ReferenceParametersType) ((WrappedMessageContext) context
                .getMessageContext()).getWrappedMessage()
                .getContextualProperty(TransferConstants.REF_PARAMS_CONTEXT_KEY);
        DeleteResponse response = new DeleteResponse();
        manager.delete(refParams);
        return response;
    }

    @Override
    public PutResponse put(Put body) {
        ReferenceParametersType refParams = (ReferenceParametersType) ((WrappedMessageContext) context
                .getMessageContext()).getWrappedMessage()
                .getContextualProperty(TransferConstants.REF_PARAMS_CONTEXT_KEY);
        ValidAndTransformHelper.validationAndTransformation(
                validators, body.getRepresentation(), manager.get(refParams));
        manager.put(refParams, body.getRepresentation());
        PutResponse response = new PutResponse();
        response.setRepresentation(body.getRepresentation());
        return response;
    }
    
}
