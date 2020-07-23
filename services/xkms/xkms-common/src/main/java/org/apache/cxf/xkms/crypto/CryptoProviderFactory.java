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

import org.apache.cxf.message.Message;
import org.apache.wss4j.common.crypto.Crypto;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

public interface CryptoProviderFactory {

    /**
     * Create with merlin fallback settings retrieved from cxf message
     * @param message
     * @return
     */
    Crypto create(Message message);

    /**
     * Create without fallback crypto
     *
     * @return xkms crypto
     */
    Crypto create();

    /**
     * Create with fallback crypto
     *
     * @param fallbackCrypto
     * @return
     */
    Crypto create(Crypto fallbackCrypto);

    /**
     * Create with overridden keystoreProperties to create default Crypto
     *
     * @param keystoreProperties
     * @return
     */
    Crypto create(String keystoreProperties);

    /**
     * Create with overridden XKMSPortType and fallbackCrypto
     *
     * @param xkmsClient
     * @param fallbackCrypto
     * @return
     */
    Crypto create(XKMSPortType xkmsClient, Crypto fallbackCrypto);

    /**
     * Create with overridden XKMSPortType, fallbackCrypto and control of getting X509 from local keystore
     *
     * @param xkmsClient
     * @param fallbackCrypto
     * @param allowX509FromJKS
     * @return
     */
    Crypto create(XKMSPortType xkmsClient, Crypto fallbackCrypto, boolean allowX509FromJKS);

}
