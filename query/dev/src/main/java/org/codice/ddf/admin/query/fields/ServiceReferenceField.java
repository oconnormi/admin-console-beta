package org.codice.ddf.admin.query.fields;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.codice.ddf.admin.api.Field;
import org.codice.ddf.admin.common.fields.base.BaseListField;
import org.codice.ddf.admin.common.fields.base.BaseObjectField;
import org.codice.ddf.admin.common.fields.base.scalar.StringField;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;

import com.google.common.collect.ImmutableList;

public class ServiceReferenceField extends BaseObjectField {

    public static final String DEFAULT_FIELD_NAME = "ref";

    public static final String FIELD_TYPE_NAME = "ServiceReference";

    public static final String DESCRIPTION = " TODO";

    public static final String RESOLUTION = "resolution";

    public static final String FILTER = "filter";

    public static final String INTERFACE = "interface";

    private StringField interfacee;
    private StringField filter;
    private StringField resolution;
    private ServiceField service;

    public ServiceReferenceField() {
        super(DEFAULT_FIELD_NAME, FIELD_TYPE_NAME, DESCRIPTION);
        service = new ServiceField();
        resolution = new StringField(RESOLUTION);
        filter = new StringField(FILTER);
        interfacee = new StringField(INTERFACE);
        updateInnerFieldPaths();
    }

    public ServiceReferenceField service(ServiceField service) {
        this.service.setValue(service.getValue());
        return this;
    }

    public ServiceReferenceField filter(String filter) {
        this.filter.setValue(filter);
        return this;
    }

    public ServiceReferenceField interfacee(String interfacee) {
        this.interfacee.setValue(interfacee);
        return this;
    }

    public ServiceReferenceField resolution(long resolution) {
        this.resolution.setValue(resolution(Math.toIntExact(resolution)));
        return this;
    }

    private static String resolution(int i) {
        return i == 1 ? "mandatory" : "optional";
    }

    @Override
    public List<Field> getFields() {
        return ImmutableList.of(interfacee, filter, resolution, service);
    }

    public static class ListImpl extends BaseListField<ServiceReferenceField> {

        public static final String DEFAULT_FIELD_NAME = "refs";


        public ListImpl() {
            super(DEFAULT_FIELD_NAME);
        }

        @Override
        public Callable<ServiceReferenceField> getCreateListEntryCallable() {
            return ServiceReferenceField::new;
        }

        @Override
        public ListImpl addAll(Collection<ServiceReferenceField> values) {
            super.addAll(values);
            return this;
        }
    }
}
