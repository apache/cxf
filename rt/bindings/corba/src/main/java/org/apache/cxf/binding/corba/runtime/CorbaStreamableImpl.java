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
package org.apache.cxf.binding.corba.runtime;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.CorbaStreamable;
import org.apache.cxf.binding.corba.types.CorbaObjectHandler;

import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

public class CorbaStreamableImpl implements CorbaStreamable {

    private CorbaObjectHandler value;
    private QName name;
    private int mode;
    private TypeCode typecode;

    public CorbaStreamableImpl(CorbaObjectHandler obj, QName elName) {
        value = obj;
        name = elName;
        typecode = obj.getTypeCode();

        mode = org.omg.CORBA.ARG_OUT.value;
    }

    public void _read(InputStream istream) {
        try {
            CorbaObjectReader reader = new CorbaObjectReader(istream);
            reader.read(value);
        } catch (java.lang.Exception ex) {
            throw new CorbaBindingException("Error reading streamable value", ex);
        }
    }

    public void _write(OutputStream ostream) {
        try {
            CorbaObjectWriter writer = new CorbaObjectWriter(ostream);
            writer.write(value);
        } catch (java.lang.Exception ex) {
            throw new CorbaBindingException("Error writing streamable value", ex);
        }
    }

    public TypeCode _type() {
        return typecode;
    }

    public CorbaObjectHandler getObject() {
        return value;
    }

    public void setObject(CorbaObjectHandler obj) {
        value = obj;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int md) {
        mode = md;
    }

    public String getName() {
        return name.getLocalPart();
    }
}
