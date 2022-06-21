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
package org.apache.cxf.systest.ws.common;

import java.util.Arrays;

import jakarta.jws.WebService;
import org.apache.cxf.feature.Features;
import org.example.contract.doubleit.DoubleItFault;
import org.example.contract.doubleit.DoubleItSwaPortType;
import org.example.schema.doubleit.DoubleIt3;
import org.example.schema.doubleit.DoubleItResponse;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItSwaPortType")
@Features(features = "org.apache.cxf.feature.LoggingFeature")
public class DoubleIt3Impl implements DoubleItSwaPortType {

    @Override
    public DoubleItResponse doubleIt3(DoubleIt3 parameters, byte[] attachment) throws DoubleItFault {
        int numberToDouble = parameters.getNumberToDouble();
        if (numberToDouble == 0) {
            throw new DoubleItFault("0 can't be doubled!");
        }

        if (!Arrays.equals(attachment, "12345".getBytes())) {
            throw new DoubleItFault("Unexpected attachment value!");
        }

        DoubleItResponse response = new DoubleItResponse();
        response.setDoubledNumber(numberToDouble * 2);
        return response;
    }

}
