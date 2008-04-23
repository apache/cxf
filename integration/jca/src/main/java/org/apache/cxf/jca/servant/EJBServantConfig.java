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

package org.apache.cxf.jca.servant;

import java.util.StringTokenizer;

import javax.xml.namespace.QName;


public class EJBServantConfig {
    
    private String jndiName;
    
    private QName serviceName;
    
    private String wsdlURL;
    
    public EJBServantConfig(String jndiName, String value) {
        this.jndiName = jndiName;
        StringTokenizer st = new StringTokenizer(value, "@", true);
        if (st.hasMoreTokens()) {
            String theValue = st.nextToken().trim();
            if ("@".equals(theValue)) {
                if (st.hasMoreTokens()) {
                    this.wsdlURL = st.nextToken().trim();
                }
            } else {
                this.serviceName = getServiceName(theValue);
                if (st.hasMoreTokens() && "@".equals(st.nextToken()) && st.hasMoreTokens()) {
                    this.wsdlURL = st.nextToken().trim();
                }
            }
        }
        
    }
    
    public String getJNDIName() {
        return jndiName;
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    public String getWsdlURL() {
        return wsdlURL;
    }

    public void setWsdlURL(String wsdlURL) {
        this.wsdlURL = wsdlURL;
    }
    
    
    private QName getServiceName(String sname) {
        StringTokenizer st = new StringTokenizer(sname, "}", false);
        String namespace = "";
        String localpart = "";
        if (st.hasMoreTokens()) {
            String value = st.nextToken().trim();
            if (value.startsWith("{")) {
                namespace = value.substring(1);
            } else {
                localpart = value;
            }
        }
        if (st.hasMoreTokens()) {
            localpart = st.nextToken().trim();
        }
        return new QName(namespace, localpart);
    }

}
