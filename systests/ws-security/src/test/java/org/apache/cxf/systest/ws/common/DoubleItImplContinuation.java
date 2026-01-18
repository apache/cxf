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



import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.feature.Features;
import org.example.contract.doubleit.DoubleItFault;
import org.example.contract.doubleit.DoubleItPortType;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class DoubleItImplContinuation implements DoubleItPortType {
    

    @Resource
    private WebServiceContext context;

    public int doubleIt(int numberToDouble) throws DoubleItFault {
        Continuation continuation = getContinuation();
        if (continuation == null) {
            throw new RuntimeException("Failed to get continuation");
        }
        synchronized (continuation) {
            if (continuation.isNew()) {
                continuation.suspend(5000);
            } else {
                
                if (numberToDouble == 0) {
                    throw new DoubleItFault("0 can't be doubled!");
                }
                return numberToDouble * 2;
            }
        }
        // unreachable
        return 0;
        
    }
    
    

    private Continuation getContinuation() {
        ContinuationProvider provider =
            (ContinuationProvider)context.getMessageContext().get(ContinuationProvider.class.getName());
        return provider.getContinuation();
    }

}
