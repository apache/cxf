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

package org.apache.cxf.management.web.browser.client.ui.resources;

import com.google.gwt.i18n.client.Constants;

//TODO divide items into groups (like: browseTab, settingsTab etc.)
public interface LogBrowserConstans extends Constants {

    @DefaultStringValue("Application Error")
    String errorDialogTitle();

    @DefaultStringValue("Continue")
    String errorDialogContineButton();

    @DefaultStringValue("No entries")
    String browserTabNoEntries();

    @DefaultStringValue("160px")
    String browseTabDatatimeColumnWidth();

    @DefaultStringValue("128px")
    String browseTabLevelColumnWidth();

    @DefaultStringValue("350px")
    String browseTabNavigationLinksColumnWidth();

    @DefaultStringValue("Loading")
    String browserTabLoading();

    @DefaultStringValue("Edit criteria")
    String editCriteriaDialogTitle();

    @DefaultStringValue("Add endpoint")
    String settingsTabAddSubscriptionDialogTitle();

    @DefaultStringValue("Edit endpoint")
    String settingsTabEditSubscriptionDialogTitle();

    @DefaultStringValue("Name can't be empty")
    String settingsTabSubscriptionDialogEmptyName();

    @DefaultStringValue("URL can't be empty")
    String settingsTabSubscriptionDialogEmptyUrl();
}
