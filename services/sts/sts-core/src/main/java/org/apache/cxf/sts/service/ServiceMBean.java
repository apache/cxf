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

package org.apache.cxf.sts.service;

import java.util.List;

/**
 * This MBean represents a service. It defines a single operation
 * "isAddressInEndpoints(String address)". This is called by the Issue binding, passing
 * through the address URL that is supplied as part of "AppliesTo". The AppliesTo address
 * must match with a "known" address of the implementation of this MBean.
 */
public interface ServiceMBean {

    /**
     * Return true if the supplied address corresponds to a known address for this service
     */
    boolean isAddressInEndpoints(String address);

    /**
     * Get the default Token Type to be issued for this Service
     */
    String getTokenType();

    /**
     * Set the default Token Type to be issued for this Service
     */
    void setTokenType(String tokenType);

    /**
     * Get the default Key Type to be issued for this Service
     */
    String getKeyType();

    /**
     * Set the default Key Type to be issued for this Service
     */
    void setKeyType(String keyType);

    /**
     * Set the list of endpoint addresses that correspond to this service
     */
    void setEndpoints(List<String> endpoints);

    /**
     * Get the EncryptionProperties to be used to encrypt tokens issued for this service
     */
    EncryptionProperties getEncryptionProperties();

    /**
     * Set the EncryptionProperties to be used to encrypt tokens issued for this service
     */
    void setEncryptionProperties(EncryptionProperties encryptionProperties);

}
