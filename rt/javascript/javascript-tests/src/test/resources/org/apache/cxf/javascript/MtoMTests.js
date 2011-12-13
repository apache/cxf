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

function assertionFailed(explanation)
{
 	var assert = new Assert(explanation); // this will throw out in Java.
}

var globalNotifier = null;
var globalErrorStatus = null;
var globalErrorStatusText = null;
var globalResponseObject = null;

function resetGlobals() {
	globalNotifier = null;
	globalErrorStatus = null;
	globalErrorStatusText = null;
	globalResponseObject = null;
}

function errorCallback(httpStatus, httpStatusText) 
{
	globalErrorStatus = httpStatus;
	globalStatusText = httpStatusText;
	globalNotifier.notify();
}

function successCallback(responseObject) 
{
	globalResponseObject = responseObject;
	globalNotifier.notify();
}

function testMtoMString(url) {
	globalNotifier = new org_apache_cxf_notifier();

	var service = new org_apache_cxf_javascript_fortest_MtoM();
	service.url = url;
	var param = new org_apache_cxf_javascript_testns_mtoMParameterBeanWithDataHandler();
	param.setOrdinary("disorganized<organized");
	param.setNotXml10("<html>\u0027</html>");
	// 'DH' means that the client side will use a DataHandler
	service.receiveNonXmlDH(successCallback, errorCallback, param);
	return globalNotifier;
}

function testMtoMReply(url) {
	globalNotifier = new org_apache_cxf_notifier();

	var service = new org_apache_cxf_javascript_fortest_MtoM();
	service.url = url;
	service.sendNonXmlDH(successCallback, errorCallback);
	return globalNotifier;
}
