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

package org.apache.cxf.common.i18n;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;
    
    transient String code;
    transient Object[] parameters;
    transient ResourceBundle bundle;

    /**
     * Constructor.
     *
     * @param key the message catalog (resource bundle) key
     * @param logger a logger with an associated resource bundle
     * @param params the message substitution parameters
     */
    public Message(String key, Logger logger, Object...params) {
        this(key, logger.getResourceBundle(), params);
    }

    /**
     * Constructor.
     *
     * @param key the message catalog (resource bundle) key
     * @param catalog the resource bundle
     * @param params the message substitution parameters
     */
    public Message(String key, ResourceBundle catalog, Object...params) {
        code = key;
        bundle = catalog;
        parameters = params;
    }
    
    public String toString() {
        String fmt = null;
        try {
            if (null == bundle) {
                return code;
            }
            fmt = bundle.getString(code);  
        } catch (MissingResourceException ex) {
            return code;
        }
        return MessageFormat.format(fmt, parameters);
    }
    
    public String getCode() {
        return code;      
    }
    
    public Object[] getParameters() {
        return parameters;
    }

    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
        out.writeUTF(toString());
    }
    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        code = in.readUTF();
        bundle = null;
        parameters = null;
    }
}
