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
package org.apache.cxf.transport.jms.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

public class JndiHelper {

    private Properties environment;

    /**
     * Create a new JndiTemplate instance, using the given environment.
     */
    public JndiHelper(Properties environment) {
        this.environment = environment;
    }

    @SuppressWarnings("rawtypes")
    protected Context createInitialContext() throws NamingException {
        //CHECKSTYLE:OFF
        Hashtable<Object, Object> icEnv = new Hashtable<Object, Object>(environment.size());
        //CHECKSTYLE:ON
        for (Enumeration en = environment.propertyNames(); en.hasMoreElements();) {
            String key = (String)en.nextElement();
            Object value = environment.getProperty(key);
            if (value == null) {
                value = environment.get(key);
            }
            icEnv.put(key, value);
        }
        return new InitialContext(icEnv);
    }

    @SuppressWarnings("unchecked")
    public <T> T lookup(final String name, Class<T> requiredType) throws NamingException {
        Context ctx = createInitialContext();
        try {
            Object located = ctx.lookup(name);
            if (located == null) {
                throw new NameNotFoundException("JNDI object with [" + name + "] not found");
            }
            return (T)located;
        } finally {
            ResourceCloser.close(ctx);
        }
    }

}
