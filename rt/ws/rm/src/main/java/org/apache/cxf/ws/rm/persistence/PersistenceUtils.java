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
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;

/**
 * 
 */
public final class PersistenceUtils {
    
    private static PersistenceUtils instance;
    private JAXBContext context;

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
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        try {
            obj = getContext().createUnmarshaller().unmarshal(reader);
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> el = (JAXBElement<?>)obj;
                obj = el.getValue();
            }
        } catch (JAXBException ex) {
            throw new RMStoreException(ex);
        } finally {
            try {
                StaxUtils.close(reader);
                is.close();
            } catch (Throwable t) {
                //ignore, just cleaning up
            }
        }
        return (SequenceAcknowledgement)obj;
    }
    
    public InputStream serialiseAcknowledgment(SequenceAcknowledgement ack) {
        LoadingByteArrayOutputStream bos = new LoadingByteArrayOutputStream(); 
        try {
            getContext().createMarshaller().marshal(ack, bos);
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
}
