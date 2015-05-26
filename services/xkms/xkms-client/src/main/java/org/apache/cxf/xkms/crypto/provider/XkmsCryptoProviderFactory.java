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

package org.apache.cxf.xkms.crypto.provider;

import java.util.Properties;

import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.xkms.crypto.CryptoProviderException;
import org.apache.cxf.xkms.crypto.CryptoProviderFactory;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

/**
 * For usage in OSGi this factory will be published as a service.
 * Outside OSGi it can be used directly 
 */
public class XkmsCryptoProviderFactory implements CryptoProviderFactory {
    
    private final XKMSPortType xkmsConsumer;
        
    public XkmsCryptoProviderFactory(XKMSPortType xkmsConsumer) {
        this.xkmsConsumer = xkmsConsumer;
    }

    @Override
    public Crypto create(Message message) {
        Object crypto = 
            SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_CRYPTO, message);
        if (crypto instanceof Crypto) {
            new XkmsCryptoProvider(xkmsConsumer, (Crypto)crypto);
        }
        
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

    @Override
    public Crypto create() {
        return new XkmsCryptoProvider(xkmsConsumer);
    }

    @Override
    public Crypto create(Crypto fallbackCrypto) {
        return new XkmsCryptoProvider(xkmsConsumer, fallbackCrypto);
    }

    @Override
    public Crypto create(XKMSPortType xkmsClient, Crypto fallbackCrypto) {
        return new XkmsCryptoProvider(xkmsClient, fallbackCrypto);
    }

    @Override
    public Crypto create(XKMSPortType xkmsClient, Crypto fallbackCrypto, boolean allowX509FromJKS) {
        return new XkmsCryptoProvider(xkmsClient, fallbackCrypto, allowX509FromJKS);
    }

    @Override
    public Crypto create(String keystorePropsPath) {
        try {
            Properties keystoreProps = SecurityUtils.loadProperties(keystorePropsPath);
            if (keystoreProps == null) {
                throw new CryptoProviderException("Cannot load security properties: "
                    + keystorePropsPath);
            }
            Crypto defaultCrypto = CryptoFactory.getInstance(keystoreProps);
            return new XkmsCryptoProvider(xkmsConsumer, defaultCrypto);
        } catch (WSSecurityException e) {
            throw new CryptoProviderException("Cannot instantiate crypto factory: "
                + e.getMessage(), e);
        }
    }
}
