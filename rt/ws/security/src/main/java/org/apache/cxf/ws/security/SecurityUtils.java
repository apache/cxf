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
package org.apache.cxf.ws.security;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * Some common functionality
 */
public final class SecurityUtils {
    
    private SecurityUtils() {
        // complete
    }

    public static CallbackHandler getCallbackHandler(Object o) throws WSSecurityException {
        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils.loadClass((String)o, 
                                                                      SecurityUtils.class).newInstance();
            } catch (Exception e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
            }
        }
        return handler;
    }
    
}
