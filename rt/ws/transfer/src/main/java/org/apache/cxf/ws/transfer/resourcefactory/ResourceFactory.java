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

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.Action;
import jakarta.xml.ws.soap.Addressing;
import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.CreateResponse;
import org.apache.cxf.ws.transfer.shared.TransferConstants;

/**
 * The interface definition of a Resource Factory web service, according to the specification.
 */

@WebService(targetNamespace = TransferConstants.TRANSFER_2011_03_NAMESPACE,
        name = TransferConstants.NAME_RESOURCE_FACTORY)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@Addressing(enabled = true, required = true)
public interface ResourceFactory {
    @Action(
            input = TransferConstants.ACTION_CREATE,
            output = TransferConstants.ACTION_CREATE_RESPONSE
    )
    @WebMethod(operationName = TransferConstants.NAME_OPERATION_CREATE)
    @WebResult(
            name = TransferConstants.NAME_MESSAGE_CREATE_RESPONSE,
            targetNamespace = TransferConstants.TRANSFER_2011_03_NAMESPACE,
            partName = "Body"
    )
    CreateResponse create(
            @WebParam(
                    name = TransferConstants.NAME_MESSAGE_CREATE,
                    targetNamespace = TransferConstants.TRANSFER_2011_03_NAMESPACE,
                    partName = "Body"
            )
            Create body
    );
}
