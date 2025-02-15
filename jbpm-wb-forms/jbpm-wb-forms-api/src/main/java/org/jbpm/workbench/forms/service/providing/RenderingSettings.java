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

package org.jbpm.workbench.forms.service.providing;

import java.io.Serializable;
import java.util.Map;

import org.kie.internal.task.api.ContentMarshallerContext;

/**
 * Defines basic settings to render a form
 */
public interface RenderingSettings extends Serializable {

    /**
     * Retrieves the Servler Template Id for the current settings
     */
    String getServerTemplateId();

    /**
     * Retrieves the actual content of the form to be rendered
     */
    String getFormContent();

    /**
     * Sets the content of the form to be rendered
     */
    void setFormContent(String formContent);

    /**
     * Retrieves the ContentMarshallerContext
     */
    ContentMarshallerContext getMarshallerContext();

    /**
     * Sets the ContentMarshallerContext
     */
    void setMarshallerContext(ContentMarshallerContext marshallerContext);
    
    Map getProcessInstanceVariables();
    
    Map getInputData();
    
    Map getRenderingMetaData();
}
