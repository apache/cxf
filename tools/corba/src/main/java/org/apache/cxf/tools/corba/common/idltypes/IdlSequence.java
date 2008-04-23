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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;


public final class IdlSequence extends IdlSequenceBase {
    private static final Logger LOG = LogUtils.getL7dLogger(IdlSequence.class);

    private IdlSequence(IdlScopeBase parent, String name, IdlType elem, int bound) {
        super(parent, name, elem, bound);
    }
    
    public static IdlSequence create(IdlScopeBase parent, String name, IdlType elem) {
        return new IdlSequence(parent, name, elem, 0);
    }


    public static IdlSequence create(IdlScopeBase parent, String name, IdlType elem, int bound) {
        return new IdlSequence(parent, name, elem, bound);
    }


    public void write(PrintWriter pw) {
        if (!elemType().isEmptyDef()) {
            if (!isCircular()) {
                writeTypedef(pw);
            }
        } else {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Ignoring Sequence " + localName() + " with Empty Element Type.");
            }
        }
    }

    
    public void writeFwd(PrintWriter pw) {
        if (!elemType().isEmptyDef() && isCircular()) {
            writeTypedef(pw);
        }
    }


    private void writeTypedef(PrintWriter pw) {
        if (!elemType().isEmptyDef()) {
            pw.print(indent() + "typedef ");
            super.write(pw);
            pw.println(localName() + ";");
        } else {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Ignoring Sequence " + localName() + " with Empty Element Type.");
            }
        }
    }
    
}
