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
package demo.jaxrs.server.simple;

import java.util.List;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;

public class StringListReceiver extends Receiver<String> {

    private static final long serialVersionUID = 1L;
    private List<String> inputStrings;

    public StringListReceiver(List<String> inputStrings) {
        super(StorageLevel.MEMORY_ONLY());
        this.inputStrings = inputStrings;
    }
    @Override
    public void onStart() {
        super.store(inputStrings.iterator());
    }
    @Override
    public void onStop() {
        // complete
    }

}
