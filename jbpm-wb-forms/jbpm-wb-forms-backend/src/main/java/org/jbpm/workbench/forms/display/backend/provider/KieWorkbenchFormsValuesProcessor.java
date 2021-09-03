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

package org.jbpm.workbench.forms.display.backend.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jbpm.workbench.forms.display.api.KieWorkbenchFormRenderingSettings;
import org.jbpm.workbench.forms.service.providing.RenderingSettings;
import org.kie.workbench.common.forms.dynamic.service.context.generation.dynamic.BackendFormRenderingContext;
import org.kie.workbench.common.forms.dynamic.service.context.generation.dynamic.BackendFormRenderingContextManager;
import org.kie.workbench.common.forms.dynamic.service.shared.impl.MapModelRenderingContext;
import org.kie.workbench.common.forms.fields.shared.fieldTypes.basic.lists.selector.AbstractMultipleSelectorFieldDefinition;
import org.kie.workbench.common.forms.fields.shared.fieldTypes.basic.lists.selector.MultipleSelectorFieldType;
import org.kie.workbench.common.forms.fields.shared.fieldTypes.basic.selectors.DefaultSelectorOption;
import org.kie.workbench.common.forms.fields.shared.fieldTypes.basic.selectors.SelectorFieldBaseDefinition;
import org.kie.workbench.common.forms.fields.shared.fieldTypes.basic.selectors.listBox.type.ListBoxFieldType;
import org.kie.workbench.common.forms.fields.shared.fieldTypes.basic.selectors.radioGroup.type.RadioGroupFieldType;
import org.kie.workbench.common.forms.jbpm.service.bpmn.DynamicBPMNFormGenerator;
import org.kie.workbench.common.forms.jbpm.service.bpmn.util.BPMNVariableUtils;
import org.kie.workbench.common.forms.model.FieldDefinition;
import org.kie.workbench.common.forms.model.FieldType;
import org.kie.workbench.common.forms.model.FormDefinition;
import org.kie.workbench.common.forms.services.backend.serialization.FormDefinitionSerializer;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.prodaxis.solar.udm.ModelFactory;
import com.prodaxis.solar.util.BusinessComponentScriptHelper;
import com.prodaxis.solar.util.IProcessInstanceData;
import com.prodaxis.solar.xtc.model.IObjectModel;

public abstract class KieWorkbenchFormsValuesProcessor<T extends RenderingSettings> implements IProcessInstanceData {

    public static final String SERVER_TEMPLATE_ID = "serverTemplateId";

    public static final String SETTINGS_ATRA_NAME = "_rendering_settings";

    protected FormDefinitionSerializer formSerializer;

    protected BackendFormRenderingContextManager contextManager;

    protected DynamicBPMNFormGenerator dynamicBPMNFormGenerator;
    
    Map processInstanceVariables;

    public KieWorkbenchFormsValuesProcessor(FormDefinitionSerializer formSerializer,
                                            BackendFormRenderingContextManager contextManager,
                                            DynamicBPMNFormGenerator dynamicBPMNFormGenerator) {
        this.formSerializer = formSerializer;
        this.contextManager = contextManager;
        this.dynamicBPMNFormGenerator = dynamicBPMNFormGenerator;
    }

    public KieWorkbenchFormRenderingSettings generateRenderingContext(T settings) {
        return generateRenderingContext(settings,false);
    }

    public KieWorkbenchFormRenderingSettings generateRenderingContext(T settings, boolean generateDefaultForms) {
        if (generateDefaultForms || !StringUtils.isEmpty(settings.getFormContent())) {

            try {

                ContextForms forms = generateDefaultForms ? generateDefaultForms(settings) : parseForms(settings);

                if (forms.getRootForm() == null || !isValid(forms.getRootForm())) {
                    return null;
                }

                Map<String, Object> rawData = generateRawFormData(settings, forms.getRootForm());
                
                Map<String, String> params = new HashMap<>();
                params.put(SERVER_TEMPLATE_ID,
                           settings.getServerTemplateId());

                BackendFormRenderingContext context = contextManager.registerContext(forms.getRootForm(),
                                                                                     rawData,
                                                                                     settings.getProcessInstanceVariables(),
                                                                                     settings.getMarshallerContext().getClassloader(),
                                                                                     params,
                                                                                     forms.getNestedForms().toArray(new FormDefinition[forms.getNestedForms().size()]));

                prepareContext(settings,
                               context);

                context.getAttributes().put(SETTINGS_ATRA_NAME,
                                            settings);
                
                MapModelRenderingContext mapModelRenderingContext = context.getRenderingContext();
                mapModelRenderingContext.getNodeRenderingSettingsMetaData().putAll(settings.getRenderingMetaData());

                return new KieWorkbenchFormRenderingSettings(context.getTimestamp(), mapModelRenderingContext, generateDefaultForms);
            } catch (Exception ex) {
                getLogger().debug("Unable to generate render form: ",
                                  ex);
            }
        }

        return null;
    }

    public Map<String, Object> generateRuntimeValuesMap(long timestamp,
                                                        Map<String, Object> formValues) {

        BackendFormRenderingContext context = contextManager.getContext(timestamp);

        if (context != null) {
            FormDefinition form = context.getRenderingContext().getRootForm();

            if (isValid(form)) {
                Map<String, Object> formData = contextManager.updateContextData(timestamp,
                                                                                formValues).getFormData();
                return getOutputValues(formData, form, (T) context.getAttributes().get(SETTINGS_ATRA_NAME));
            }
        }
        return Collections.emptyMap();
    }

    protected ContextForms parseForms(T settings) {
        ContextForms result = new ContextForms();
        processInstanceVariables = settings.getProcessInstanceVariables();
        JsonParser parser = new JsonParser();
        Gson gson = new Gson();
        JsonElement element = parser.parse(settings.getFormContent());

        JsonArray forms = element.getAsJsonArray();
		forms.forEach(jsonForm -> {
			String content = gson.toJson(jsonForm);
			if (!StringUtils.isEmpty(content)) {
				FormDefinition formDefinition = formSerializer.deserialize(content);
				if (formDefinition != null) {
					// <Prodaxis> - Load dynamic data initial
					List<FieldDefinition> fields = formDefinition.getFields();
					if (null != fields) {
						for (FieldDefinition field : fields) {
							try {
								field.setDataInitialLoaded(null);
								String fieldBinding = field.getBinding();
								String methodClassMapping = field.getMethodClassMappingParteor();
								if (null != methodClassMapping && methodClassMapping.contains("#")) {
									String keyMapping = field.getKeyMappingParteor();
									String valueMapping = field.getValueMappingParteor();
									Object resultReturn = BusinessComponentScriptHelper.getInstance().executionBusinessComponentScript(this, methodClassMapping, null);
									if (field.isDoLoadInitialData() && null != resultReturn) {
										if(null != keyMapping && keyMapping.contains("#")){ // return is collection or object
											String[] componentClassKey = keyMapping.split("#");
											String keyAttribut = componentClassKey[1];
											FieldType fieldType = field.getFieldType();
											if (null != resultReturn) {
												if (resultReturn instanceof Collection) {
													Collection results = (Collection) resultReturn;
													Iterator it = results.iterator();
													if (MultipleSelectorFieldType.NAME.equals(fieldType.getTypeName())) {
														AbstractMultipleSelectorFieldDefinition multipleSelectorField = (AbstractMultipleSelectorFieldDefinition) field;
														while (it.hasNext()) {
															IObjectModel resultModel = ModelFactory.getInstance().createObjectModel("TmpModel", it.next());
															multipleSelectorField.getListOfValues().add(resultModel.getAttribute(keyAttribut).get());
														}
													} else if (ListBoxFieldType.NAME.equals(fieldType.getTypeName()) || RadioGroupFieldType.NAME.equals(fieldType.getTypeName()) || "ComboBox".equals(fieldType.getTypeName())) {
														SelectorFieldBaseDefinition selectorFieldBaseDefinition = (SelectorFieldBaseDefinition) field;
														while (it.hasNext()) {
															IObjectModel resultModel = ModelFactory.getInstance().createObjectModel("TmpModel", it.next());
															Object keyValue = resultModel.getAttribute(keyAttribut).get();
															String text = keyValue + "";
															if (null != valueMapping && valueMapping.contains("#")) {
																String[] valueMappings = valueMapping.split("#");
																String valueAttribut = valueMappings[1];
																if (!keyAttribut.equals(valueAttribut)) {
																	Object value = resultModel.getAttribute(valueAttribut).get();
																	text += (null == value) ? "" : (" : " + value.toString());
																}
															}
															DefaultSelectorOption option = new DefaultSelectorOption(keyValue, text);
															selectorFieldBaseDefinition.getOptions().add(option);
														}
													}
												} else if (!resultReturn.getClass().getName().startsWith("java.lang")) { // return object
													IObjectModel resultModel = ModelFactory.getInstance().createObjectModel("TmpModel", resultReturn);
													Object keyValue = resultModel.getAttribute(keyAttribut).get();
													field.setDataInitialLoaded(keyValue + "");
												}
											}
										}else{
											field.setDataInitialLoaded(resultReturn + "");
										}
									}
									if(resultReturn!= null && resultReturn.toString().startsWith("ERROR")){
										field.setAsyncErrorKey(resultReturn.toString());
										field.setDataInitialLoaded(resultReturn.toString());
									}
								}
							} catch (Exception e) {
								getLogger().error("Error load dynamic data initial : " + e.getMessage());
							}
						}
					}
					// </Prodaxis>
					if (formDefinition.getName().startsWith(getFormName(settings) + BPMNVariableUtils.TASK_FORM_SUFFIX)) {
						result.setRootForm(formDefinition);
					} else {
						result.getNestedForms().add(formDefinition);
					}
				}
			}
		});
        return result;
    }

    protected ContextForms generateDefaultForms(T settings) {
        ContextForms result = new ContextForms();

        Collection<FormDefinition> contextForms = generateDefaultFormsForContext(settings);

        if (contextForms == null) {
            throw new IllegalArgumentException("Unable to create forms for context");
        }

        contextForms.forEach(form -> {
            if (form.getName().equals(getFormName(settings) + BPMNVariableUtils.TASK_FORM_SUFFIX)) {
                result.setRootForm(form);
            } else {
                result.getNestedForms().add(form);
            }
        });

        return result;
    }

    protected abstract Collection<FormDefinition> generateDefaultFormsForContext(T settings);

    protected abstract Map<String, Object> getOutputValues(Map<String, Object> values,
                                                           FormDefinition form,
                                                           T settings);

    protected abstract boolean isValid(FormDefinition rootForm);

    protected abstract String getFormName(T settings);

    protected abstract void prepareContext(T settings,
                                           BackendFormRenderingContext context);

    protected Map<String, Object> generateRawFormData(T settings,
                                                      FormDefinition form) {
        return new HashMap<>();
    }

    protected abstract Logger getLogger();

    protected class ContextForms {

        private FormDefinition rootForm;
        private List<FormDefinition> nestedForms = new ArrayList<>();

        public FormDefinition getRootForm() {
            return rootForm;
        }

        public void setRootForm(FormDefinition rootForm) {
            this.rootForm = rootForm;
        }

        public List<FormDefinition> getNestedForms() {
            return nestedForms;
        }
    }
    
    @Override
	public Object getProcessVariable(String name) {
    	if(null != processInstanceVariables)
    		return processInstanceVariables.get(name);
    	return null;
	}

	@Override
	public void updateProcessVariable(String name, Object value) {
		if(null != processInstanceVariables)
			processInstanceVariables.put(name, value);
	}

	@Override
	public Object convertBPMNObject(String variableName, Object objectValue) {
		return null;
	}
}
