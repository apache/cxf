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
package demo.jaxrs.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;

public class InputStreamReceiver extends Receiver<String> {

    private static final long serialVersionUID = 1L;
    private List<String> inputStrings = new LinkedList<String>();
    
    public InputStreamReceiver(InputStream is) {
        super(StorageLevel.MEMORY_ONLY());
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String userInput = null;
        // Receiver is meant to be serializable, but it would be
        // great if if we could avoid copying InputStream
        // TODO: submit Spark enhancement request so that it can keep streaming from 
        // the incoming InputStream to its processing nodes ?
        while ((userInput = readLine(reader)) != null) {
            inputStrings.add(userInput);
        }
    }
    @Override
    public void onStart() {
        super.store(inputStrings.iterator());
    }

    private String readLine(BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException ex) {
            throw new WebApplicationException(500);
        }
    }
    @Override
    public void onStop() {
        // complete
    }
    
}
