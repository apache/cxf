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
package org.apache.cxf.systest.sts.kerberos;

import java.security.Principal;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.feature.Features;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.Assert;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(classes = org.apache.cxf.ext.logging.LoggingFeature.class)
public class DoubleItPortTypeImpl implements DoubleItPortType {

    @Resource
    WebServiceContext wsContext;

    public int doubleIt(int numberToDouble) {
        Principal pr = wsContext.getUserPrincipal();

        Assert.assertNotNull("Principal must not be null", pr);
        Assert.assertNotNull("Principal.getName() must not return null", pr.getName());
        Assert.assertTrue(pr.getName().startsWith("alice"));

        return numberToDouble * 2;
    }

}
