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

import java.util.Collections;
import java.util.Enumeration;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

public class MessageStub implements Message {

    @Override
    public String getJMSMessageID() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setJMSMessageID(String id) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public long getJMSTimestamp() throws JMSException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setJMSTimestamp(long timestamp) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setJMSCorrelationID(String correlationID) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public String getJMSCorrelationID() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Destination getJMSReplyTo() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setJMSReplyTo(Destination replyTo) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public Destination getJMSDestination() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setJMSDestination(Destination destination) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public int getJMSDeliveryMode() throws JMSException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean getJMSRedelivered() throws JMSException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setJMSRedelivered(boolean redelivered) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public String getJMSType() throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setJMSType(String type) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public long getJMSExpiration() throws JMSException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setJMSExpiration(long expiration) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public int getJMSPriority() throws JMSException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setJMSPriority(int priority) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearProperties() throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean propertyExists(String name) throws JMSException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getBooleanProperty(String name) throws JMSException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public byte getByteProperty(String name) throws JMSException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public short getShortProperty(String name) throws JMSException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getIntProperty(String name) throws JMSException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLongProperty(String name) throws JMSException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getFloatProperty(String name) throws JMSException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getDoubleProperty(String name) throws JMSException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getStringProperty(String name) throws JMSException {
        if ("JMSXUserID".equals(name)) {
            return "jason";
        } else {
            return null;
        }
    }

    @Override
    public Object getObjectProperty(String name) throws JMSException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration getPropertyNames() throws JMSException {
        return Collections.emptyEnumeration();
    }

    @Override
    public void setBooleanProperty(String name, boolean value) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setByteProperty(String name, byte value) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setShortProperty(String name, short value) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setIntProperty(String name, int value) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLongProperty(String name, long value) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFloatProperty(String name, float value) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDoubleProperty(String name, double value) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setStringProperty(String name, String value) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setObjectProperty(String name, Object value) throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void acknowledge() throws JMSException {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearBody() throws JMSException {
        // TODO Auto-generated method stub

    }

}
