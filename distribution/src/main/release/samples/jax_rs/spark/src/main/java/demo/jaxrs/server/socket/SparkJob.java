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
import java.io.PrintStream;
import java.util.List;

import javax.ws.rs.container.AsyncResponse;

public class SparkJob implements Runnable {
    private AsyncResponse ac;
    private BufferedReader sparkInputStream;
    private PrintStream sparkOutputStream;
    private List<String> inputStrings;
    public SparkJob(AsyncResponse ac, BufferedReader sparkInputStream,
                          PrintStream sparkOutputStream, List<String> inputStrings) {
        this.ac = ac;
        this.inputStrings = inputStrings;
        this.sparkInputStream = sparkInputStream;
        this.sparkOutputStream = sparkOutputStream;
    }
    @Override
    public void run() {
        for (String s : inputStrings) {
            sparkOutputStream.println(s);
        }
        ac.resume(new SparkStreamingOutput(sparkInputStream));
    }

}
