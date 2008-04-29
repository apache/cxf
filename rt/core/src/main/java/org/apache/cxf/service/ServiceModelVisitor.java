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
package org.apache.cxf.service;

import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.UnwrappedOperationInfo;

/**
 * Implements the Visitor pattern for the Service model.
 * The visit order is as follows:
 * <pre>
 * 1) Begin the overall service info.
 * 2) Begin the service's interface.
 * 3) For each operation, begin the operation.
 * 3.1) begin the input message.
 * 3.1.1) begin and end each part of the input message.
 * 3.2) end the input message.
 * 3.3) begin the output message.
 * 3.3.1) begin and end each part of the output message.
 * 3.4) end the output message
 * 3.5) begin each fault. (3.5-3.6 repeated for each fault)
 * 3.5.1) begin and end each part of each fault
 * 3.6) end each fault.
 * 3.7) if a wrapped operation, begin the corresponding unwrapped operation.
 * 3.8) process the entire unwrapped operation starting at (3).
 * 3.9) end the unwrapped operation.
 * 4) end the operation.
 * 5) end the interface.
 * 6) For each endpoint (= port) begin and end the EndpointInfo
 * 7) For each binding (= BindingInfo) begin and end the BindingInfo.
 * 8) end the service info.
 * </pre>
 * Unwrapped operations <i>share messages</i> with their corresponding wrapped messages,
 * so beware of processing the same messages twice as if unique.
 */
public class ServiceModelVisitor {
    protected ServiceInfo serviceInfo;
    
    public ServiceModelVisitor(ServiceInfo serviceInfo) {
        super();
        this.serviceInfo = serviceInfo;
    }
    
    public void walk() {
        begin(serviceInfo);
        begin(serviceInfo.getInterface());
        
        for (OperationInfo o : serviceInfo.getInterface().getOperations()) {
            begin(o);
            
            visitOperation(o);
            
            end(o);
        }
        
        end(serviceInfo.getInterface());
        for (EndpointInfo endpointInfo : serviceInfo.getEndpoints()) {
            begin(endpointInfo);
            end(endpointInfo);
        }
        for (BindingInfo bindingInfo : serviceInfo.getBindings()) {
            begin(bindingInfo);
            end(bindingInfo);
        }
        end(serviceInfo);
    }

    private void visitOperation(OperationInfo o) {
        MessageInfo in = o.getInput();
        if (in != null) {
            begin(in);
            
            for (MessagePartInfo part : in.getMessageParts()) {
                begin(part);
                end(part);
            }
            
            end(in);
        }
        
        MessageInfo out = o.getOutput();
        if (out != null) {
            begin(out);
            
            for (MessagePartInfo part : out.getMessageParts()) {
                begin(part);
                end(part);
            }
            
            end(out);
        }
        
        for (FaultInfo f : o.getFaults()) {
            begin(f);
            
            for (MessagePartInfo part : f.getMessageParts()) {
                begin(part);
                end(part);
            }
            
            end(f);
        }
        
        if (o.isUnwrappedCapable()) {
            OperationInfo uop = o.getUnwrappedOperation();
            begin(uop);
            visitOperation(o.getUnwrappedOperation());
            end(uop);
        }
    }
    
    public void begin(ServiceInfo service) {
    }
    public void begin(InterfaceInfo intf) {
    }
    public void begin(OperationInfo op) {
    }
    public void begin(UnwrappedOperationInfo op) {
    }
    public void begin(MessageInfo msg) {
    }
    public void begin(MessagePartInfo part) {
    }
    public void begin(FaultInfo fault) {
    }
    public void end(ServiceInfo service) {
    }
    public void end(InterfaceInfo intf) {
    }
    public void end(OperationInfo op) {
    }
    public void end(UnwrappedOperationInfo op) {
    }
    public void end(MessageInfo msg) {
    }
    public void end(MessagePartInfo part) {
    }
    public void end(FaultInfo fault) {
    }
    public void begin(EndpointInfo endpointInfo) {
    }
    public void end(EndpointInfo endpointInfo) {
    }
    private void begin(BindingInfo bindingInfo) {
    }
    private void end(BindingInfo bindingInfo) {
    }
}
