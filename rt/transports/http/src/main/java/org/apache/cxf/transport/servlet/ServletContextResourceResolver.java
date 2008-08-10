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

package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;


import org.apache.cxf.resource.ResourceResolver;


public class ServletContextResourceResolver implements ResourceResolver {
    ServletContext servletContext;
    Map<String, URL> urlMap = new ConcurrentHashMap<String, URL>();

    public ServletContextResourceResolver(ServletContext sc) {
        servletContext = sc;
    }


    public final InputStream getAsStream(final String string) {
        if (urlMap.containsKey(string)) {
            try {
                return urlMap.get(string).openStream();
            } catch (IOException e) {
                //ignore
            }
        }
        return servletContext.getResourceAsStream(string);
    }

    public final <T> T resolve(final String entryName, final Class<T> clz) {

        Object obj = null;
        try {
            if (entryName != null) {
                InitialContext ic = new InitialContext();
                obj = ic.lookup(entryName);
            }
        } catch (NamingException e) {
            //do nothing
        }

        if (obj != null && clz.isInstance(obj)) {
            return clz.cast(obj);
        }

        if (clz.isAssignableFrom(URL.class)) {
            if (urlMap.containsKey(entryName)) {
                return clz.cast(urlMap.get(entryName));
            }
            try {
                URL url = servletContext.getResource(entryName);
                if (url != null) {
                    urlMap.put(url.toString(), url);
                    return clz.cast(url);
                }
            } catch (MalformedURLException e) {
                //fallthrough
            }
            try {
                URL url = servletContext.getResource("/" + entryName);
                if (url != null) {
                    urlMap.put(url.toString(), url);
                    return clz.cast(url);
                }
            } catch (MalformedURLException e1) {
                //ignore
            }
        } else if (clz.isAssignableFrom(InputStream.class)) {
            return clz.cast(getAsStream(entryName));
        }
        return null;
    }
}
