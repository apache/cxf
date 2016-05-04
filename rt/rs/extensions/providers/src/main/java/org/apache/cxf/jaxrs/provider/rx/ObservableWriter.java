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
package org.apache.cxf.jaxrs.provider.rx;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;

import rx.Observable;

public class ObservableWriter<T> implements MessageBodyWriter<Observable<T>> {
    
    @Context
    private Providers providers;
    
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
        obs.subscribe(value -> writeToOutputStream(value, anns, mt, headers, os),
            throwable -> throwError(throwable));   
    }

    private void writeToOutputStream(T value,
                                     Annotation[] anns,
                                     MediaType mt,
                                     MultivaluedMap<String, Object> headers, 
                                     OutputStream os) {
        @SuppressWarnings("unchecked")
        MessageBodyWriter<T> writer = 
            (MessageBodyWriter<T>)providers.getMessageBodyWriter(value.getClass(), value.getClass(), anns, mt);
        if (writer == null) {
            throwError(null);
        }
    
        try {
            writer.writeTo(value, value.getClass(), value.getClass(), anns, mt, headers, os);    
        } catch (IOException ex) {
            throwError(ex);
        }
    }
    
    private static void throwError(Throwable cause) {
        throw ExceptionUtils.toInternalServerErrorException(cause, null);
    }

}
