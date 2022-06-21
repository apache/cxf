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

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.Action;
import jakarta.xml.ws.soap.Addressing;
import org.apache.cxf.ws.transfer.Delete;
import org.apache.cxf.ws.transfer.DeleteResponse;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.GetResponse;
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.PutResponse;
import org.apache.cxf.ws.transfer.shared.TransferConstants;

/**
 * The interface definition of a Resource web service, according to the specification.
 */
@WebService(targetNamespace = TransferConstants.TRANSFER_2011_03_NAMESPACE,
        name = TransferConstants.NAME_RESOURCE)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@Addressing(enabled = true, required = true)
public interface Resource {

    @Action(
            input = TransferConstants.ACTION_GET,
            output = TransferConstants.ACTION_GET_RESPONSE)
    @WebMethod(operationName = TransferConstants.NAME_OPERATION_GET)
    @WebResult(
            name = TransferConstants.NAME_MESSAGE_GET_RESPONSE,
            targetNamespace = TransferConstants.TRANSFER_2011_03_NAMESPACE,
            partName = "Body"
    )
    GetResponse get(
        @WebParam(
                name = TransferConstants.NAME_MESSAGE_GET,
                targetNamespace = TransferConstants.TRANSFER_2011_03_NAMESPACE,
                partName = "Body"
        )
        Get body
    );

    @Action(
            input = TransferConstants.ACTION_DELETE,
            output = TransferConstants.ACTION_DELETE_RESPONSE
    )
    @WebMethod(operationName = TransferConstants.NAME_OPERATION_DELETE)
    @WebResult(
            name = TransferConstants.NAME_MESSAGE_DELETE_RESPONSE,
            targetNamespace = TransferConstants.TRANSFER_2011_03_NAMESPACE,
            partName = "Body"
    )
    DeleteResponse delete(
        @WebParam(
                name = TransferConstants.NAME_MESSAGE_DELETE,
                targetNamespace = TransferConstants.TRANSFER_2011_03_NAMESPACE,
                partName = "Body"
        )
        Delete body
    );

    @Action(
            input = TransferConstants.ACTION_PUT,
            output = TransferConstants.ACTION_PUT_RESPONSE
    )
    @WebMethod(operationName = TransferConstants.NAME_OPERATION_PUT)
    @WebResult(
            name = TransferConstants.NAME_MESSAGE_PUT_RESPONSE,
            targetNamespace = TransferConstants.TRANSFER_2011_03_NAMESPACE,
            partName = "Body"
    )
    PutResponse put(
        @WebParam(
                name = TransferConstants.NAME_MESSAGE_PUT,
                targetNamespace = TransferConstants.TRANSFER_2011_03_NAMESPACE,
                partName = "Body"
        )
        Put body
    );
}
