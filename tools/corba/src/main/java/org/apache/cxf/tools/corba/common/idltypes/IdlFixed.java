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

package org.apache.cxf.tools.corba.common.idltypes;

import java.io.PrintWriter;

public final class IdlFixed extends IdlFixedBase {

    private IdlFixed(IdlScopeBase parent, String name, int digits, int scale) {
        super(parent, name, digits, scale);
    }

    public static IdlFixed create(IdlScopeBase parent, String name, int digits, int scale) {
        return new IdlFixed(parent, name, digits, scale);
    }


    public void write(PrintWriter pw) {
        pw.print(indent() + "typedef ");
        super.write(pw);
        pw.println(localName() + ";");
    }

}