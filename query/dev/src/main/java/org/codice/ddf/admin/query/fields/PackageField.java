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

public class PackageField extends BaseObjectField {

    public static final String DEFAULT_FIELD_NAME = "pkg";

    public static final String FIELD_TYPE_NAME = "Package";

    public static final String DESCRIPTION = "TODO";

    public static final String BUNDLE_ID = "bundleId";

    public static final String NAME = "name";

    private StringField name;

    private IntegerField bundleId;

    public PackageField() {
        super(DEFAULT_FIELD_NAME, FIELD_TYPE_NAME, DESCRIPTION);
        name = new StringField(NAME);
        bundleId = new IntegerField(BUNDLE_ID);
        updateInnerFieldPaths();
    }

    public PackageField name(String name) {
        this.name.setValue(name);
        return this;
    }

    public PackageField bundleId(int bundleId) {
        this.bundleId.setValue(bundleId);
        return this;
    }

    @Override
    public List<Field> getFields() {
        return ImmutableList.of(name, bundleId);
    }

    public static class ListImpl extends BaseListField<PackageField> {

        public static final String DEFAULT_FIELD_NAME = "packages";


        public ListImpl() {
            super(DEFAULT_FIELD_NAME);
        }

        public ListImpl(String fieldName) {
            super(fieldName);
        }

        @Override
        public Callable<PackageField> getCreateListEntryCallable() {
            return PackageField::new;
        }

        @Override
        public ListImpl addAll(Collection<PackageField> values) {
            super.addAll(values);
            return this;
        }
    }
}
