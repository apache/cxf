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
package org.apache.cxf.ant.extensions;

import java.io.File;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;

public class CxfAntTask extends Task {
    protected boolean verbose;
    protected CommandlineJava cmd = new CommandlineJava();
    protected File classesDir;
    protected File sourcesDir;

    public void setFork(boolean b) {
        //we always fork, but the silly TCK requires this param for some reason
    }
    public void setKeep(boolean b) {
        //we always "keep", but TCK requires this flag
    }
    public void setDebug(boolean b) {
        // TCK requires this flag
    }

    public void setVerbose(boolean b) {
        verbose = b;
    }

    public Commandline.Argument createJvmarg() {
        return cmd.createVmArgument();
    }

    public void setSourceDestDir(File f) {
        sourcesDir = f;
    }

    public void setDestdir(File c) {
        classesDir = c;
    }

}