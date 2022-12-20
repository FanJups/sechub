// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.sereco;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercedesbenz.sechub.commons.model.SecHubMessage;
import com.mercedesbenz.sechub.sereco.importer.ProductFailureMetaDataBuilder;
import com.mercedesbenz.sechub.sereco.importer.ProductImportAbility;
import com.mercedesbenz.sechub.sereco.importer.ProductResultImporter;
import com.mercedesbenz.sechub.sereco.importer.ProductSuccessMetaDataBuilder;
import com.mercedesbenz.sechub.sereco.metadata.SerecoAnnotation;
import com.mercedesbenz.sechub.sereco.metadata.SerecoMetaData;
import com.mercedesbenz.sechub.sereco.metadata.SerecoVulnerability;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Workspace {

    private static final Logger LOG = LoggerFactory.getLogger(Workspace.class);

    private SerecoMetaData workspaceMetaData = new SerecoMetaData();

    @Autowired
    private ImporterRegistry registry;

    private String id;

    private ObjectMapper objectMapper;

    public List<SerecoVulnerability> getVulnerabilties() {
        return workspaceMetaData.getVulnerabilities();
    }

    public Workspace(String id) {
        this.id = id;
        this.objectMapper = new ObjectMapper();

        // configure. we do NOT want empty or null values inside our JSON anymore. So
        // easier to read
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.setSerializationInclusion(Include.NON_EMPTY);
    }

    public String getId() {
        return id;
    }

    public void doImport(ImportParameter param) throws IOException {
        if (param == null) {
            throw new IllegalArgumentException("param may not be null!");
        }
        if (param.getImportData() == null) {
            LOG.error("Import data was null for import id:{}, so unable to import.", param.getImportId());
            return;
        }
        if (param.getImportId() == null) {
            LOG.error("Import data was not null, but import id was not set, so unable to import.");
            return;
        }
        boolean atLeastOneImporterWasAbleToImport = false;
        for (ProductResultImporter importer : registry.getImporters()) {
            ProductImportAbility ableToImportForProduct = importer.isAbleToImportForProduct(param);

            if (ProductImportAbility.PRODUCT_FAILED.equals(ableToImportForProduct)) {
                LOG.debug("Importer {} knows product, but recognized as product failure, so no import possible for {}", importer.getName(),
                        param.getImportId());
                /*
                 * means the importer would be able to import, but it is sure that the product
                 * failed, so we add just a critical finding for the product itself
                 */
                ProductFailureMetaDataBuilder builder = new ProductFailureMetaDataBuilder();
                SerecoMetaData failureMetaData = builder.forParam(param).build();
                mergeWithWorkspaceData(failureMetaData);

                mergeWithWorkspaceData(param.getProductMessages());

                atLeastOneImporterWasAbleToImport = true;
                break;
            }
            if (ProductImportAbility.ABLE_TO_IMPORT.equals(ableToImportForProduct)) {
                LOG.debug("Importer {} is able to import {}", importer.getName(), param.getImportId());
                SerecoMetaData importedMetaData = importer.importResult(param.getImportData());
                if (importedMetaData == null) {
                    LOG.error("Meta data was null for product={}, importer={}, importId={}", param.getProductId(), importer.getClass().getSimpleName(),
                            param.getImportId());
                    return;
                }
                mergeWithWorkspaceData(importedMetaData);

                /* add now success meta data */
                ProductSuccessMetaDataBuilder builder = new ProductSuccessMetaDataBuilder();
                SerecoMetaData successMetaData = builder.forParam(param).build();
                mergeWithWorkspaceData(successMetaData);

                mergeWithWorkspaceData(param.getProductMessages());

                atLeastOneImporterWasAbleToImport = true;

            } else {
                LOG.debug("Importer {} is NOT able to import {}", importer.getName(), param.getImportId());
            }
        }
        if (!atLeastOneImporterWasAbleToImport) {
            StringBuilder importerNames = new StringBuilder();
            importerNames.append("[");
            for (ProductResultImporter importer : registry.getImporters()) {
                importerNames.append(importer.getClass().getSimpleName());
                importerNames.append(" ");
            }
            importerNames.append("]");

            LOG.error("For meta data from product={} with importId={} no importers were able to import it! Importers used ={}", param.getProductId(),
                    param.getImportId(), importerNames);
            throw new IOException("Import failed, no importer was able to import product result: " + param.getProductId());
        }

    }

    private void mergeWithWorkspaceData(List<SecHubMessage> productMessages) {
        if (productMessages == null || productMessages.isEmpty()) {
            return;
        }
        Set<SerecoAnnotation> annotations = workspaceMetaData.getAnnotations();

        for (SecHubMessage message : productMessages) {
            SerecoAnnotation annotation = SerecoAnnotation.fromSecHubMessage(message);
            annotations.add(annotation);
        }
    }

    private void mergeWithWorkspaceData(SerecoMetaData metaData) {
        /* currently a very simple approach for vulnerabilities: */
        workspaceMetaData.getVulnerabilities().addAll(metaData.getVulnerabilities());

        workspaceMetaData.getAnnotations().addAll(metaData.getAnnotations());

        workspaceMetaData.getLicenseDocuments().addAll(metaData.getLicenseDocuments());
    }

    public String createReport() {
        try {
            return objectMapper.writeValueAsString(workspaceMetaData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Was not able to write report as json", e);
        }
    }

}
