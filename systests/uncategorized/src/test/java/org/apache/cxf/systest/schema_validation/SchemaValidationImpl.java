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

package org.apache.cxf.systest.schema_validation;

import java.io.Serializable;
import java.util.List;

import jakarta.jws.WebService;
import org.apache.schema_validation.DoSomethingFault;
import org.apache.schema_validation.SchemaValidation;
import org.apache.schema_validation.types.ComplexStruct;
import org.apache.schema_validation.types.OccuringStruct;
import org.apache.schema_validation.types.SomeFault;
import org.apache.schema_validation.types.SomeHeader;
import org.apache.schema_validation.types.SomeRequest;
import org.apache.schema_validation.types.SomeRequestWithHeader;
import org.apache.schema_validation.types.SomeResponse;
import org.apache.schema_validation.types.SomeResponseWithHeader;

@WebService(serviceName = "SchemaValidationService",
            portName = "SoapPort",
            endpointInterface = "org.apache.schema_validation.SchemaValidation",
            targetNamespace = "http://apache.org/schema_validation",
            wsdlLocation = "classpath:/wsdl/schema_validation.wsdl")
public class SchemaValidationImpl implements SchemaValidation {

    public boolean setComplexStruct(ComplexStruct in) {
        return true;
    }

    public boolean setOccuringStruct(OccuringStruct in) {
        return true;
    }

    public ComplexStruct getComplexStruct(String in) {
        ComplexStruct complexStruct = new ComplexStruct();
        complexStruct.setElem1(in + "-one");
        // Don't initialize a member of the structure.  Validation should throw
        // an exception.
        // complexStruct.setElem2(in + "-two");
        complexStruct.setElem3(in.length());
        return complexStruct;
    }

    public OccuringStruct getOccuringStruct(String in) {
        OccuringStruct occuringStruct = new OccuringStruct();
        // Populate the list in the wrong order.  Validation should throw
        // an exception.
        List<Serializable> floatIntStringList = occuringStruct.getVarFloatAndVarIntAndVarString();
        floatIntStringList.add(in + "-two");
        floatIntStringList.add(Integer.valueOf(2));
        floatIntStringList.add(Float.valueOf(2.5f));
        return occuringStruct;
    }

    @Override
    public SomeResponse doSomething(SomeRequest in) throws DoSomethingFault {
        SomeResponse response = new SomeResponse();
        if ("1234567890".equals(in.getId())) {
            response.setTransactionId("aaaaaaaaaaxxx"); // invalid transaction id
        } else if ("9999999999".equals(in.getId())) {
            SomeFault someFault = new SomeFault();
            someFault.setErrorCode("1234");
            throw new DoSomethingFault("Fault", someFault);
        } else if ("8888888888".equals(in.getId())) {
            SomeFault someFault = new SomeFault();
            someFault.setErrorCode("1");
            throw new DoSomethingFault("Fault", someFault);
        } else {
            response.setTransactionId("aaaaaaaaaa");
        }

        return response;
    }


    public SomeResponseWithHeader doSomethingWithHeader(SomeRequestWithHeader in,
                                                        SomeHeader inHeader) {
        return null;
    }
}
