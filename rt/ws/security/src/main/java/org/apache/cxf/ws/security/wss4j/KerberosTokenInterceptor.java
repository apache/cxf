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
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractToken;

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

    protected AbstractToken assertTokens(SoapMessage message) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        PolicyUtils.assertPolicy(aim, "WssKerberosV5ApReqToken11");
        PolicyUtils.assertPolicy(aim, "WssGssKerberosV5ApReqToken11");
        return assertTokens(message, SPConstants.KERBEROS_TOKEN, false);
    }

}
