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
package org.apache.cxf.binding.corba.utils;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;

public class EprMetaData {
    
    private Definition candidateWsdlDef;
    private Binding binding;
    private QName serviceQName;
    private String portName;
    
    public Binding getBinding() {
        return binding;
    }
    
    public void setBinding(Binding b) {
        binding = b;
    }
    
    public Definition getCandidateWsdlDef() {
        return candidateWsdlDef;
    }
    
    public void setCandidateWsdlDef(Definition def) {
        candidateWsdlDef = def;
    }
    
    public String getPortName() {
        return portName;
    }
    
    public void setPortName(String name) {
        portName = name;
    }
    
    public QName getServiceQName() {
        return serviceQName;
    }
    
    public void setServiceQName(QName name) {
        serviceQName = name;
    }
    
    public boolean isValid() {
        return binding != null && candidateWsdlDef != null;
    }
    
    public String toString() {
        String ret = null;
        if (isValid()) {
            StringBuffer b = new StringBuffer();
            b.append('{');
            b.append(binding.getQName());
            b.append(',');
            b.append(serviceQName);
            b.append(',');
            b.append(portName);
            b.append("@");
            b.append(candidateWsdlDef.getDocumentBaseURI());
            b.append('}');
            ret =  b.toString();
        }
        return ret;
    }
}
