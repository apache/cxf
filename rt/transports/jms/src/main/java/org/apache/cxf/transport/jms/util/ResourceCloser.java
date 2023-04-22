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
package org.apache.cxf.transport.jms.util;

import java.io.Closeable;
import java.util.AbstractSequentialList;
import java.util.LinkedList;

import javax.naming.Context;

import jakarta.jms.Connection;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;

public class ResourceCloser implements Closeable, AutoCloseable {
    private AbstractSequentialList<Object> resources;

    public ResourceCloser() {
        resources = new LinkedList<>();
    }

    public <E> E register(E resource) {
        resources.add(0, resource);
        return resource;
    }

    @Override
    public void close() {
        for (Object resource : resources) {
            close(resource);
        }
        resources.clear();
    }

    public void close(Object ...resources2) {
        for (Object resource : resources2) {
            close(resource);
        }
    }

    public static void close(Object resource) {
        if (resource == null) {
            return;
        }
        try {
            if (resource instanceof MessageProducer) {
                ((MessageProducer)resource).close();
            } else if (resource instanceof MessageConsumer) {
                ((MessageConsumer)resource).close();
            } else if (resource instanceof Session) {
                ((Session)resource).close();
            } else if (resource instanceof Connection) {
                ((Connection)resource).close();
            } else if (resource instanceof Context) {
                ((Context)resource).close();
            } else {
                throw new IllegalArgumentException("Can not handle resource " + resource.getClass());
            }
        } catch (Exception e) {
            // Ignore
        }
    }

}
