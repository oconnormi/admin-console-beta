package org.codice.ddf.admin.query.discover;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.admin.api.fields.FunctionField;
import org.codice.ddf.admin.common.fields.base.function.GetFunctionField;
import org.codice.ddf.admin.query.fields.FeatureField;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class GetFeatures extends GetFunctionField<FeatureField.ListImpl> {

    public static final String FIELD_NAME = "getFeatures";
    public static final String DESCRIPTION = "Retrieves all features";

    public static final FeatureField.ListImpl RETURN_TYPE = new FeatureField.ListImpl();

    public GetFeatures() {
        super(FIELD_NAME, DESCRIPTION);
    }

    @Override
    public FeatureField.ListImpl performFunction() {
        FeaturesService featuresService = getFeaturesService();
        List<FeatureField> features = new ArrayList<>();
        try {
            for(Feature feat : featuresService.listFeatures()) {
                List<String> featDeps = feat.getDependencies().stream().map(Dependency::getName).collect(Collectors.toList());
                List<String> bundleDeps = feat.getBundles().stream().map(BundleInfo::getLocation).collect(
                        Collectors.toList());

                features.add(new FeatureField().name(feat.getName())
                        .description(feat.getDescription())
                        .state(feat.getInstall())
                        .id(feat.getId())
                        .repoUrl(feat.getRepositoryUrl())
                        .addFeatureDeps(featDeps)
                        .addBundleDeps(bundleDeps));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new FeatureField.ListImpl().addAll(features);
    }

    public FeaturesService getFeaturesService() {
        return getBundleContext().getService(getBundleContext().getServiceReference(FeaturesService.class));
    }

    public BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(GetFeatures.class).getBundleContext();
    }

    @Override
    public FeatureField.ListImpl getReturnType() {
        return RETURN_TYPE;
    }

    @Override
    public FunctionField newInstance() {
        return new GetFeatures();
    }

    @Override
    public Set<String> getFunctionErrorCodes() {
        return new HashSet<>();
    }
}
