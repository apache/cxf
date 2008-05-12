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
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;

public class BindingInfo extends AbstractDescriptionElement implements NamedItem {
    
    private static final Logger LOG = LogUtils.getL7dLogger(BindingInfo.class);
    
    QName name;
    ServiceInfo service;
    final String bindingId;
    
    Map<QName, BindingOperationInfo> operations = new ConcurrentHashMap<QName, BindingOperationInfo>(4);
    
    public BindingInfo(ServiceInfo service, String bindingId) {
        this.service = service;
        this.bindingId = bindingId;
    }
    
    public InterfaceInfo getInterface() {
        return service.getInterface();
    }

    public ServiceInfo getService() {
        return service;
    }

    public String getBindingId() {
        return bindingId;
    }
    
    public void setName(QName n) {
        name = n;
    }
    public QName getName() {
        return name;
    }
    
    private boolean nameEquals(String a, String b, String def) {
        if (a == null) {
            // in case of input/output itself is empty
            return true;
        } else {
            if (b == null) {
                b = def;
            }
            return "".equals(a) ? "".equals(b) : a.equals(b);
        }
    }
    public BindingOperationInfo buildOperation(QName opName, String inName, String outName) {
        for (OperationInfo op : getInterface().getOperations()) {
            if (opName.equals(op.getName())
                && nameEquals(inName, op.getInputName(), op.getName().getLocalPart() + "Request")
                && nameEquals(outName, op.getOutputName(), op.getName().getLocalPart() + "Response")) {
                
                return new BindingOperationInfo(this, op);
            }
        }
        return null;
    }

    /**
     * Adds an operation to this service.
     *
     * @param operation the operation.
     */
    public void addOperation(BindingOperationInfo operation) {
        if (operation.getName() == null) {
            throw new NullPointerException(
                new Message("BINDING.OPERATION.NAME.NOT.NULL", LOG).toString());
        } 
        if (operations.containsKey(operation.getName())) {
            throw new IllegalArgumentException(
                new Message("DUPLICATED.OPERATION.NAME", LOG, new Object[]{operation.getName()}).toString());
        }
        
        operations.put(operation.getName(), operation);
    }
    
    /**
     * Removes an operation from this service.
     *
     * @param operation the operation.
     */
    public void removeOperation(BindingOperationInfo operation) {
        if (operation.getName() == null) {
            throw new NullPointerException(
                new Message("BINDING.OPERATION.NAME.NOT.NULL", LOG).toString());
        } 
        
        operations.remove(operation.getName());
    }

    /**
     * Returns the operation info with the given name, if found.
     *
     * @param oname the name.
     * @return the operation; or <code>null</code> if not found.
     */
    public BindingOperationInfo getOperation(QName oname) {
        return operations.get(oname);
    }

    /**
     * Returns all operations for this service.
     *
     * @return all operations.
     */
    public Collection<BindingOperationInfo> getOperations() {
        return Collections.unmodifiableCollection(operations.values());
    }

    public BindingOperationInfo getOperation(OperationInfo oi) {
        for (BindingOperationInfo b : operations.values()) {
            if (b.getOperationInfo() == oi) {
                return b;
            } else if (b.isUnwrappedCapable() && b.getUnwrappedOperation().getOperationInfo() == oi) {
                return b.getUnwrappedOperation();
            }
        }
        
        return null;
    }
    
    @Override
    public String toString() {
        return "[BindingInfo " + getBindingId() + "]";
    }
}


