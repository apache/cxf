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

package org.apache.cxf.ws.security.wss4j;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.Token;

/**
 * An interceptor to add a Kerberos token to the security header of an outbound request, and to
 * process a Kerberos Token on an inbound request. It takes the Kerberos Token from the message 
 * context on the outbound side, where it was previously placed by the 
 * KerberosTokenInterceptorProvider.
 */
public class KerberosTokenInterceptor extends BinarySecurityTokenInterceptor {

    public KerberosTokenInterceptor() {
        super();
    }
    
    protected Token assertTokens(SoapMessage message) {
        return assertTokens(message, SP12Constants.KERBEROS_TOKEN, false);
    }

}
