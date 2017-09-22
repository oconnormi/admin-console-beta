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
package org.codice.ddf.admin.query.fields;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.codice.ddf.admin.api.Field;
import org.codice.ddf.admin.common.fields.base.BaseListField;
import org.codice.ddf.admin.common.fields.base.BaseObjectField;
import org.codice.ddf.admin.common.fields.base.scalar.IntegerField;
import org.codice.ddf.admin.common.fields.base.scalar.StringField;

import com.google.common.collect.ImmutableList;

public class BundleField extends BaseObjectField {

    public static final String DEFAULT_FIELD_NAME = "bundle";

    public static final String FIELD_TYPE_NAME = "Bundle";

    public static final String DESCRIPTION = "An OSGI bundle";

    public static final String ID = "id";

    public static final String NAME = "name";

    public static final String LOCATION = "location";

    public static final String STATE = "state";

    public static final String EXPORTED_PKGS = "exportedPkgs";

    public static final String IMPORTED_PKGS = "importedPkgs";

    private IntegerField id;

    private StringField name;

    private StringField location;

    private StringField state;

    private PackageField.ListImpl exportedPkgs;

    private PackageField.ListImpl importedPkgs;

    private ServiceField.ListImpl services;

    private ServiceReferenceField.ListImpl refs;

    private ReferenceListField.ListImpl refLists;
    //    private StateField state;

    public BundleField() {
        super(DEFAULT_FIELD_NAME, FIELD_TYPE_NAME, DESCRIPTION);
        id = new IntegerField(ID);
        name = new StringField(NAME);
        location = new StringField(LOCATION);
        state = new StringField(STATE);
        services = new ServiceField.ListImpl();
        exportedPkgs = new PackageField.ListImpl(EXPORTED_PKGS);
        importedPkgs = new PackageField.ListImpl(IMPORTED_PKGS);
        refs = new ServiceReferenceField.ListImpl();
        refLists = new ReferenceListField.ListImpl();
        updateInnerFieldPaths();
    }

    public BundleField id(int id) {
        this.id.setValue(id);
        return  this;
    }

    public BundleField name(String name) {
        this.name.setValue(name);
        return  this;
    }

    public BundleField location(String location) {
        this.location.setValue(location);
        return  this;
    }

    public BundleField addService(ServiceField ref) {
        services.add(ref);
        return this;
    }

    public BundleField addServiceRef(ServiceReferenceField ref) {
        refs.add(ref);
        return this;
    }

    public BundleField addServiceRefList(ReferenceListField refList) {
        refLists.add(refList);
        return this;
    }

    public BundleField addExportedPackage(PackageField importedPkg) {
        exportedPkgs.add(importedPkg);
        return this;
    }

    public BundleField addImportedPackage(PackageField importedPkg) {
        importedPkgs.add(importedPkg);
        return this;
    }

    public ReferenceListField.ListImpl refLists() {
        return refLists;
    }

    @Override
    public List<Field> getFields() {
        return ImmutableList.of(id, name, location, state, services, exportedPkgs, importedPkgs, refs, refLists);
    }

    public static class ListImpl extends BaseListField<BundleField> {

        public static final String DEFAULT_FIELD_NAME = "bundles";


        public ListImpl() {
            super(DEFAULT_FIELD_NAME);
        }

        @Override
        public Callable<BundleField> getCreateListEntryCallable() {
            return () -> new BundleField();
        }

        @Override
        public ListImpl addAll(Collection<BundleField> values) {
            super.addAll(values);
            return this;
        }
    }
}
