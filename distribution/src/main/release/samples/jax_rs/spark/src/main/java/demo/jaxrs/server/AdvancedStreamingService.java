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

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.StreamingOutput;

import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.dstream.ReceiverInputDStream;
import org.apache.spark.streaming.receiver.Receiver;

import scala.reflect.ClassTag;

// INCOMPLETE

@Path("/")
public class AdvancedStreamingService {
    private JavaStreamingContext jssc;
    public AdvancedStreamingService(SparkConf sparkConf) {
        this.jssc = new JavaStreamingContext(sparkConf, Durations.seconds(1));
        new MyReceiverInputDStream(jssc.ssc(), 
                                   scala.reflect.ClassTag$.MODULE$.apply(String.class));
    }
    
    @POST
    @Path("/stream")
    @Consumes("text/plain")
    @Produces("text/plain")
    public StreamingOutput getStream(InputStream is) {
        
        return null;
    }

     
    public static class MyReceiverInputDStream extends ReceiverInputDStream<String> {

        public MyReceiverInputDStream(StreamingContext ssc_, ClassTag<String> evidence$1) {
            super(ssc_, evidence$1);
        }
        public void putInputStream(InputStream is) {
            
        }
        @Override
        public Receiver<String> getReceiver() {
            // A receiver can be created per every String the input stream
            return new InputStreamReceiver(getInputStream());
        }
        public InputStream getInputStream() {
            // TODO Auto-generated method stub
            return null;
        }    
    }


    
}
