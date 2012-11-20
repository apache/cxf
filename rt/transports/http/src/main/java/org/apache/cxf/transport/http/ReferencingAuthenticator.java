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
package org.apache.cxf.transport.http;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class ReferencingAuthenticator extends Authenticator {
    SoftReference<Authenticator> auth;
    Authenticator wrapped;
    public ReferencingAuthenticator(Authenticator cxfauth, Authenticator wrapped) {
        this.auth = new SoftReference<Authenticator>(cxfauth);
        this.wrapped = wrapped;
    }
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        PasswordAuthentication pauth = null;
        if (wrapped != null) {
            try {
                pauth = tryWith(wrapped);
                if (pauth != null) {
                    return pauth;
                }
            } catch (Exception e) {
                pauth = null;
            }
        }
        Authenticator cxfauth = auth.get();
        if (cxfauth == null) {
            try {
                Authenticator.setDefault(wrapped);
            } catch (Throwable t) {
                //ignore
            }
        } else {
            try {
                pauth = tryWith(cxfauth);
            } catch (Exception e1) {
                pauth = null;
            }
        }
        return pauth;
    }  
    PasswordAuthentication tryWith(Authenticator a) throws Exception {
        if (a == null) {
            return null;
        }
        for (final Field f : Authenticator.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                Object o = f.get(this);
                f.set(a, o);
            }
        } 
        final Method m = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
        m.setAccessible(true);
        return (PasswordAuthentication)m.invoke(a);
    }
}