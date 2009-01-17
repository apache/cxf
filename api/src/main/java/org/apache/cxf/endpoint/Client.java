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

package org.apache.cxf.endpoint;

import java.util.Map;
import java.util.concurrent.Executor;

import javax.xml.namespace.QName;

import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;

public interface Client extends InterceptorProvider, MessageObserver {
    String REQUEST_CONTEXT = "RequestContext";
    String RESPONSE_CONTEXT = "ResponseContext";
    String KEEP_CONDUIT_ALIVE = "KeepConduitAlive";

    /**
     * Invokes an operation synchronously
     * @param operationName The name of the operation to be invoked. The service namespace will be used
     * when looking up the BindingOperationInfo.
     * @param params  The params that matches the parts of the input message of the operation.  If the 
     * BindingOperationInfo supports unwrapping, it assumes the params are in the "unwrapped" form.  If 
     * params are in the wrapped form, use invokeWrapped
     * @return The return values that matche the parts of the output message of the operation
     */
    Object[] invoke(String operationName,
                    Object... params) throws Exception;
    
    /**
     * Invokes an operation synchronously
     * @param operationName The name of the operation to be invoked
     * @param params  The params that matches the parts of the input message of the operation.  If the 
     * BindingOperationInfo supports unwrapping, it assumes the params are in the "unwrapped" form.  If 
     * params are in the wrapped form, use invokeWrapped
     * @return The return values that matche the parts of the output message of the operation
     */
    Object[] invoke(QName operationName,
                    Object... params) throws Exception;


    /**
     * Invokes an operation synchronously
     * @param operationName The name of the operation to be invoked. The service namespace will be used
     * when looking up the BindingOperationInfo.
     * @param params  The params that matches the parts of the input message of the operation
     * @return The return values that matche the parts of the output message of the operation
     */
    Object[] invokeWrapped(String operationName,
                    Object... params) throws Exception;
    
    /**
     * Invokes an operation synchronously
     * @param operationName The name of the operation to be invoked
     * @param params  The params that matches the parts of the input message of the operation
     * @return The return values that matche the parts of the output message of the operation
     */
    Object[] invokeWrapped(QName operationName,
                    Object... params) throws Exception;
    
    /**
     * Invokes an operation synchronously
     * @param oi  The operation to be invoked
     * @param params  The params that matches the parts of the input message of the operation
     * @return The return values that matche the parts of the output message of the operation
     */
    Object[] invoke(BindingOperationInfo oi,
                    Object... params) throws Exception;

    /**
     * Invokes an operation synchronously
     * @param oi  The operation to be invoked
     * @param params  The params that matches the parts of the input message of the operation
     * @param context  Optional (can be null) contextual information for the invocation     
     * @return The return values that matche the parts of the output message of the operation
     */
    Object[] invoke(BindingOperationInfo oi,
                    Object[] params,
                    Map<String, Object> context) throws Exception;
    
    /**
     * Invokes an operation synchronously
     * @param oi  The operation to be invoked
     * @param params  The params that matches the parts of the input message of the operation
     * @param context  Optional (can be null) contextual information for the invocation
     * @param exchange The Exchange to be used for the invocation     
     * @return The return values that matche the parts of the output message of the operation
     */
    Object[] invoke(BindingOperationInfo oi,
            Object[] params, 
            Map<String, Object> context,
            Exchange exchange) throws Exception;
    
    /**
     * Invokes an operation asynchronously
     * @param callback The callback that is called when the response is ready
     * @param operationName The name of the operation to be invoked. The service namespace will be used
     * when looking up the BindingOperationInfo.
     * @param params  The params that matches the parts of the input message of the operation.  If the 
     * BindingOperationInfo supports unwrapping, it assumes the params are in the "unwrapped" form.  If 
     * params are in the wrapped form, use invokeWrapped
     * @return The return values that matche the parts of the output message of the operation
     */
    void invoke(ClientCallback callback,
                    String operationName,
                    Object... params) throws Exception;
    
    /**
     * Invokes an operation asynchronously
     * @param callback The callback that is called when the response is ready
     * @param operationName The name of the operation to be invoked
     * @param params  The params that matches the parts of the input message of the operation.  If the 
     * BindingOperationInfo supports unwrapping, it assumes the params are in the "unwrapped" form.  If 
     * params are in the wrapped form, use invokeWrapped
     * @return The return values that matche the parts of the output message of the operation
     */
    void invoke(ClientCallback callback,
                    QName operationName,
                    Object... params) throws Exception;


    /**
     * Invokes an operation asynchronously
     * @param callback The callback that is called when the response is ready
     * @param operationName The name of the operation to be invoked. The service namespace will be used
     * when looking up the BindingOperationInfo.
     * @param params  The params that matches the parts of the input message of the operation
     * @return The return values that matche the parts of the output message of the operation
     */
    void invokeWrapped(ClientCallback callback,
                           String operationName,
                    Object... params) throws Exception;
    
    /**
     * Invokes an operation asynchronously
     * @param callback The callback that is called when the response is ready
     * @param operationName The name of the operation to be invoked
     * @param params  The params that matches the parts of the input message of the operation
     * @return The return values that matche the parts of the output message of the operation
     */
    void invokeWrapped(ClientCallback callback,
                           QName operationName,
                    Object... params) throws Exception;    
    
    /**
     * Invokes an operation asynchronously
     * @param callback The callback that is called when the response is ready
     * @param oi  The operation to be invoked
     * @param params  The params that matches the parts of the input message of the operation
     * @return The return values that matche the parts of the output message of the operation
     */
    void invoke(ClientCallback callback,
                BindingOperationInfo oi,
                Object... params) throws Exception;    
    
    
    /**
     * Gets the request context used for future invocations
     * @return context The context
     */
    Map<String, Object> getRequestContext();
    /**
     * Gets the response context from the last invocation on this thread
     * @return context The context
     */
    Map<String, Object> getResponseContext();
    
    /**
     * Sets whether the request context is thread local or global to this client.  By 
     * default, the request context is "global" in that any values set in the context
     * are seen by all threads using this client.  If set to true, the context is changed 
     * to be a ThreadLocal and values set are not seen by other threads.
     * @param b
     */
    void setThreadLocalRequestContext(boolean b);
    
    /**
     * Checks if the Request context is thread local or global.
     * @return
     */
    boolean isThreadLocalRequestContext();


    Endpoint getEndpoint();

    /**
     * Get the Conduit that messages for this client will be sent on.
     * @return Conduit
     */
    Conduit getConduit();
    
    /**
     * Get the ConduitSelector responsible for retreiving the Conduit.
     * 
     * @return the current ConduitSelector
     */
    ConduitSelector getConduitSelector();

    /**
     * Set the ConduitSelector responsible for retreiving the Conduit.
     * 
     * @param selector the ConduitSelector to use
     */
    void setConduitSelector(ConduitSelector selector);
    
    /**
     * Indicates that the client is no longer needed and that any resources it holds
     * can now be freed.
     *
     */
    void destroy();
    
    /**
     * Sets the executor which is used to process Asynchronous responses.  The default
     * is to use the threads provided by the transport.  (example: the JMS listener threads) 
     * @param executor
     */
    void setExecutor(Executor executor);
}
