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

package org.apache.cxf.jaxb;

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/*
 * Override default styles to recognise JAXBElement as needing introspection
 */
public final class JAXBToStringStyle {

    public static final ToStringStyle MULTI_LINE_STYLE =
        new JAXBToStringStyleImpl(true);

    public static final ToStringStyle SIMPLE_STYLE =
        new JAXBToStringStyleImpl(false);

    public static final ToStringStyle DEFAULT_STYLE =
        new JAXBToStringStyleImpl();
    
    private JAXBToStringStyle() {
        //utility class
    }
}

class JAXBToStringStyleImpl extends ToStringStyle {
    JAXBToStringStyleImpl() {
        super();
    }
            
    JAXBToStringStyleImpl(boolean multiLine) {
        super();
        if (multiLine) {
            this.setContentStart("[");
            this.setFieldSeparator(SystemUtils.LINE_SEPARATOR + "  ");
            this.setFieldSeparatorAtStart(true);
            this.setContentEnd(SystemUtils.LINE_SEPARATOR + "]");
        } else {
            // simple
            this.setUseClassName(false);
            this.setUseIdentityHashCode(false);
            this.setUseFieldNames(false);
            this.setContentStart("");
            this.setContentEnd("");
        }
    }
        
    /*
     * Introspect into JAXBElement as a special case as it does not have a
     * toString() and we loose the content
     * 
     * @see org.apache.commons.lang.builder.ToStringStyle
     */
    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, Object value) {
        if (value instanceof JAXBElement) {
            buffer.append(ToStringBuilder.reflectionToString(value, this));
        } else {
            buffer.append(value);
        }
    } 
}
