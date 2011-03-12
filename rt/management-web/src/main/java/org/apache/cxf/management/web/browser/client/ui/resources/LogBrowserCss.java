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

import com.google.gwt.resources.client.CssResource;

public interface LogBrowserCss extends CssResource {

    String navigationSidebarSlot();
    String viewerSlot();

    /* Error Dialog styles */
    String errorDialog();
    String errorDialogGlass();
    String errorDialogTitle();
    String errorDialogButtons();
    String errorDialogErrorType();

    /* Browser tab styles*/
    String browserTabLoadingMessage();
    String browserTabNoEntriesMessage();
    String browserTabSelectedRow();
    String browserTabManageSubscriptionsButton();
    String browserTabToolBar();
    String browserTabEntryTableHeaders();
    String browserTabEntrySelectableTable();
    String browserTabSubscriptionsSideBar();
    String browserTabSubscriptionsHeader();
    String browserTabEntryDetailsSection();
    String browserTabEntryDetailsContent();
    String browserTabNavigationLink();

    String sidebarItem();
    String sidebarHeader();

    String topbarLink();

    String selectableTableRow();

    /* Settings tab styles */
    String settingsTabHeader();
    String settingsTabBackButton();
    String settingsTabTitle();
    String settingsTabToolBar();
    String settingsTabContent();
    String settingsTabFeedList();

    /*  Feed's entry (in settings tab) styles */
    String feedEntry();
    String feedEntryNameLabel();
    String feedEntryUrlLabel();
    String feedEntryButtons();
    String feedEntryRemoveButton();
    String feedEntryEditButton();

    /* Edit feed dialog (in settings tab) styles */
    String editFeedDialogErrorMessage();
    String editFeedDialogButtons();
    String editFeedDialogAddButton();
}
