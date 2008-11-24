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

package org.apache.cxf.jaxrs.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;

@Consumes("application/x-www-form-urlencoded")
@Provider
public final class FormEncodingReaderProvider implements MessageBodyReader<Object> {

    private static final char SPACE_CHAR = '+';
    private static final String NEXT_LINE = "%0D%0A";
    
    
    private boolean decode;
    private FormValidator validator;
    
    public void setDecode(boolean formDecoding) {
        decode = formDecoding;
    }
    
    public void setValidator(FormValidator formValidator) {
        validator = formValidator;
    }
    
    public boolean isReadable(Class<?> type, Type genericType, 
                              Annotation[] annotations, MediaType mt) {
        return MultivaluedMap.class.isAssignableFrom(type);
    }

    public MultivaluedMap<String, String> readFrom(
        Class<Object> clazz, Type genericType, Annotation[] annotations, MediaType type, 
        MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        try {

            String charset = "UTF-8";

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(is, bos, 1024);
            String postBody = new String(bos.toByteArray(), charset);

            MultivaluedMap<String, String> params = createMap(clazz);
            populateMap(params, postBody);
            validateMap(params);
            return params;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    public static void copy(final InputStream input, final OutputStream output, final int bufferSize)
        throws IOException {
        final byte[] buffer = new byte[bufferSize];
        int n = 0;
        n = input.read(buffer);
        while (-1 != n) {
            output.write(buffer, 0, n);
            n = input.read(buffer);
        }
    }

    @SuppressWarnings("unchecked")
    protected MultivaluedMap<String, String> createMap(Class<?> clazz) throws Exception {
        if (clazz == MultivaluedMap.class) {
            return new MetadataMap<String, String>();
        }
        return (MultivaluedMap<String, String>)clazz.newInstance();
    }
    
    /**
     * Retrieve map of parameters from the passed in message
     *
     * @param message
     * @return a Map of parameters.
     */
    protected void populateMap(MultivaluedMap<String, String> params, 
                               String body) {
        if (!StringUtils.isEmpty(body)) {
            List<String> parts = Arrays.asList(body.split("&"));
            for (String part : parts) {
                String[] keyValue = part.split("=");
                // Change to add blank string if key but not value is specified
                if (keyValue.length == 2) {
                    if (decode) {
                        String[] values = keyValue[1].split(NEXT_LINE);
                        for (String value : values) {
                            params.add(keyValue[0], 
                                       value.replace(SPACE_CHAR, ' '));
                        }
                    } else {
                        params.add(keyValue[0], keyValue[1]);
                    }
                } else {
                    params.add(keyValue[0], "");
                }
            }
        }
    }
    
    protected void validateMap(MultivaluedMap<String, String> params) {
        if (validator != null) {
            validator.validate(params);
        }
    }
}
