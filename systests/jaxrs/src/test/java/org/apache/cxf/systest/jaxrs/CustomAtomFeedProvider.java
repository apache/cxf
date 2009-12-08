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
package org.apache.cxf.systest.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.abdera.model.Feed;
import org.apache.cxf.jaxrs.provider.AtomFeedProvider;

@Produces({"application/atom+xml", "application/atom+xml;type=feed", "application/json" })
@Consumes({"application/atom+xml", "application/atom+xml;type=feed" })
@Provider
public class CustomAtomFeedProvider extends AtomFeedProvider {
    
    @Override
    public void writeTo(Feed element, Class<?> clazz, Type type, Annotation[] a, 
                        MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os) 
        throws IOException {
        os.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>".getBytes());
        super.writeTo(element, clazz, type, a, mt, headers, os);
    }
}
