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

package org.apache.cxf.transport.jms.uri;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 */
public class JMSEndpoint extends JMSEndpointType {
    Map<String, String> jndiParameters = new HashMap<String, String>();
    Map<String, String> parameters = new HashMap<String, String>();

    /**
     * @param uri
     * @param subject
     */
    public JMSEndpoint(String endpointUri, String jmsVariant, String destinationName) {
        this.endpointUri = endpointUri;
        this.jmsVariant = jmsVariant;
        this.destinationName = destinationName;
    }

    public String getRequestURI() {
        String requestUri = "jms:" + jmsVariant + ":" + destinationName;
        boolean first = true;
        Iterator iter = parameters.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            String value = (String)parameters.get(key);
            if (first) {
                requestUri += "?" + key + "=" + value;
                first = false;
            } else {
                requestUri += "&" + key + "=" + value;
            }
        }
        return requestUri;
    }

    /**
     * @param key
     * @param value
     */
    public void putJndiParameter(String key, String value) {
        jndiParameters.put(key, value);
    }

    public void putParameter(String key, String value) {
        parameters.put(key, value);
    }

    /**
     * @param targetserviceParameterName
     * @return
     */
    public String getParameter(String key) {
        return (String)parameters.get(key);
    }

    public Map getJndiParameters() {
        return jndiParameters;
    }

    /**
     * @return
     */
    public Map getParameters() {
        return parameters;
    }
}
