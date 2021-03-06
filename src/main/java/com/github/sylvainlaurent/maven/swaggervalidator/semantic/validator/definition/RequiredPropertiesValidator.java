package com.github.sylvainlaurent.maven.swaggervalidator.semantic.validator.definition;

import com.github.sylvainlaurent.maven.swaggervalidator.semantic.VisitableModelFactory;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.VisitablePropertyFactory;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.node.VisitableProperty;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.node.model.ArrayModelWrapper;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.node.model.ComposedModelWrapper;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.node.model.ModelImplWrapper;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.node.property.ArrayPropertyWrapper;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.node.property.ObjectPropertyWrapper;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.validator.ValidationContext;
import com.github.sylvainlaurent.maven.swaggervalidator.semantic.validator.error.DefinitionSemanticError;
import com.github.sylvainlaurent.maven.swaggervalidator.util.Util;
import io.swagger.models.Model;
import io.swagger.models.RefModel;
import io.swagger.models.properties.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class RequiredPropertiesValidator extends ModelValidatorTemplate {

    @Override
    public void validate(ModelImplWrapper modelImplWrapper) {
        List<String> objectProperties = new ArrayList<>(modelImplWrapper.getProperties().keySet());
        List<String> requiredProperties = modelImplWrapper.getRequired();

        validateDiscriminator(modelImplWrapper.getDiscriminator(), requiredProperties, objectProperties);
        validateProperties(objectProperties, requiredProperties);

        for (Map.Entry<String, VisitableProperty> property : modelImplWrapper.getProperties().entrySet()) {
            property.getValue().accept(this);
        }
    }

    private void validateDiscriminator(String discriminator, List<String> requiredProperties, List<String> properties) {
        if (isEmpty(discriminator)) {
            return;
        }

        if (!properties.contains(discriminator)) {
            validationErrors.add(new DefinitionSemanticError(holder.getCurrentPath(),
                    "discriminator \"" + discriminator + "\" is not a property defined at this schema"));
        }

        if (!requiredProperties.contains(discriminator)) {
            validationErrors.add(new DefinitionSemanticError(holder.getCurrentPath(),
                    "discriminator property \"" + discriminator + "\" is not marked as required"));
        }
    }

    private void validateProperties(Collection<String> objectProperties, List<String> required) {
        Set<String> duplicates = Util.findDuplicates(required);
        if (!duplicates.isEmpty()) {
            validationErrors.add(new DefinitionSemanticError(holder.getCurrentPath(),
                    "required property is defined multiple times: " + duplicates));
        }

        if (objectProperties.containsAll(required)) {
            return;
        }

        List<String> requiredButNotDefinedProperties = new ArrayList<>(required);
        requiredButNotDefinedProperties.removeAll(objectProperties);
        validationErrors.add(new DefinitionSemanticError(holder.getCurrentPath(),
                "required properties are not defined as object properties: " + requiredButNotDefinedProperties));
    }

    @Override
    public void validate(ArrayModelWrapper arrayModelWrapper) {
        if (arrayModelWrapper.getItems() == null) {
            validationErrors.add(new DefinitionSemanticError(holder.getCurrentPath(),
                    "'items' must be defined for an array"));
            return;
        }
        arrayModelWrapper.getItems().accept(this);
    }

    @Override
    public void validate(ComposedModelWrapper composedModelWrapper) {
        for (Model element : composedModelWrapper.getAllOf()) {
            String modelName = element instanceof RefModel ? ((RefModel) element).getSimpleRef() : "child";
            VisitableModelFactory.createVisitableModel(modelName, element).accept(this);
        }
    }

    @Override
    public void validate(ObjectPropertyWrapper objectProperty) {
        validateProperties(objectProperty.getProperties().keySet(), objectProperty.getRequiredProperties());

        for (Map.Entry<String, Property> property : objectProperty.getProperties().entrySet()) {
            VisitablePropertyFactory
                    .createVisitableProperty(property.getKey(), property.getValue()).accept(this);
        }
    }

    @Override
    public void validate(ArrayPropertyWrapper arrayProperty) {
        if (arrayProperty.getItems() == null) {
            validationErrors.add(new DefinitionSemanticError(holder.getCurrentPath(),
                    "'items' must be defined for an array"));
            return;
        }

        VisitablePropertyFactory.createVisitableProperty("items", arrayProperty.getItems())
                .accept(this);
    }

    @Override
    public void setValidationContext(ValidationContext context) {}
}
