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

package org.apache.cxf.service.model;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

public class UnwrappedOperationInfo extends OperationInfo {
    OperationInfo wrappedOp;

    public UnwrappedOperationInfo(OperationInfo op) {
        super(op);
        wrappedOp = op;
    }
    
    public OperationInfo getWrappedOperation() {
        return wrappedOp;
    }
    
    public boolean isUnwrapped() {
        return true;
    }

    public FaultInfo addFault(QName name, QName message) {
        return wrappedOp.addFault(name, message);
    }
    
    public FaultInfo getFault(QName name) {
        return wrappedOp.getFault(name);
    }
    
    public Collection<FaultInfo> getFaults() {
        return wrappedOp.getFaults();
    }
    
    public Object getProperty(String name) {
        return wrappedOp.getProperty(name);
    }
    
    public <T> T getProperty(String name, Class<T> cls) {
        return wrappedOp.getProperty(name, cls);
    }
    
    public void setProperty(String name, Object v) {
        wrappedOp.setProperty(name, v);
    }
    
    public void addExtensor(Object el) {
        wrappedOp.addExtensor(el);
    }

    public <T> T getExtensor(Class<T> cls) {
        return wrappedOp.getExtensor(cls);
    }
    public <T> List<T> getExtensors(Class<T> cls) {
        return wrappedOp.getExtensors(cls);
    }

}
