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
/*
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.cxf.transport.https.httpclient;

final class SubjectName {

    static final int DNS = 2;
    static final int IP = 7;

    private final String value;
    private final int type;

    SubjectName(final String value, final int type) {
        if (value == null) {
            throw new IllegalArgumentException("Value is null");
        }
        this.value = value;

        if (type <= 0) {
            throw new IllegalArgumentException("Type must not be negative or zero: " + type);
        }
        this.type = type;
    }

    //CHECKSTYLE:OFF
    static SubjectName IP(final String value) {
        return new SubjectName(value, IP);
    }

    static SubjectName DNS(final String value) {
        return new SubjectName(value, DNS);
    }
    //CHECKSTYLE:ON

    public int getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

}
