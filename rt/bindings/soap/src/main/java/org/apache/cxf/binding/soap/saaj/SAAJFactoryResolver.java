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

package org.apache.cxf.binding.soap.saaj;

import java.lang.reflect.InvocationTargetException;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.util.SystemPropertyAction;

/**
 *
 */
public final class SAAJFactoryResolver {

    public static final String MESSAGE_FACTORY_KEY
        = "org.apache.cxf.binding.soap.messageFactoryClassName";
    public static final String SOAP_FACTORY_KEY
        = "org.apache.cxf.binding.soap.soapFactoryClassName";

    private SAAJFactoryResolver() {
        //utility class
    }

    public static MessageFactory createMessageFactory(SoapVersion version) throws SOAPException {
        MessageFactory messageFactory;
        String messageFactoryClassName = SystemPropertyAction.getPropertyOrNull(MESSAGE_FACTORY_KEY);
        if (messageFactoryClassName != null) {
            messageFactory = newInstanceCxfSAAJFactory(messageFactoryClassName,
                                                          MessageFactory.class);
        } else if (version instanceof Soap11) {
            try {
                messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            } catch (Throwable t) {
                messageFactory = MessageFactory.newInstance();
            }
        } else if (version instanceof Soap12) {
            try {
                messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            } catch (Throwable t) {
                messageFactory = MessageFactory.newInstance();
            }
        } else {
            messageFactory = MessageFactory.newInstance();
        }
        return messageFactory;
    }

    public static SOAPFactory createSOAPFactory(SoapVersion version) throws SOAPException {
        SOAPFactory soapFactory;
        String soapFactoryClassName = SystemPropertyAction.getPropertyOrNull(SOAP_FACTORY_KEY);
        if (soapFactoryClassName != null) {
            soapFactory = newInstanceCxfSAAJFactory(soapFactoryClassName,
                                                       SOAPFactory.class);
        } else if (version instanceof Soap11) {
            try {
                soapFactory = SOAPFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            } catch (Throwable t) {
                soapFactory = SOAPFactory.newInstance();
            }
        } else if (version instanceof Soap12) {
            try {
                soapFactory = SOAPFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            } catch (Throwable t) {
                soapFactory = SOAPFactory.newInstance();
            }
        } else {
            soapFactory = SOAPFactory.newInstance();
        }
        return soapFactory;
    }

    private static <T> T newInstanceCxfSAAJFactory(String factoryName, Class<T> cls)
        throws SOAPException {
        try {
            Class<?> klass = Class.forName(factoryName);
            return cls.cast(klass.getDeclaredConstructor().newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                 | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            throw new SOAPException("Provider " + factoryName + " could not be instantiated: " + ex, ex);
        }
    }

}
