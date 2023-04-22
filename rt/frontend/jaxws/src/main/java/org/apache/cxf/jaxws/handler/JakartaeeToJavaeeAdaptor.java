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
package org.apache.cxf.jaxws.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.jaxws.handler.types.CString;
import org.apache.cxf.jaxws.handler.types.DescriptionType;
import org.apache.cxf.jaxws.handler.types.DisplayNameType;
import org.apache.cxf.jaxws.handler.types.FullyQualifiedClassType;
import org.apache.cxf.jaxws.handler.types.IconType;
import org.apache.cxf.jaxws.handler.types.ParamValueType;
import org.apache.cxf.jaxws.handler.types.PortComponentHandlerType;
import org.apache.cxf.jaxws.handler.types.XsdQNameType;
import org.apache.cxf.jaxws.handler.types.XsdStringType;

public final class JakartaeeToJavaeeAdaptor {

    private JakartaeeToJavaeeAdaptor() {
        // forbidden instantiation
    }

    public static PortComponentHandlerType of(org.apache.cxf.jaxws.handler.jakartaee.PortComponentHandlerType pcht) {
        return new PortComponentHandlerTypeAdaptor(pcht);
    }

    private static CString of(org.apache.cxf.jaxws.handler.jakartaee.CString cs) {
        return new CStringAdaptor(cs);
    }

    private static FullyQualifiedClassType of(org.apache.cxf.jaxws.handler.jakartaee.FullyQualifiedClassType fqct) {
        return new FullyQualifiedClassTypeAdaptor(fqct);
    }

    private static XsdStringType of(org.apache.cxf.jaxws.handler.jakartaee.XsdStringType xst) {
        return new XsdStringTypeAdaptor(xst);
    }

    private static ParamValueType of(org.apache.cxf.jaxws.handler.jakartaee.ParamValueType pvt) {
        return new ParamValueTypeAdaptor(pvt);
    }

    private static final class PortComponentHandlerTypeAdaptor extends PortComponentHandlerType {

        private final org.apache.cxf.jaxws.handler.jakartaee.PortComponentHandlerType adaptee;

        PortComponentHandlerTypeAdaptor(org.apache.cxf.jaxws.handler.jakartaee.PortComponentHandlerType adaptee) {
            this.adaptee = adaptee;
        }

        @Override
        public CString getHandlerName() {
            return of(adaptee.getHandlerName());
        }

        @Override
        public FullyQualifiedClassType getHandlerClass() {
            return of(adaptee.getHandlerClass());
        }

        @Override
        public List<ParamValueType> getInitParam() {
            final List<org.apache.cxf.jaxws.handler.jakartaee.ParamValueType> temp = adaptee.getInitParam();
            if (temp == null || temp.isEmpty()) {
                return Collections.emptyList();
            } else {
                final List<ParamValueType> retVal = new ArrayList<>(temp.size());
                for (org.apache.cxf.jaxws.handler.jakartaee.ParamValueType pvt : temp) {
                    retVal.add(of(pvt));
                }
                return retVal;
            }
        }

        // not implemented methods

        @Override
        public List<DescriptionType> getDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DisplayNameType> getDisplayName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<IconType> getIcon() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<XsdQNameType> getSoapHeader() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<CString> getSoapRole() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setHandlerClass(FullyQualifiedClassType value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setHandlerName(CString value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setId(String value) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class CStringAdaptor extends CString {
        private final org.apache.cxf.jaxws.handler.jakartaee.CString adaptee;

        CStringAdaptor(org.apache.cxf.jaxws.handler.jakartaee.CString adaptee) {
            this.adaptee = adaptee;
        }

        @Override
        public String getValue() {
            return adaptee.getValue();
        }

        @Override
        public void setValue(String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setId(String v) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FullyQualifiedClassTypeAdaptor extends FullyQualifiedClassType {
        private final org.apache.cxf.jaxws.handler.jakartaee.FullyQualifiedClassType adaptee;

        FullyQualifiedClassTypeAdaptor(org.apache.cxf.jaxws.handler.jakartaee.FullyQualifiedClassType adaptee) {
            this.adaptee = adaptee;
        }

        @Override
        public String getValue() {
            return adaptee.getValue();
        }

        @Override
        public void setValue(String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setId(String v) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class ParamValueTypeAdaptor extends ParamValueType {
        private final org.apache.cxf.jaxws.handler.jakartaee.ParamValueType adaptee;

        ParamValueTypeAdaptor(org.apache.cxf.jaxws.handler.jakartaee.ParamValueType adaptee) {
            this.adaptee = adaptee;
        }

        @Override
        public CString getParamName() {
            return of(adaptee.getParamName());
        }

        @Override
        public XsdStringType getParamValue() {
            return of(adaptee.getParamValue());
        }

        @Override
        public List<DescriptionType> getDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setParamName(CString value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setParamValue(XsdStringType value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setId(String value) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class XsdStringTypeAdaptor extends XsdStringType {
        private final org.apache.cxf.jaxws.handler.jakartaee.XsdStringType adaptee;

        XsdStringTypeAdaptor(org.apache.cxf.jaxws.handler.jakartaee.XsdStringType adaptee) {
            this.adaptee = adaptee;
        }

        @Override
        public String getValue() {
            return adaptee.getValue();
        }

        @Override
        public void setValue(String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setId(String v) {
            throw new UnsupportedOperationException();
        }
    }
}
