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

package org.apache.cxf.binding.soap.interceptor;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;


/**
 * This has been merged into the SoapPreProtocolOutInterceptor
 * to make sure the content type is set properly only once.
 */
@Deprecated
public class SoapActionOutInterceptor extends AbstractSoapInterceptor {
    
    public SoapActionOutInterceptor() {
        super(Phase.POST_LOGICAL);
    }
    
    public void handleMessage(SoapMessage message) throws Fault {
        //no-op
    }

}
