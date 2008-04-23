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

package org.apache.cxf.binding.corba;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.runtime.CorbaStreamableImpl;
import org.apache.cxf.binding.corba.types.CorbaObjectHandler;
import org.apache.cxf.message.AbstractWrappedMessage;
import org.apache.cxf.message.Message;
import org.omg.CORBA.NVList;
import org.omg.CORBA.SystemException;

public class CorbaMessage extends AbstractWrappedMessage {

    private List<CorbaStreamable> arguments;
    private CorbaStreamable returnParam;
    private CorbaStreamable except;
    private SystemException systemExcept;
    private CorbaTypeMap corbaTypeMap;
    
    private NVList list;

    public CorbaMessage(Message m) {
        super(m);
        if (m instanceof CorbaMessage) {
            CorbaMessage msg = (CorbaMessage)m;
            CorbaStreamable[] data = msg.getStreamableArguments();
            setStreamableArguments(data);            
            returnParam = msg.getStreamableReturn();
            except = msg.getStreamableException();
            systemExcept = msg.getSystemException();
            list = msg.getList();
            corbaTypeMap = msg.getCorbaTypeMap();
        } else {
            this.arguments = new ArrayList<CorbaStreamable>();    
        }        
    }

    
    public void setList(NVList lst) {
        this.list = lst;
    }
    
    public NVList getList() {
        return this.list;
    }
    
    public CorbaStreamable getStreamableException() {
        return this.except;
    }

    public CorbaStreamable getStreamableReturn() {
        return this.returnParam;
    }
    
    public SystemException getSystemException() {
        return this.systemExcept;
    }
    
    public void setSystemException(SystemException sysEx) {
        systemExcept = sysEx;
    }

    public final void addStreamableArgument(CorbaStreamable arg) {
        if (this.arguments == null) {
            this.arguments = new ArrayList<CorbaStreamable>(1);
        }

        this.arguments.add(arg);
    }
    
    public CorbaStreamable[] getStreamableArguments() {
        return this.arguments.toArray(new CorbaStreamable[0]);
    }


    public final void setStreamableArguments(CorbaStreamable[] data) {
        if (this.arguments == null) {
            this.arguments = new ArrayList<CorbaStreamable>(data.length);
        }

        for (CorbaStreamable streamable : data) {         
            addStreamableArgument(streamable);
        }       
    }

    public void setStreamableArgumentValue(CorbaObjectHandler data, int idx) {
        if (idx >= this.arguments.size()) {
            throw new CorbaBindingException("setStreamableArgumentValue: Index out of range");
        }

        this.arguments.get(idx).setObject(data);
    }

    public void setStreamableArgumentValues(CorbaObjectHandler[] data) {
        for (int i = 0; i < data.length; ++i) {
            this.arguments.get(i).setObject(data[i]);
        }
    }

    public void setStreamableReturn(CorbaStreamable data) {
        returnParam = data;
    }

    public void setStreamableReturnValue(CorbaObjectHandler data) {
        // TODO: Handle case of the return parameter has not yet been initialized.
        returnParam.setObject(data);
    }
    
    public void setStreamableException(CorbaStreamable ex) {
        except = ex;
    }
    
    public void setStreamableExceptionValue(CorbaObjectHandler exData) {
        // TODO: Handle case of the return parameter has not yet been initialized.
        except.setObject(exData);
    }

    public CorbaStreamable createStreamableObject(CorbaObjectHandler obj, QName elName) {
        return new CorbaStreamableImpl(obj, elName);
    }

    public CorbaTypeMap getCorbaTypeMap() {
        return corbaTypeMap;
    }

    public void setCorbaTypeMap(CorbaTypeMap typeMap) {
        corbaTypeMap = typeMap;
    }
}
