/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.query.discover;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.karaf.bundle.core.BundleService;
import org.codice.ddf.admin.api.Field;
import org.codice.ddf.admin.api.fields.FunctionField;
import org.codice.ddf.admin.common.fields.base.BaseFunctionField;
import org.codice.ddf.admin.common.fields.base.scalar.IntegerField;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.codice.ddf.admin.query.dependency.Node;
import org.codice.ddf.admin.query.dependency.TreeShow;
import org.codice.ddf.admin.query.fields.BundleField;
import org.codice.ddf.admin.query.fields.PackageField;
import org.codice.ddf.admin.query.fields.ReferenceListField;
import org.codice.ddf.admin.query.fields.ServiceField;
import org.codice.ddf.admin.query.fields.ServiceReferenceField;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;

import com.google.common.collect.ImmutableList;

public class GetBundles extends BaseFunctionField<BundleField.ListImpl> {

    public static final String FIELD_NAME = "bundles";

    public static final String DESCRIPTION = "Retrieves all bundles in the karaf instance";

    public static final String BUNDLE_ID = "bundleId";

    private IntegerField bundleIdArg;

    private BundleField.ListImpl returnType;

    private ConfigurationAdmin configAdmin;

    public GetBundles(ConfigurationAdmin configAdmin) {
        super(FIELD_NAME, DESCRIPTION);
        bundleIdArg = new IntegerField(BUNDLE_ID);
        returnType = new BundleField.ListImpl();
        updateArgumentPaths();

        this.configAdmin = configAdmin;
    }

    @Override
    public BundleField.ListImpl performFunction() {
        List<BundleField> bundlesFields = new ArrayList<>();
        List<Bundle> bundles;

        if (bundleIdArg.getValue() != null) {
            bundles = Arrays.asList(FrameworkUtil.getBundle(GetBundles.class)
                    .getBundleContext()
                    .getBundle(Math.toIntExact(bundleIdArg.getValue())));
        } else {
            bundles = Arrays.asList(FrameworkUtil.getBundle(GetBundles.class)
                    .getBundleContext()
                    .getBundles());
        }

        try {
            for (Bundle bundle : bundles) {
                BundleField newBundleField = new BundleField().name(bundle.getSymbolicName())
                        .id(Math.toIntExact(bundle.getBundleId()))
                        .location(bundle.getLocation());

                //imported pkgs
                for (Node<Bundle> pkgDep : new TreeShow().doExecute(bundle).getChildren()) {
                    newBundleField.addImportedPackage(new PackageField().name(pkgDep.getValue()
                            .getSymbolicName())
                            .bundleId(Math.toIntExact(pkgDep.getValue()
                                    .getBundleId())));
                }

                //exported pkgs
                for(Clause clause : Parser.parseHeader(bundle.getHeaders().get("Export-Package"))) {
                    newBundleField.addExportedPackage(new PackageField().name(clause.getName()).bundleId(Math.toIntExact(bundle.getBundleId())));
                }

                //services
                populateServices(bundle, newBundleField);
                bundlesFields.add(newBundleField);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new BundleField.ListImpl().addAll(bundlesFields);
    }

    public void populateServices(Bundle bundle, BundleField toPopulate)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            NoSuchFieldException, InvalidSyntaxException {
        if(bundle.getRegisteredServices() == null || bundle.getRegisteredServices().length == 0) {
            return;
        }

        Optional<ServiceReference<?>> blueprintRef = Arrays.stream(bundle.getRegisteredServices())
                .filter(ref -> ref.toString()
                        .contains("org.osgi.service.blueprint.container.BlueprintContainer"))
                .findFirst();

        if(!blueprintRef.isPresent()) {
            return;
        }

        BlueprintContainer blueprintContainer  = (BlueprintContainer) bundle.getBundleContext().getService(blueprintRef.get());
        for(String id : blueprintContainer.getComponentIds()) {
            ComponentMetadata cmpMetadata = blueprintContainer.getComponentMetadata(id);

            if(cmpMetadata instanceof ReferenceListMetadata) {
                ReferenceListMetadata refListMeta = (ReferenceListMetadata) cmpMetadata;
                ReferenceListField refListF = new ReferenceListField();
                String searchFilter = getLdapFilter(refListMeta.getFilter());

                refListF.filter(searchFilter)
                        .interfacee(refListMeta.getInterface())
                        .resolution(refListMeta.getAvailability());

                ServiceReference[] refs = bundle.getBundleContext().getServiceReferences(refListMeta.getInterface(), searchFilter);

                if(refs == null) {
                    continue;
                }

                for(ServiceReference ref : refs) {
                    refListF.addService(new ServiceField(ref));
                }
                toPopulate.addServiceRefList(refListF);
            } else if(cmpMetadata instanceof ReferenceMetadata) {
                ReferenceMetadata refMeta = (ReferenceMetadata) cmpMetadata;
                String searchFilter = getLdapFilter(refMeta.getFilter());

                ServiceReference[] refs = bundle.getBundleContext().getServiceReferences(refMeta.getInterface(), searchFilter);

                if(refs == null) {
                    continue;
                }

                for(ServiceReference ref : refs) {
                    toPopulate.addServiceRef(new ServiceReferenceField()
                            .interfacee(refMeta.getInterface())
                            .filter(searchFilter)
                            .resolution(refMeta.getAvailability())
                            .service(new ServiceField(ref)));
                }

            } else if(cmpMetadata instanceof ServiceMetadata) {
                ServiceReference[] refs = bundle.getRegisteredServices();

                if(refs == null) {
                    continue;
                }

                for(ServiceReference ref : refs) {
                    toPopulate.addService(new ServiceField(ref));
                }
            }
        }
    }

    public String getLdapFilter(String filter) {
        if(filter == null || filter.startsWith("(")) {
            return filter;
        } else {
            return "(" + filter + ")";
        }
    }
    @Override
    public FunctionField<BundleField.ListImpl> newInstance() {
        return new GetBundles(configAdmin);
    }

    @Override
    public Set<String> getFunctionErrorCodes() {
        return new HashSet<>();
    }

    @Override
    public List<Field> getArguments() {
        return ImmutableList.of(bundleIdArg);
    }

    @Override
    public BundleField.ListImpl getReturnType() {
        return returnType;
    }
}
