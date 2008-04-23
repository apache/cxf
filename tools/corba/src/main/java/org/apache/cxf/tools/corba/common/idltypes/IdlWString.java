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

public final class IdlWString extends IdlDefnImplBase implements IdlType {
    private int bound;

    private IdlWString() {
        super(null, "wstring");
        bound = 0;
    }
    
    private IdlWString(int range) {
        super(null, "wstring<" + range + "> ");
        this.bound = range;
    }
    
    public static IdlWString create() {
        return new IdlWString();
    }


    static IdlWString create(int bound) {
        IdlWString result;

        if (bound == 0) {
            result = new IdlWString();
        } else {
            result = new IdlWString(bound);
        }

        return result;
    }


    public void write(PrintWriter pw) {
        pw.print("wstring");

        if (bound != 0) {
            pw.print("<" + bound + "> ");
        }
    }    
    
}
