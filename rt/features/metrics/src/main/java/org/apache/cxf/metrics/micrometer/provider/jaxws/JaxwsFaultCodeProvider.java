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

package org.apache.cxf.metrics.micrometer.provider.jaxws;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;

public class JaxwsFaultCodeProvider {
    
    public String getFaultCode(Exchange ex, boolean client) {
        FaultMode fm = ex.get(FaultMode.class);
        if (client) {
            if (fm == null && ex.getInFaultMessage() != null) {
                fm = ex.getInFaultMessage().get(FaultMode.class);
            }
            if (fm == null && ex.getOutMessage() != null) {
                fm = ex.getOutMessage().get(FaultMode.class);
            }
        } else {
            if (fm == null && ex.getOutFaultMessage() != null) {
                fm = ex.getOutFaultMessage().get(FaultMode.class);
            }
            if (fm == null && ex.getInMessage() != null) {
                fm = ex.getInMessage().get(FaultMode.class);
            }
        }
        
        if (fm == null) {
            return null;
        }
        
        return fm.name();
    }
}
