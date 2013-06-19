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

package org.apache.cxf.xkms.crypto;

import java.util.Properties;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

public class XkmsCryptoProviderFactory implements CryptoProviderFactory {
    
    private final XKMSPortType xkmsConsumer;
        
    public XkmsCryptoProviderFactory(XKMSPortType xkmsConsumer) {
        this.xkmsConsumer = xkmsConsumer;
    }

    public Crypto create(Message message) {
        Properties keystoreProps = CryptoProviderUtils
            .loadKeystoreProperties(message,
                                    SecurityConstants.SIGNATURE_PROPERTIES);
        try {
            Crypto defaultCrypto = CryptoFactory.getInstance(keystoreProps);
            return new XkmsCryptoProvider(xkmsConsumer, defaultCrypto);
        } catch (WSSecurityException e) {
            throw new CryptoProviderException("Cannot instantiate crypto factory: "
                                              + e.getMessage(), e);
        }
    }
}
