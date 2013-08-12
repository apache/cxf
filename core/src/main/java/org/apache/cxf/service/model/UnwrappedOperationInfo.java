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

import javax.xml.namespace.QName;

public class UnwrappedOperationInfo extends OperationInfo {
    OperationInfo wrappedOp;

    public UnwrappedOperationInfo(OperationInfo op) {
        super(op);
        wrappedOp = op;
        setDelegate(wrappedOp, true);
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
    
    
    public void setOutput(String nm, MessageInfo out) {
        super.setOutput(nm, out);
        out.setDelegate(wrappedOp.getOutput(), false);
    }    

    public void setInput(String nm, MessageInfo in) {
        super.setInput(nm, in);
        in.setDelegate(wrappedOp.getInput(), false);
    }

}
