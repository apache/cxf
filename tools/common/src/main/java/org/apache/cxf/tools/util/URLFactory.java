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

package org.apache.cxf.tools.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.StringTokenizer;

public final class URLFactory {
    public static final String PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs";
    public static final String UNKNOWN_PROPTOCL_EX_MSG = "unknown protocol: ";

    private URLFactory() {
        
    }
    
    public static URL createURL(String spec) throws MalformedURLException {
        return createURL(null, spec);
    }

    public static URL createURL(URL context, String spec) throws MalformedURLException {
        URL url = null;
        try {
            url = new URL(context, spec);
        } catch (MalformedURLException mue) {

            String msg = mue.getMessage();
            if (msg.indexOf(UNKNOWN_PROPTOCL_EX_MSG) != -1) {
                URLStreamHandler handler = findHandler(msg.substring(UNKNOWN_PROPTOCL_EX_MSG.length()));
                if (handler != null) {
                    url = new URL(context, spec, handler);
                }
            }
            if (url == null) {
                throw mue;
            }
        }
        return url;
    }

    public static URLStreamHandler findHandler(String protocol) {

        URLStreamHandler handler = null;
        String packagePrefixList = System.getProperty(PROTOCOL_HANDLER_PKGS, "");
        StringTokenizer packagePrefixIter = new StringTokenizer(packagePrefixList, "|");
        while (handler == null && packagePrefixIter.hasMoreTokens()) {
            String packagePrefix = packagePrefixIter.nextToken().trim();
            try {
                String clsName = packagePrefix + "." + protocol + ".Handler";
                Class cls = null;
                try {
                    cls = Class.forName(clsName);
                } catch (ClassNotFoundException e) {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    if (cl != null) {
                        cls = cl.loadClass(clsName);
                    }
                }
                if (cls != null) {
                    handler = (URLStreamHandler)cls.newInstance();
                }
            } catch (Exception ignored) {
                ignored.getMessage();
            }
        }
        return handler;
    }
}
