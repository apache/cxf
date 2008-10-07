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

package demo.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Client {

    protected Client() {
    }


    public static void main(String[] args) throws Exception {

        invokeServlet("sayHi", null);
        invokeServlet("greetMe", "CXF");
    }

    public static void invokeServlet(String op, String user) throws Exception {
        boolean isSayHi = true;
        try {
            if (!"sayHi".equals(op)) {
                isSayHi = false;
            }

            /*
             * Default port for JBoss is 8080
             * URL url = new URL("http://localhost:8080/helloworld/*.do");
             */
            URL url = new URL("http://localhost:9080/helloworld/*.do");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
                
            BufferedWriter out =
                    new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                
            out.write("Operation=" + op);
            if (!isSayHi) {
                out.write("&User=" + user);                
            }
            out.flush();
            out.close();
                
            BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
            
            boolean correctReturn = false;
            String response;
            if (isSayHi) {
                while ((response = in.readLine()) != null) {
                    
                    if (response.contains("Bonjour")) {
                        System.out.println(" sayHi server return: Bonjour");
                        correctReturn = true;
                        break;
                    }
                }
            } else {
                while ((response = in.readLine()) != null) {
                    if (response.contains("Hello CXF")) {
                        System.out.println(" greetMe server return: Hello CXF");
                        correctReturn = true;
                        break;
                    }
                }
            }

            if (!correctReturn) {
                System.out.println("Can't got correct return from server.");
            }
            
            in.close();
            
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
