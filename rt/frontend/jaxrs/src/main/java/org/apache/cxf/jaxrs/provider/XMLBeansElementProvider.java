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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamReader;

import org.apache.xmlbeans.XmlObject;

/**
 * Provider for XMLBeans data objects.
 */
@ProduceMime("application/xml")
@ConsumeMime("application/xml")
@Provider
public class XMLBeansElementProvider implements MessageBodyReader<XmlObject>, MessageBodyWriter<XmlObject> {

    /** {@inheritDoc} */
    public XmlObject readFrom(Class<XmlObject> type, Type genericType, 
                              Annotation[] annotations, MediaType m,  
                     MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {

        return parseXmlBean(type, is);
    }

    /** {@inheritDoc} */
    public void writeTo(XmlObject t, Class<?> cls, Type genericType, Annotation[] annotations,  
                        MediaType m, MultivaluedMap<String, Object> headers, OutputStream entityStream)
        throws IOException {

        // May need to set some XMLOptions here
        t.save(entityStream);
    }

    /** {@inheritDoc} */
    public long getSize(XmlObject t) {
        // return length not known
        return -1;
    }

    /** {@inheritDoc} */
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations) {
        return isXmlBean(type);
    }

    /** {@inheritDoc} */
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations) {
        return isXmlBean(type);
    }

    /**
     * Create an XMLBean object from an XML stream.
     * 
     * @param type declared type of the target object
     * @param reader stream reader for the XML stream
     * @return an XMLBean data object, or none if unable to process
     */
    protected XmlObject parseXmlBean(Class type, XMLStreamReader reader) {

        XmlObject result = null;

        // get XMLBeans inner class Factory
        Class<?> factory = getFactory(type);

        try {

            // find and invoke method parse(InputStream)
            Method m = factory.getMethod("parse", reader.getClass());
            Object[] args = {
                reader
            };
            Object obj = m.invoke(type, args);

            if (obj instanceof XmlObject) {
                result = (XmlObject)obj;
            }

        } catch (NoSuchMethodException nsme) {
            throw new WebApplicationException(HttpURLConnection.HTTP_INTERNAL_ERROR);
        } catch (InvocationTargetException ite) {
            throw new WebApplicationException(HttpURLConnection.HTTP_INTERNAL_ERROR);
        } catch (IllegalAccessException iae) {
            throw new WebApplicationException(HttpURLConnection.HTTP_INTERNAL_ERROR);
        }

        return result;
    }

    /**
     * Create an XMLBean data object from an <code>InputStream</code>
     * 
     * @param type declared type of the required object
     * @param inStream
     * @return an XMLBean object if successful, otherwise null
     */
    protected XmlObject parseXmlBean(Class type, InputStream inStream) {
        XmlObject result = null;

        Reader r = new InputStreamReader(inStream);

        // delegate to parseXmlBean(Class type, Reader reader)
        result = parseXmlBean(type, r);

        return result;
    }

    /**
     * Create an XMLBean data object using a stream <code>Reader</code>
     * 
     * @param type declared type of the desired XMLBean data object
     * @param reader
     * @return an instance of the required object, otherwise null
     */
    protected XmlObject parseXmlBean(Class type, Reader reader) {
        XmlObject result = null;

        Class<?> factory = getFactory(type);

        try {

            // get factory method parse(InputStream)
            Method m = factory.getMethod("parse", Reader.class);
            Object[] args = {reader};
            Object obj = m.invoke(type, args);

            if (obj instanceof XmlObject) {
                result = (XmlObject)obj;
            }

        } catch (NoSuchMethodException nsme) {
            // do nothing, just return null
        } catch (InvocationTargetException ite) {
            // do nothing, just return null
        } catch (IllegalAccessException iae) {
            // do nothing, just return null
        }

        return result;
    }

    /**
     * Locate the XMLBean <code>Factory</code> inner class.
     * 
     * @param type
     * @return the Factory class if present, otherwise null.
     */
    private Class getFactory(Class type) {
        Class result = null;

        Class[] interfaces = type.getInterfaces();

        // look for XMLBeans inner class Factory
        for (Class inter : interfaces) {

            Class[] declared = inter.getDeclaredClasses();

            for (Class c : declared) {

                if (c.getSimpleName().equals("Factory")) {
                    result = c;
                }
            }
        }

        return result;
    }

    /**
     * Check if a <code>Class</code> is a valid XMLBeans data object. The check procedure involves looking
     * for the Interface <code>XmlObject</code> in the target type's declaration. Assumed to be sufficient
     * to identify the type as an XMLBean. From the javadoc (2.3.0) for XmlObject: "Corresponds to the XML
     * Schema xs:anyType, the base type for all XML Beans."
     * 
     * @param type
     * @return true if found to be an XMLBean object, otherwise false
     */
    protected boolean isXmlBean(Class type) {
        boolean result = false;

        Class[] interfaces = {type};

        if (!type.isInterface()) {

            interfaces = type.getInterfaces();
        }

        for (Class i : interfaces) {

            Class[] superInterfaces = i.getInterfaces();

            for (Class superI : superInterfaces) {

                if (superI.getName().equals("org.apache.xmlbeans.XmlObject")) {
                    result = true;
                }
            }
        }

        return result;
    }
}
