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

package org.apache.cxf.metrics.micrometer.provider;

import java.util.Optional;

import org.apache.cxf.message.Exchange;

public class DefaultExceptionClassProvider implements ExceptionClassProvider {

    @Override
    public Class<?> getExceptionClass(Exchange ex, boolean client) {
        return getFault(ex, client).map(Throwable::getCause).map(Throwable::getClass).orElse(null);
    }

    private Optional<Throwable> getFault(Exchange ex, boolean client) {
        Exception exception = ex.get(Exception.class);
        
        if (client) {
            if (exception == null && ex.getInFaultMessage() != null) {
                exception = ex.getInFaultMessage().get(Exception.class);
            }
            if (exception == null && ex.getOutMessage() != null) {
                exception = ex.getOutMessage().get(Exception.class);
            }
        } else {
            if (exception == null && ex.getOutFaultMessage() != null) {
                exception = ex.getOutFaultMessage().get(Exception.class);
            }
            if (exception == null && ex.getInMessage() != null) {
                exception = ex.getInMessage().get(Exception.class);
            }
        }
        
        return Optional.ofNullable(exception);
    }
}
