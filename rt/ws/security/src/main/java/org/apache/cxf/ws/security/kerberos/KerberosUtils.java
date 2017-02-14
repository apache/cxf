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

package org.apache.cxf.ws.security.kerberos;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ext.WSSecurityException;

/**
 *
 */
public final class KerberosUtils {

    private KerberosUtils() {
        //utility class
    }

    public static KerberosClient getClient(Message message, String type) throws WSSecurityException {
        KerberosClient client = (KerberosClient)message
            .getContextualProperty(SecurityConstants.KERBEROS_CLIENT);
        if (client == null) {
            client = new KerberosClient();

            String jaasContext =
                (String)message.getContextualProperty(SecurityConstants.KERBEROS_JAAS_CONTEXT_NAME);
            String kerberosSpn =
                (String)message.getContextualProperty(SecurityConstants.KERBEROS_SPN);
            try {
                CallbackHandler callbackHandler =
                    SecurityUtils.getCallbackHandler(
                        SecurityUtils.getSecurityPropertyValue(SecurityConstants.CALLBACK_HANDLER, message)
                    );
                client.setCallbackHandler(callbackHandler);
            } catch (Exception ex) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
            }
            boolean useCredentialDelegation =
                MessageUtils.getContextualBoolean(message,
                                              SecurityConstants.KERBEROS_USE_CREDENTIAL_DELEGATION,
                                              false);

            boolean isInServiceNameForm =
                MessageUtils.getContextualBoolean(message,
                                              SecurityConstants.KERBEROS_IS_USERNAME_IN_SERVICENAME_FORM,
                                              false);

            boolean requestCredentialDelegation =
                MessageUtils.getContextualBoolean(message,
                                              SecurityConstants.KERBEROS_REQUEST_CREDENTIAL_DELEGATION,
                                              false);

            client.setContextName(jaasContext);
            client.setServiceName(kerberosSpn);
            client.setUseDelegatedCredential(useCredentialDelegation);
            client.setUsernameServiceNameForm(isInServiceNameForm);
            client.setRequestCredentialDelegation(requestCredentialDelegation);
        }
        return client;
    }

}
