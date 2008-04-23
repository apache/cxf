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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;

public class OperationInfo extends AbstractPropertiesHolder implements NamedItem {
    private static final Logger LOG = LogUtils.getL7dLogger(OperationInfo.class);
    InterfaceInfo intf;
    QName opName;
    String inName;
    MessageInfo inputMessage;
    String outName;
    MessageInfo outputMessage;
    Map<QName, FaultInfo> faults;
    OperationInfo unwrappedOperation;
    List<String> parameterOrdering;     

    public OperationInfo() {
    }
    
    OperationInfo(InterfaceInfo it, QName n) { 
        intf = it;
        setName(n);
    }
    OperationInfo(OperationInfo op) {
        intf = op.getInterface();
        setName(op.getName());
    }
    
    /**
     * Returns the name of the Operation.
     * @return the name of the Operation
     */
    public QName getName() {
        return opName;
    }
    /**
     * Sets the name of the operation.
     * @param name the new name of the operation
     */
    public final void setName(QName name) {
        if (name == null) {
            throw new NullPointerException("Operation Name cannot be null.");                
        }        
        opName = name;
    }
    public InterfaceInfo getInterface() {
        return intf;
    }

    
    public MessageInfo createMessage(QName nm, MessageInfo.Type type) {
        return new MessageInfo(this, type, nm);
    }

    public MessageInfo getOutput() {
        return outputMessage;
    }
    public String getOutputName() {
        return outName;
    }
    public void setOutput(String nm, MessageInfo out) {
        outName = nm;
        outputMessage = out;
    }    
    public boolean hasOutput() {
        return outputMessage != null;
    }

    public MessageInfo getInput() {
        return inputMessage;
    }
    public String getInputName() {
        return inName;
    }
    public void setInput(String nm, MessageInfo in) {
        inName = nm;
        inputMessage = in;
    }
    public boolean hasInput() {
        return inputMessage != null;
    }
    
    public boolean isOneWay() {
        return inputMessage != null && outputMessage == null;
    }
    
    public boolean isUnwrappedCapable() {
        return unwrappedOperation != null;
    }
    
    public OperationInfo getUnwrappedOperation() {
        return unwrappedOperation;
    }
    public void setUnwrappedOperation(OperationInfo op) {
        unwrappedOperation = op;
    }
    public boolean isUnwrapped() {
        return false;
    }
    
    
    /**
     * Adds an fault to this operation.
     *
     * @param name the fault name.
     */
    public FaultInfo addFault(QName name, QName message) {
        if (name == null) {
            throw new NullPointerException(new Message("FAULT.NAME.NOT.NULL", LOG).toString());
        } 
        if (faults != null && faults.containsKey(name)) {
            throw new IllegalArgumentException(
                new Message("DUPLICATED.FAULT.NAME", LOG, new Object[] {name}).toString());
        }
        FaultInfo fault = new FaultInfo(name, message, this);
        addFault(fault);
        return fault;
    }

    /**
     * Adds a fault to this operation.
     *
     * @param fault the fault.
     */
    public synchronized void addFault(FaultInfo fault) {
        if (faults == null) { 
            faults = new ConcurrentHashMap<QName, FaultInfo>(4);
        }
        faults.put(fault.getFaultName(), fault);
    }

    /**
     * Removes a fault from this operation.
     *
     * @param name the qualified fault name.
     */
    public void removeFault(QName name) {
        if (faults != null) {
            faults.remove(name);
        }
    }

    /**
     * Returns the fault with the given name, if found.
     *
     * @param name the name.
     * @return the fault; or <code>null</code> if not found.
     */
    public FaultInfo getFault(QName name) {
        if (faults != null) {
            return faults.get(name);
        }
        return null;
    }
    
    public boolean hasFaults() {
        return faults != null && faults.size() > 0;
    }

    /**
     * Returns all faults for this operation.
     *
     * @return all faults.
     */
    public Collection<FaultInfo> getFaults() {
        if (faults == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(faults.values());
    }
    
    public void setParameterOrdering(List<String> o) {
        this.parameterOrdering = o;
    }
    
    public List<String> getParameterOrdering() {
        return parameterOrdering;
    }
    
    @Override
    public String toString() {
        return new StringBuilder().append("[OperationInfo: ")
            .append(opName)
            .append("]").toString();
    }
}
