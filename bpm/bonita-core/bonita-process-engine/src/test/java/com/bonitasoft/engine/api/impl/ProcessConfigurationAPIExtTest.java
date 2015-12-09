/*
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 */
package com.bonitasoft.engine.api.impl;

import static org.mockito.Mockito.*;

import org.bonitasoft.engine.api.impl.resolver.BusinessArchiveArtifactsManager;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.commons.exceptions.SObjectModificationException;
import org.bonitasoft.engine.commons.exceptions.SObjectNotFoundException;
import org.bonitasoft.engine.core.form.FormMappingService;
import org.bonitasoft.engine.core.form.impl.SFormMappingImpl;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDefinitionNotFoundException;
import org.bonitasoft.engine.exception.FormMappingNotFoundException;
import org.bonitasoft.engine.exception.RetrieveException;
import org.bonitasoft.engine.exception.UpdateException;
import org.bonitasoft.engine.form.FormMappingType;
import org.bonitasoft.engine.page.impl.SPageMappingImpl;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.bonitasoft.engine.service.TenantServiceAccessor;
import com.bonitasoft.engine.service.impl.LicenseChecker;
import com.bonitasoft.manager.Features;

/**
 * author Emmanuel Duchastenier
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessConfigurationAPIExtTest {

    private static final long PROCESS_DEF_ID = 158l;
    private static final long FORM_MAPPING_ID = 458l;

    @Mock
    private LicenseChecker licenceChecker;
    @Mock
    private BusinessArchiveArtifactsManager businessArchiveArtifactsManager;
    @Mock
    private FormMappingService formMappingService;
    @Mock
    private ProcessDefinitionService processDefinitionService;
    @Mock
    private TenantServiceAccessor tenantServiceAccessor;
    @Spy
    private ProcessConfigurationAPIExt processConfigurationAPI;

    @Before
    public void setUp() throws Exception {
        doReturn(licenceChecker).when(processConfigurationAPI).getLicenseChecker();
        doReturn(tenantServiceAccessor).when(processConfigurationAPI).getTenantAccessor();
        doReturn(formMappingService).when(tenantServiceAccessor).getFormMappingService();
        doReturn(processDefinitionService).when(tenantServiceAccessor).getProcessDefinitionService();
        doReturn(businessArchiveArtifactsManager).when(tenantServiceAccessor).getBusinessArchiveArtifactsManager();
    }

    SFormMappingImpl createSFormMapping(FormMappingType type, long formMappingId, String url, Long pageId, String urlAdapter, long processDefinitionId) {
        SFormMappingImpl sFormMapping = new SFormMappingImpl();
        sFormMapping.setId(formMappingId);
        sFormMapping.setProcessDefinitionId(processDefinitionId);
        sFormMapping.setType(type.getId());
        SPageMappingImpl pageMapping = new SPageMappingImpl();
        sFormMapping.setPageMapping(pageMapping);
        pageMapping.setUrlAdapter(urlAdapter);
        pageMapping.setPageId(pageId);
        pageMapping.setUrl(url);
        return sFormMapping;
    }

    @Test(expected = FormMappingNotFoundException.class)
    public void updateFormMappingShouldThrowFormMappingNotFound() throws Exception {
        doThrow(SObjectNotFoundException.class).when(formMappingService).get(FORM_MAPPING_ID);
        processConfigurationAPI.updateFormMapping(FORM_MAPPING_ID, "other", null);
    }

    @Test(expected = RetrieveException.class)
    public void updateFormMappingShouldThrowRetrieveException() throws Exception {
        doThrow(SBonitaReadException.class).when(formMappingService).get(FORM_MAPPING_ID);
        processConfigurationAPI.updateFormMapping(FORM_MAPPING_ID, "some", null);
    }

    @Test(expected = UpdateException.class)
    public void updateFormMappingShouldThrowUpdateException() throws Exception {
        doThrow(SObjectModificationException.class).when(formMappingService).get(FORM_MAPPING_ID);
        processConfigurationAPI.updateFormMapping(FORM_MAPPING_ID, "someForm", null);
    }

    @Test
    public void updateFormMappingShouldCallUpdateOnService() throws Exception {
        //given
        SFormMappingImpl sFormMapping = createSFormMapping(FormMappingType.PROCESS_START, FORM_MAPPING_ID, "theUrl", null, null, PROCESS_DEF_ID);
        doReturn(sFormMapping).when(formMappingService).get(FORM_MAPPING_ID);
        //when
        processConfigurationAPI.updateFormMapping(FORM_MAPPING_ID, "theNewForm", null);
        //then
        verify(formMappingService, times(1)).update(sFormMapping, "theNewForm", null);
    }

    @Test
    public void updateFormMapping_call_resolution_of_process() throws Exception {
        //given
        doReturn(createSFormMapping(FormMappingType.PROCESS_START, FORM_MAPPING_ID, "theUrl", null, null, PROCESS_DEF_ID)).when(formMappingService).get(FORM_MAPPING_ID);
        //when
        processConfigurationAPI.updateFormMapping(FORM_MAPPING_ID, "theNewForm", null);
        //then
        verify(businessArchiveArtifactsManager).resolveDependencies(PROCESS_DEF_ID, tenantServiceAccessor);
    }

    @Test
    public void updateExpressionShouldCheckLicenseKey() throws Exception {
        processConfigurationAPI.updateExpressionContent(124L, 87L, "someGroovyScript");

        verify(licenceChecker).checkLicenseAndFeature(Features.LIVE_UPDATE_SCRIPT);
    }

    @Test
    public void updateExpressionShouldCallProcessDefinitionService_update() throws Exception {
        processConfigurationAPI.updateExpressionContent(124L, 87L, "someGroovyScript");

        verify(processDefinitionService).updateExpressionContent(124L, 87L, "someGroovyScript");
    }

    @Test(expected = UpdateException.class)
    public void updateExressionShouldThrowUpdateExceptionWhenAppropriate() throws Exception {
        doThrow(SObjectModificationException.class).when(processDefinitionService).updateExpressionContent(anyLong(), anyLong(), anyString());
        processConfigurationAPI.updateExpressionContent(124L, 87L, "someGroovyScript");
    }

    @Test(expected = ProcessDefinitionNotFoundException.class)
    public void updateExressionShouldThrowProcessDefinitionNotFoundExceptionWhenAppropriate() throws Exception {
        doThrow(SProcessDefinitionNotFoundException.class).when(processDefinitionService).updateExpressionContent(anyLong(), anyLong(), anyString());
        processConfigurationAPI.updateExpressionContent(124L, 87L, "anyString");
    }
}
