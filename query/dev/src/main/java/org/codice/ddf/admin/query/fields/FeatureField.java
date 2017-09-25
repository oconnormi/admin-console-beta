package org.codice.ddf.admin.query.fields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.codice.ddf.admin.api.Field;
import org.codice.ddf.admin.common.fields.base.BaseListField;
import org.codice.ddf.admin.common.fields.base.BaseObjectField;
import org.codice.ddf.admin.common.fields.base.scalar.StringField;
import org.codice.ddf.admin.common.fields.common.UrlField;

import com.google.common.collect.ImmutableList;

public class FeatureField extends BaseObjectField {

    public static final String DEFAULT_FIELD_NAME  = "feature";
    public static final String FIELD_TYPE_NAME = "Feature";
    public static final String DESCRIPTION = "TODO";
    public static final String NAME = "name";
    public static final String STATE = "state";
    public static final String DESCRIPTION_FIELD = "description";
    public static final String ID = "id";
    public static final String REPO_URL = "repoUrl";
    public static final String FEATURE_DEPS = "featureDeps";
    public static final String BUNDLE_DEPS = "bundleDeps";

    private StringField id;
    private StringField name;
    private StringField state;
    private StringField description;
    private UrlField repoUrl;
    private StringField.ListImpl featDeps;
    private StringField.ListImpl bundleDeps;

    public FeatureField() {
        super(DEFAULT_FIELD_NAME, FIELD_TYPE_NAME, DESCRIPTION);
        id = new StringField(ID);
        name = new StringField(NAME);
        state = new StringField(STATE);
        description = new StringField(DESCRIPTION_FIELD);
        repoUrl = new UrlField(REPO_URL);
        featDeps = new StringField.ListImpl(FEATURE_DEPS);
        bundleDeps = new StringField.ListImpl(BUNDLE_DEPS);

        updateInnerFieldPaths();
    }

    public FeatureField name(String name) {
        this.name.setValue(name);
        return this;
    }

    public FeatureField state(String state) {
        this.state.setValue(state);
        return this;
    }

    public FeatureField description(String description) {
        this.description.setValue(description);
        return this;
    }

    public FeatureField id(String id) {
        this.id.setValue(id);
        return this;
    }

    public FeatureField repoUrl(String uri) {
        this.repoUrl.setValue(uri);
        return this;
    }

    public FeatureField addFeatureDeps(List<String> feats) {
        feats.forEach(str -> {
            StringField f = new StringField();
            f.setValue(str);
            featDeps.add(f);
        });
        return this;
    }

    public FeatureField addBundleDeps(List<String> bundles) {
        bundles.forEach(str -> {
            StringField b = new StringField();
            b.setValue(str);
            bundleDeps.add(b);
        });
        return this;
    }

    @Override
    public List<Field> getFields() {
        return ImmutableList.of(id, name, state, description, repoUrl, featDeps, bundleDeps);
    }

    public static class ListImpl extends BaseListField<FeatureField> {

        public static final String DEFAULT_FIELD_NAME = "features";


        public ListImpl() {
            super(DEFAULT_FIELD_NAME);
        }

        public ListImpl(String fieldName) {
            super(fieldName);
        }

        @Override
        public Callable<FeatureField> getCreateListEntryCallable() {
            return () -> new FeatureField();
        }

        @Override
        public ListImpl addAll(Collection<FeatureField> values) {
            super.addAll(values);
            return this;
        }
    }
}
