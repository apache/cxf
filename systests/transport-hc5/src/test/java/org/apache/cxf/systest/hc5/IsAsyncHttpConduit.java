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
package org.apache.cxf.systest.hc5;

import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.asyncclient.hc5.AsyncHTTPConduit;
import org.apache.cxf.transport.http.asyncclient.hc5.URLConnectionAsyncHTTPConduit;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IsAsyncHttpConduit extends TypeSafeMatcher<Object> {
    public void describeTo(Description description) {
        if (HTTPTransportFactory.isForceURLConnectionConduit()) {
            description.appendText("instance of URLConnectionAsyncHTTPConduit async conduit ");
        } else {
            description.appendText("instance of AsyncHTTPConduit async conduit ");
        }
    }

    @Override
    protected boolean matchesSafely(Object item) {
        return isAsyncConduit(item);
    }

    public static IsAsyncHttpConduit isInstanceOfAsyncHttpConduit() {
        return new IsAsyncHttpConduit();
    }
    
    private static boolean isAsyncConduit(Object instance) {
        if (HTTPTransportFactory.isForceURLConnectionConduit()) {
            return instance instanceof URLConnectionAsyncHTTPConduit;
        } else {
            return instance instanceof AsyncHTTPConduit;
        }
    }
}
