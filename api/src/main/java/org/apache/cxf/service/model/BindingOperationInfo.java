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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;


/**
 * 
 */
public class BindingOperationInfo extends AbstractPropertiesHolder {

    protected OperationInfo opInfo;

    BindingInfo bindingInfo;

    BindingMessageInfo inputMessage;
    BindingMessageInfo outputMessage;
    Map<QName, BindingFaultInfo> faults;

    BindingOperationInfo opHolder;

    public BindingOperationInfo() {
    }
    
    public BindingOperationInfo(BindingInfo bi, OperationInfo opinfo) { 
        bindingInfo = bi;
        opInfo = opinfo;
        
        if (opInfo.getInput() != null) {
            inputMessage = new BindingMessageInfo(opInfo.getInput(), this);
        } else {
            inputMessage = null;
        }
        if (opInfo.getOutput() != null) {
            outputMessage = new BindingMessageInfo(opInfo.getOutput(), this);
        } else {
            outputMessage = null;
        }
        
        Collection<FaultInfo> of = opinfo.getFaults();
        if (of != null && !of.isEmpty()) {
            faults = new ConcurrentHashMap<QName, BindingFaultInfo>(of.size());
            for (FaultInfo fault : of) {
                faults.put(fault.getFaultName(), new BindingFaultInfo(fault, this));
            }
        }  
        
        if (opinfo.isUnwrappedCapable()) {
            opHolder = new BindingOperationInfo(bi, opinfo.getUnwrappedOperation(), this);
        }
    }
    BindingOperationInfo(BindingInfo bi, OperationInfo opinfo, BindingOperationInfo wrapped) {
        this(bi, opinfo);
        opHolder = wrapped;
    }

    public void updateUnwrappedOperation() {
        if (opInfo.isUnwrappedCapable()
            && opHolder == null) {
            opHolder = new BindingOperationInfo(bindingInfo, opInfo.getUnwrappedOperation(), this);
        }        
    }
    
    public BindingInfo getBinding() {
        return bindingInfo;
    }
    
    public QName getName() {
        return opInfo.getName();
    }
    
    public OperationInfo getOperationInfo() {
        return opInfo;
    }

    public BindingMessageInfo getInput() {
        return inputMessage;
    }
    
    public BindingMessageInfo getOutput() {
        return outputMessage;
    }
    
    public BindingFaultInfo getFault(QName name) {
        if (faults != null) {
            return faults.get(name);
        }
        return null;
    }
    public Collection<BindingFaultInfo> getFaults() {
        if (faults == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(faults.values());
    }
    
    public boolean isUnwrappedCapable() {
        return opInfo.isUnwrappedCapable();
    }
    public BindingOperationInfo getUnwrappedOperation() {
        return opHolder;
    }
    public void setUnwrappedOperation(BindingOperationInfo op) {
        opHolder = op;
    }
    public boolean isUnwrapped() {
        return opInfo.isUnwrapped();
    }
    public BindingOperationInfo getWrappedOperation() {
        return opHolder;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("[BindingOperationInfo: ")
            .append(getName() == null ? "" : getName())
            .append("]").toString();
    }
    
}
