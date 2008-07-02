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
package org.apache.cxf.jca.core.resourceadapter;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

public class ResourceAdapterInternalException extends javax.resource.spi.ResourceAdapterInternalException {

    private static final long serialVersionUID = 6769505138041263456L;
    private static final String REASON_PREFIX = ", reason: ";
    private static final Logger LOGGER = LogUtils.getL7dLogger(ResourceAdapterInternalException.class);

    public ResourceAdapterInternalException(String msg) {
        this(msg, null);
    }

    public ResourceAdapterInternalException(String msg, Throwable cause) {
        super(msg + ResourceAdapterInternalException.optionalReasonFromCause(cause));
        setCause(cause);
        if (cause != null) {
            if (null != LOGGER.getLevel()
                 && LOGGER.getLevel().intValue() < Level.INFO.intValue()) {
                cause.printStackTrace();
            }
            LOGGER.warning(cause.toString());
        }
    }

    private static String optionalReasonFromCause(Throwable cause) {
        String reason = "";
        if (cause != null) {
            if (cause instanceof InvocationTargetException) {
                reason = REASON_PREFIX + ((InvocationTargetException)cause).getTargetException();
            } else {
                reason = REASON_PREFIX + cause;
            }
        }
        return reason;
    }

    private void setCause(Throwable cause) {
        if (getCause() != null) {
            return;
        }

        if (cause instanceof InvocationTargetException
            && (((InvocationTargetException)cause).getTargetException() != null)) {
            initCause(((InvocationTargetException)cause).getTargetException());
        } else {
            initCause(cause);
        }
    }


    public Exception getLinkedException() {
        Exception linkedEx = null;
        if (getCause() instanceof Exception) {
            linkedEx = (Exception)getCause();
        }
        return linkedEx;
    }

}
