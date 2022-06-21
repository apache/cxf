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

package org.apache.cxf.systest.jaxws.schemavalidation;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;

@WebService(name = "PersonServiceWithRequestResponseAnns",
    targetNamespace = "http://org.apache.cxf/service/PersonServiceWithRequestResponseAnns")
@SchemaValidation(type = SchemaValidationType.BOTH)
public interface PersonServiceWithRequestResponseAnns {
    @WebMethod(operationName = "saveInheritEndpoint")
    @WebResult(name = "Person")
    Person saveInheritEndpoint(@WebParam(name = "Person") Person data);

    @SchemaValidation(type = SchemaValidationType.NONE)
    @WebMethod(operationName = "saveNoValidation")
    @WebResult(name = "Person")
    Person saveNoValidation(@WebParam(name = "Person") Person data);

    @SchemaValidation(type = SchemaValidationType.RESPONSE)
    @WebMethod(operationName = "saveValidateIn")
    @WebResult(name = "Person")
    Person saveValidateIn(@WebParam(name = "Person") Person data);

    @SchemaValidation(type = SchemaValidationType.REQUEST)
    @WebMethod(operationName = "saveValidateOut")
    @WebResult(name = "Person")
    Person saveValidateOut(@WebParam(name = "Person") Person data);
}
