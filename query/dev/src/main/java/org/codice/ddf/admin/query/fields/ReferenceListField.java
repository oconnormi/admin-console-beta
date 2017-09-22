package org.codice.ddf.admin.query.fields;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.codice.ddf.admin.api.Field;
import org.codice.ddf.admin.common.fields.base.BaseListField;
import org.codice.ddf.admin.common.fields.base.BaseObjectField;
import org.codice.ddf.admin.common.fields.base.scalar.StringField;

import com.google.common.collect.ImmutableList;

public class ReferenceListField extends BaseObjectField {

    public static final String DEFAULT_FIELD_NAME = "refList";

    public static final String FIELD_TYPE_NAME = "ServiceReferenceList";

    public static final String FILTER = "filter";

    public static final String RESOLUTION = "resolution";

    public static final String INTERFACE = "interface";

    public static final String DESCRIPTION = "TODO";

    private ServiceField.ListImpl services;
    private StringField filter;
    private StringField resolution;
    private StringField interfaceF;

    public ReferenceListField() {
        super(DEFAULT_FIELD_NAME, FIELD_TYPE_NAME, DESCRIPTION);
        services = new ServiceField.ListImpl();
        filter = new StringField(FILTER);
        resolution = new StringField(RESOLUTION);
        interfaceF = new StringField(INTERFACE);
        updateInnerFieldPaths();
    }

    @Override
    public List<Field> getFields() {
        return ImmutableList.of(services, filter, resolution, interfaceF);
    }

    public ReferenceListField filter(String filter) {
        this.filter.setValue(filter);
        return this;
    }

    public ReferenceListField interfacee(String interfacee) {
        this.interfaceF.setValue(interfacee);
        return this;
    }

    public ReferenceListField addService(ServiceField serviceField) {
        services.add(serviceField);
        return this;
    }

    public ReferenceListField resolution(long resolution) {
        this.resolution.setValue(resolution(Math.toIntExact(resolution)));
        return this;
    }

    private static String resolution(int i) {
        return i == 1 ? "mandatory" : "optional";
    }

    public static class ListImpl extends BaseListField<ReferenceListField> {

        public static final String DEFAULT_FIELD_NAME = "refLists";


        public ListImpl() {
            super(DEFAULT_FIELD_NAME);
        }

        @Override
        public Callable<ReferenceListField> getCreateListEntryCallable() {
            return ReferenceListField::new;
        }

        @Override
        public ListImpl addAll(Collection<ReferenceListField> values) {
            super.addAll(values);
            return this;
        }
    }
}
