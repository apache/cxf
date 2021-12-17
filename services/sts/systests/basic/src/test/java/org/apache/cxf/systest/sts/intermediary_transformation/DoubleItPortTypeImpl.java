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
package org.apache.cxf.systest.sts.intermediary_transformation;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.feature.Features;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.Assert;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(features = "org.apache.cxf.feature.LoggingFeature")
/**
 * A PortType implementation that only allows a user call it twice. This is to test the caching logic
 * of the intermediary.
 */
public class DoubleItPortTypeImpl implements DoubleItPortType {

    @Resource
    WebServiceContext wsContext;
    
    private Map<String, Integer> userCount = new ConcurrentHashMap<>();

    public int doubleIt(int numberToDouble) {
        Principal pr = wsContext.getUserPrincipal();

        Assert.assertNotNull("Principal must not be null", pr);
        Assert.assertNotNull("Principal.getName() must not return null", pr.getName());
        
        // Test caching logic here
        updateCache(pr.getName());
        
        return numberToDouble * 2;
    }

    private void updateCache(String user) {
        if (userCount.containsKey(user)) {
            if (userCount.get(user) > 2) {
                throw new RuntimeException("Only two iterations allowed");
            }
            userCount.put(user, userCount.get(user) + 1);
        } else {
            userCount.put(user, 1);
        }
    }
}
