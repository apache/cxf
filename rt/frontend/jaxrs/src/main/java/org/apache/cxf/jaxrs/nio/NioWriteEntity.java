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
package org.apache.cxf.jaxrs.nio;

public final class NioWriteEntity {
    private final NioWriteHandler writer;
    private final NioErrorHandler error;

    public NioWriteEntity(final NioWriteHandler writer) {
        this(writer, null);
    }
    
    public NioWriteEntity(final NioWriteHandler writer, final NioErrorHandler error) {
        this.writer = writer;
        this.error = error;
    }

    public NioWriteHandler getWriter() {
        return writer;
    }

    public NioErrorHandler getError() {
        return error;
    }
}

