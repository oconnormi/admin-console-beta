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
package org.codice.ddf.admin.query;

import java.util.ArrayList;
import java.util.List;

import org.codice.ddf.admin.api.fields.FunctionField;
import org.codice.ddf.admin.common.fields.base.function.BaseFieldProvider;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.codice.ddf.admin.query.discover.GetBundles;
import org.codice.ddf.admin.query.discover.GetFeatures;

import com.google.common.collect.ImmutableList;

public class DeveloperToolsFieldProvider extends BaseFieldProvider {

    public static final String FIELD_NAME = "dev";

    public static final String FIELD_TYPE_NAME = "DeveloperTools";

    public static final String DESCRIPTION = "Awesome developer tools!";

    private GetBundles getBundles;
    private GetFeatures getFeatures;

    public DeveloperToolsFieldProvider(ConfigurationAdmin configAdmin) {
        super(FIELD_NAME, FIELD_TYPE_NAME, DESCRIPTION);
        getBundles = new GetBundles(configAdmin);
        getFeatures = new GetFeatures();
        updateInnerFieldPaths();
    }

    @Override
    public List<FunctionField> getDiscoveryFunctions() {
        return ImmutableList.of(getBundles, getFeatures);
    }

    @Override
    public List<FunctionField> getMutationFunctions() {
        return new ArrayList<>();
    }
}
