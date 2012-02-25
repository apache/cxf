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

package org.apache.cxf.ws.rm;

/**
 * Supported protocol variations.
 */
public enum ProtocolVariation {
    
    // these must be ordered so the default WS-A namespace is first for a particular WS-RM namespace
    RM10WSA200408(EncoderDecoder10Impl.INSTANCE, RM10Constants.INSTANCE),
    RM10WSA200508(EncoderDecoder10AImpl.INSTANCE, RM10Constants.INSTANCE),
    RM11WSA200508(EncoderDecoder11Impl.INSTANCE, RM11Constants.INSTANCE);
    
    private final EncoderDecoder codec;
    private final RMConstants constants;
    
    private ProtocolVariation(EncoderDecoder ed, RMConstants rmc) {
        codec = ed;
        constants = rmc;
    }

    public EncoderDecoder getCodec() {
        return codec;
    }

    public RMConstants getConstants() {
        return constants;
    }
    
    public String getWSRMNamespace() {
        return codec.getWSRMNamespace();
    }
    
    public String getWSANamespace() {
        return codec.getWSANamespace();
    }
    
    /**
     * Get the information for a supported version of WS-ReliableMessaging and WS-Addressing. If the WS-A
     * namespace is not specified this just returns the first match on the WS-RM namespace, which should
     * always be the default.
     * 
     * @param wsrm WS-RM namespace URI
     * @param wsa WS-A namespace URI (<code>null</code> if not specified)
     * @return variant (<code>null</code> if not a supported version)
     */
    public static ProtocolVariation findVariant(String wsrm, String wsa) {
        for (ProtocolVariation variant : ProtocolVariation.values()) {
            if (variant.getWSRMNamespace().equals(wsrm)
                && (wsa == null || variant.getWSANamespace().equals(wsa))) {
                return variant;
            }
        }
        return null;
    }
}