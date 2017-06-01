/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.admin.common.services;

import static org.codice.ddf.admin.common.report.message.DefaultMessages.failedDeleteError;
import static org.codice.ddf.admin.common.report.message.DefaultMessages.failedPersistError;
import static org.codice.ddf.admin.common.report.message.DefaultMessages.failedUpdateError;
import static org.codice.ddf.admin.common.report.message.DefaultMessages.noExistingConfigError;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.text.StrSubstitutor;
import org.codice.ddf.admin.api.Field;
import org.codice.ddf.admin.common.fields.common.PidField;
import org.codice.ddf.admin.common.report.ReportImpl;
import org.codice.ddf.admin.configurator.Configurator;
import org.codice.ddf.admin.configurator.ConfiguratorFactory;
import org.codice.ddf.admin.configurator.OperationReport;

public class ServiceCommons {

    public static final String SERVICE_PID_KEY = "service.pid";

    public static final String FACTORY_PID_KEY = "service.factoryPid";

    // A flag to indicate if a service being updated has a password of "password". If so, the
    // password will not be updated.
    public static final String FLAG_PASSWORD = "password";

    public String resolveProperty(String str) {
        return StrSubstitutor.replaceSystemProperties(str);
    }

    public List<String> resolveProperties(String... list) {
        return Arrays.stream(list)
                .map(this::resolveProperty)
                .collect(Collectors.toList());
    }

    public static ReportImpl createManagedService(Map<String, Object> serviceProps, String factoryPid,
            ConfiguratorFactory configuratorFactory) {
        ReportImpl report = new ReportImpl();
        Configurator configurator = configuratorFactory.getConfigurator();

        configurator.createManagedService(factoryPid, serviceProps);
        if(configurator.commit("Service saved with details [{}]", serviceProps.toString())
                .containsFailedResults()) {
            report.addResultMessage(failedPersistError());
        }
        return report;
    }

    public static ReportImpl updateService(PidField servicePid, Map<String, Object> newConfig,
            ConfiguratorFactory configuratorFactory) {
        ReportImpl report = new ReportImpl();
        report.addMessages(serviceConfigurationExists(servicePid, configuratorFactory));
        if (report.containsErrorMsgs()) {
            return report;
        }

        String pid = servicePid.getValue();
        Configurator configurator = configuratorFactory.getConfigurator();
        configurator.updateConfigFile(pid, newConfig, true);
        OperationReport operationReport = configurator.commit(
                "Updated config with pid [{}] and new service properties [{}]",
                pid,
                newConfig.toString());
        if (operationReport.containsFailedResults()) {
            return report.addResultMessage(failedUpdateError());
        }

        return report;
    }

    public static ReportImpl deleteService(PidField servicePid, ConfiguratorFactory configuratorFactory) {
        ReportImpl report = new ReportImpl();
        Configurator configurator = configuratorFactory.getConfigurator();
        configurator.deleteManagedService(servicePid.getValue());
        if(configurator.commit("Deleted source with pid [{}].", servicePid.getValue())
                .containsFailedResults()) {
            report.addResultMessage(failedDeleteError());
        }
        return report;
    }

    public static ReportImpl serviceConfigurationExists(PidField servicePid, ConfiguratorFactory configuratorFactory) {
        ReportImpl report = new ReportImpl();
        if(configuratorFactory.getConfigReader()
                .getConfig(servicePid.getValue())
                .isEmpty()) {
            report.addResultMessage(noExistingConfigError());
        }
        return report;
    }

    public static boolean serviceConfigurationExists(String servicePid, ConfiguratorFactory configuratorFactory) {
        return !configuratorFactory.getConfigReader()
                .getConfig(servicePid)
                .isEmpty();
    }

    public static <T> T mapValue(Map<String, Object> props, String key) {
        return props.get(key) == null ? null : (T) props.get(key);
    }

    public static class ServicePropertyBuilder {

        private Map<String, Object> serviceProperties;

        public ServicePropertyBuilder() {
            serviceProperties = new HashMap<>();
        }

        public ServicePropertyBuilder put(String key, Object object) {
            serviceProperties.put(key, object);
            return this;
        }

        public ServicePropertyBuilder putPropertyIfNotNull(String key, Field field) {
            if(field.getValue() != null) {
                put(key, field.getValue());
            }
            return this;
        }

        public Map<String, Object> build() {
            return serviceProperties;
        }
    }
}