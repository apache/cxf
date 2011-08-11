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

package org.apache.cxf.ws.rm.persistence;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.ws.rm.SequenceAcknowledgement;

/**
 * 
 */
public final class PersistenceUtils {
    
    private static PersistenceUtils instance;
    private JAXBContext context;
    private Unmarshaller unmarshaller;
    private Marshaller marshaller;
    

    /**
     * Prevents instantiation.
     */
    private PersistenceUtils() {
    }
    
    public static PersistenceUtils getInstance() {
        if (null == instance) {
            instance = new PersistenceUtils();
        }
        return instance;
    }
    
    public SequenceAcknowledgement deserialiseAcknowledgment(InputStream is) {
        Object obj = null;
        try {
            obj = getUnmarshaller().unmarshal(is);
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> el = (JAXBElement<?>)obj;
                obj = el.getValue();
            }
        } catch (JAXBException ex) {
            throw new RMStoreException(ex);
        }
        return (SequenceAcknowledgement)obj;
    }
    
    public InputStream serialiseAcknowledgment(SequenceAcknowledgement ack) {
        LoadingByteArrayOutputStream bos = new LoadingByteArrayOutputStream(); 
        try {
            getMarshaller().marshal(ack, bos);
        } catch (JAXBException ex) {
            throw new RMStoreException(ex);
        }
        return bos.createInputStream();
    }
    
    private JAXBContext getContext() throws JAXBException {
        if (null == context) {
            context = JAXBContext.newInstance(PackageUtils
                .getPackageName(SequenceAcknowledgement.class), 
                getClass().getClassLoader()); 
        }
        return context;
    }
      
    private Unmarshaller getUnmarshaller() throws JAXBException {
        if (null == unmarshaller) {
            unmarshaller = getContext().createUnmarshaller();
        }
        return unmarshaller;
    }
    
    private Marshaller getMarshaller() throws JAXBException {
        if (null == marshaller) {
            marshaller = getContext().createMarshaller();
        }
        return marshaller;
    }
}
