package com.github.sylvainlaurent.maven.swaggervalidator.semantic.validator.definition;

import com.github.sylvainlaurent.maven.swaggervalidator.semantic.node.VisitableModel;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.node.model.ComposedModelWrapper;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.node.model.ModelImplWrapper;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.node.model.RefModelWrapper;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.validator.ValidationContext;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.validator.error.DefinitionSemanticError;
import io.swagger.models.RefModel;
import io.swagger.models.properties.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.sylvainlaurent.maven.swaggervalidator.semantic.VisitableModelFactory.createVisitableModel;
import static java.util.Collections.disjoint;

public class InheritanceChainPropertiesValidator extends ModelValidatorTemplate {

    protected ValidationContext context;

    @Override
    public void validate(ComposedModelWrapper composedModel) {
        Collection<String> childProperties = composedModel.getChild() == null || composedModel.getChild().getProperties() == null ?
                                             Collections.<String>emptyList() :
                                             composedModel.getChild().getProperties().keySet();
        List<String> parentProperties = new ParentPropertiesCollector(composedModel).getParentProperties();

        boolean childHasPropertiesAlreadyPresentInParent = !disjoint(childProperties, parentProperties);
        if (childHasPropertiesAlreadyPresentInParent) {
            validationErrors.add(new DefinitionSemanticError(
                holder.getCurrentPath(), "following properties are already defined in ancestors: "
                    + findCommonProperties(childProperties, parentProperties)));
        }
    }

    private List<String> findCommonProperties(Collection<String> propertyNames, Collection<String> parentPropertyNames) {
        List<String> commonProperties = new ArrayList<>(propertyNames);
        commonProperties.retainAll(parentPropertyNames);
        return commonProperties;
    }

    @Override
    public void setValidationContext(ValidationContext context) {
        this.context = context;
    }

    final class ParentPropertiesCollector extends ModelValidatorTemplate {

        private List<String> parentProperties = new ArrayList<>();

        private VisitableModel root;

        ParentPropertiesCollector(VisitableModel root) {
            this.root = root;
        }

        @Override
        public void validate(ModelImplWrapper modelImplWrapper) {
            if (modelImplWrapper.getProperties() != null) {
                parentProperties.addAll(modelImplWrapper.getProperties().keySet());
            }
        }

        @Override
        public void validate(RefModelWrapper refModelWrapper) {
            String ref = refModelWrapper.getSimpleRef();
            createVisitableModel(ref, context.getDefinitions().get(ref)).accept(this);
        }

        @Override
        public void validate(ComposedModelWrapper composedModelWrapper) {
            if (composedModelWrapper != root) {
                Map<String, Property> childProperties = composedModelWrapper.getChild().getProperties() != null ?
                                                        composedModelWrapper.getChild().getProperties() :
                                                        Collections.<String, Property>emptyMap();
                parentProperties.addAll(childProperties.keySet());
            }

            for (RefModel element : composedModelWrapper.getInterfaces()) {
                createVisitableModel(element.getSimpleRef(), element).accept(this);
            }
        }

        List<String> getParentProperties() {
            if (parentProperties.isEmpty()) {
                root.accept(this);
            }
            return parentProperties;
        }

        @Override
        public void setValidationContext(ValidationContext context) {}
    }
}
