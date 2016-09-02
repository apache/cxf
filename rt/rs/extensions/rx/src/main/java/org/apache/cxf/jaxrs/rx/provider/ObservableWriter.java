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
package org.apache.cxf.jaxrs.rx.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.ParameterizedCollectionType;

import rx.Observable;

@Provider
public class ObservableWriter<T> implements MessageBodyWriter<Observable<T>> {
    
    @Context
    private Providers providers;
    private boolean writeSingleElementAsList;
    
    @Override
    public long getSize(Observable<T> arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        // TODO Auto-generated method stub
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return true;
    }

    @Override
    public void writeTo(Observable<T> obs, Class<?> cls, Type t, Annotation[] anns, MediaType mt,
                        MultivaluedMap<String, Object> headers, OutputStream os)
                            throws IOException, WebApplicationException {
        List<T> entities = new LinkedList<T>();
        obs.subscribe(value -> entities.add(value),
            throwable -> throwError(throwable));
        if (!entities.isEmpty()) {
            
            if (entities.get(0) instanceof List) {
                List<T> allEntities = new LinkedList<T>();
                for (T obj : entities) {
                    @SuppressWarnings("unchecked")
                    List<T> listT = (List<T>)obj;
                    allEntities.addAll(listT);
                }
                writeToOutputStream(allEntities, anns, mt, headers, os);
            } else if (entities.size() > 1 || writeSingleElementAsList) {
                writeToOutputStream(entities, anns, mt, headers, os);
            } else {
                writeToOutputStream(entities.get(0), anns, mt, headers, os);
            }
        }
    }

    private void writeToOutputStream(Object value,
                                     Annotation[] anns,
                                     MediaType mt,
                                     MultivaluedMap<String, Object> headers, 
                                     OutputStream os) {
        Class<?> valueCls = value.getClass();
        Type valueType = null;
        if (value instanceof List) {
            List<?> list = (List<?>)value;
            valueType = new ParameterizedCollectionType(list.isEmpty() ? Object.class : list.get(0).getClass());
        } else {
            valueType = valueCls;
        }
        @SuppressWarnings("unchecked")
        MessageBodyWriter<Object> writer = 
            (MessageBodyWriter<Object>)providers.getMessageBodyWriter(valueCls, valueType, anns, mt);
        if (writer == null) {
            throwError(null);
        }
    
        try {
            writer.writeTo(value, valueCls, valueType, anns, mt, headers, os);    
        } catch (IOException ex) {
            throwError(ex);
        }
    }
    
    private static void throwError(Throwable cause) {
        throw ExceptionUtils.toInternalServerErrorException(cause, null);
    }

    public void setWriteSingleElementAsList(boolean writeSingleElementAsList) {
        this.writeSingleElementAsList = writeSingleElementAsList;
    }

}
