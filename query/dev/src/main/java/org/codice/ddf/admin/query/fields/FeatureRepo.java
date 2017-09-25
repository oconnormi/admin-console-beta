package org.codice.ddf.admin.query.fields;

import java.util.List;

import org.codice.ddf.admin.api.Field;
import org.codice.ddf.admin.common.fields.base.BaseObjectField;

public class FeatureRepo extends BaseObjectField {

    public static final String DEFAULT_FIELD_NAME = "featureRepo";
    public static final String FIELD_TYPE_NAME  ="FeatureRepository";
    public static final String DESCRIPTION = "Holds locations of features.";

    public FeatureRepo() {
        super(DEFAULT_FIELD_NAME, FIELD_TYPE_NAME, DESCRIPTION);
    }

    @Override
    public List<Field> getFields() {
        return null;
    }
}
