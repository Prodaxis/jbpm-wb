/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.workbench.forms.client.i18n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

/**
 * This uses GWT to provide client side compile time resolving of locales. See:
 * http://code.google.com/docreader/#p=google-web-toolkit-doc-1-5&s=google-web- toolkit-doc-1-5&t=DevGuideInternationalization
 * (for more information).
 * <p/>
 * Each method name matches up with a key in Constants.properties (the properties file can still be used on the server). To use
 * this, use <code>GWT.create(Constants.class)</code>.
 */
public interface Constants extends Messages {

    Constants INSTANCE = GWT.create(Constants.class);

    String Actions();

    String Refresh();

    String Work();

    String Details();

    String Comments();

    String Assignments();

    String Claim();

    String Save();

    String Release();

    String Complete();

    String Form();

    String Start();

    String Submit();

    String Correlation_Key();

    String UnexpectedError(String errorMessage);
    
    String ActionCheckValueExist();

    String TaskCompleted(Long id);

    String TaskStarted(Long id);

    String TaskClaimed(Long id);

    String TaskSaved(Long id);

    String TaskReleased(Long id);

    String UnableToFindFormForTask(Long id);

    String Start_process_instance();

    String Select_Process();

    String Process_DeploymentId();

    String Process_Definition();

    String ProcessStarted(Long processInstanceId);
    
    String CaseStarted(String caseId);

    String UnableToFindFormForProcess(String processName);

    String TaskFormErrorHeader();

    String PermissionDenied();
    
    String ValueNotExistInTable(String value, String table);
    
    String FieldKeyMappingIsRequiredToCheckExist();
    
    String FieldKeyValueMappingFormatErorr();

    String Exception(String message);
}