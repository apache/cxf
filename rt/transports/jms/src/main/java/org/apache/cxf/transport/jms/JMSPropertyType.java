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
package org.apache.cxf.transport.jms;

import javax.jms.JMSException;
import javax.jms.Message;

public class JMSPropertyType {
    private String name;
    private Object value;
    
    @Deprecated
    public JMSPropertyType() {
    }
    
    public JMSPropertyType(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
    
    @Deprecated
    public void setName(String name) {
        this.name = name;
    }

    @Deprecated
    public void setValue(Object value) {
        this.value = value;
    }

    public void writeTo(Message jmsMessage) throws JMSException {
        if (value == null) {
            jmsMessage.setStringProperty(name, null);
            return;
        }
        Class<?> cls = value.getClass();
        if (cls == String.class) {
            jmsMessage.setStringProperty(name, (String)value);
        } else if (cls == Integer.TYPE || cls == Integer.class) {
            jmsMessage.setIntProperty(name, (Integer)value);
        } else if (cls == Double.TYPE || cls == Double.class) {
            jmsMessage.setDoubleProperty(name, (Double)value);
        } else if (cls == Float.TYPE || cls == Float.class) {
            jmsMessage.setFloatProperty(name, (Float)value);
        } else if (cls == Long.TYPE || cls == Long.class) {
            jmsMessage.setLongProperty(name, (Long)value);
        } else if (cls == Boolean.TYPE || cls == Boolean.class) {
            jmsMessage.setBooleanProperty(name, (Boolean)value);
        } else if (cls == Short.TYPE || cls == Short.class) {
            jmsMessage.setShortProperty(name, (Short)value);
        } else if (cls == Byte.TYPE || cls == Byte.class) {
            jmsMessage.setShortProperty(name, (Byte)value);
        } else {
            jmsMessage.setObjectProperty(name, value);
        }
    }
}

