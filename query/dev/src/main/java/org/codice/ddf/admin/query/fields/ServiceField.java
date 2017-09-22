package org.codice.ddf.admin.query.fields;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.codice.ddf.admin.api.Field;
import org.codice.ddf.admin.common.fields.base.BaseListField;
import org.codice.ddf.admin.common.fields.base.BaseObjectField;
import org.codice.ddf.admin.common.fields.base.scalar.IntegerField;
import org.codice.ddf.admin.common.fields.base.scalar.StringField;
import org.osgi.framework.ServiceReference;

import com.google.common.collect.ImmutableList;

public class ServiceField extends BaseObjectField {

    public static final String DEFAULT_FIELD_NAME = "service";

    public static final String FIELD_TYPE_NAME = "Service";

    public static final String DESCRIPTION = "Some service running.";

    public static final String BUNDLE_ID = "bundleId";

    public static final String NAME = "name";

    private StringField name;

    private IntegerField bundleId;

    public ServiceField() {
        super(DEFAULT_FIELD_NAME, FIELD_TYPE_NAME, DESCRIPTION);
        name = new StringField(NAME);
        bundleId = new IntegerField(BUNDLE_ID);
        updateInnerFieldPaths();
    }

    public ServiceField(ServiceReference ref) {
        this();
        name(ref.toString());
        bundleId(ref.getBundle().getBundleId());
    }

    public ServiceField name(String name) {
        this.name.setValue(name);
        return this;
    }

    public ServiceField bundleId(int bundleId) {
        this.bundleId.setValue(bundleId);
        return this;
    }

    public ServiceField bundleId(long bundleId) {
        this.bundleId.setValue(Math.toIntExact(bundleId));
        return this;
    }

    @Override
    public List<Field> getFields() {
        return ImmutableList.of(name, bundleId);
    }

    public static class ListImpl extends BaseListField<ServiceField> {

        public static final String DEFAULT_FIELD_NAME = "services";


        public ListImpl() {
            super(DEFAULT_FIELD_NAME);
        }

        @Override
        public Callable<ServiceField> getCreateListEntryCallable() {
            return ServiceField::new;
        }

        @Override
        public ListImpl addAll(Collection<ServiceField> values) {
            super.addAll(values);
            return this;
        }
    }
}
