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
package demo.jaxrs.server.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

public class SparkStreamingOutput implements StreamingOutput {
    private BufferedReader sparkInputStream;
    public SparkStreamingOutput(BufferedReader sparkInputStream) {
        this.sparkInputStream = sparkInputStream;
    }

    @Override
    public void write(final OutputStream output) throws IOException, WebApplicationException {
        PrintStream printStream = new PrintStream(output, true);
        String s = null;
        while ((s = sparkInputStream.readLine()) != null) {
            if ("<batchEnd>".equals(s)) {
                break;
            }
            printStream.println(s);
        }
        
    }
}
