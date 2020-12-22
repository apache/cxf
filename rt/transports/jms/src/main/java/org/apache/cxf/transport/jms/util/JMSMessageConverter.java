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
package org.apache.cxf.transport.jms.util;

import java.io.Serializable;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;

/**
 * Converts jms messages to Objects and vice a versa.
 * <p>
 * String <=> {@link jakarta.jms.TextMessage}
 * byte[] <=> {@link jakarta.jms.BytesMessage}
 * Serializable object <=> {@link jakarta.jms.ObjectMessage}
 */
public class JMSMessageConverter {

    public Message toMessage(Object object, Session session) throws JMSException {
        if (object instanceof Message) {
            return (Message)object;
        } else if (object instanceof String) {
            return session.createTextMessage((String)object);
        } else if (object instanceof byte[]) {
            BytesMessage message = session.createBytesMessage();
            message.writeBytes((byte[])object);
            return message;
        } else if (object instanceof Serializable) {
            return session.createObjectMessage((Serializable)object);
        } else {
            throw new IllegalArgumentException("Unsupported type " + nullSafeClassName(object) + "."
                                               + " Valid types are: String, byte[], Serializable object.");
        }
    }

    private String nullSafeClassName(Object object) {
        return object == null ? "null" : object.getClass().getName();
    }

    public Object fromMessage(Message message) throws JMSException {
        if (message instanceof TextMessage) {
            return ((TextMessage)message).getText();
        } else if (message instanceof BytesMessage) {
            BytesMessage message1 = (BytesMessage)message;
            byte[] bytes = new byte[(int)message1.getBodyLength()];
            message1.readBytes(bytes);
            return bytes;
        } else if (message instanceof ObjectMessage) {
            return ((ObjectMessage)message).getObject();
        } else if (message instanceof StreamMessage) {
            StreamMessage streamMessage = (StreamMessage)message;
            return streamMessage.readObject();
        } else {
            return new byte[]{};
        }
    }

}
