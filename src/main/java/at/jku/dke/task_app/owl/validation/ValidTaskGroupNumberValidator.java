package at.jku.dke.task_app.owl.validation;

import at.jku.dke.task_app.owl.dto.ModifyOWLTaskGroupDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Custom validator for numbers in {@link ModifyOWLTaskGroupDto}.
 */
public class ValidTaskGroupNumberValidator implements ConstraintValidator<ValidTaskGroupNumber, ModifyOWLTaskGroupDto> {
    /**
     * Creates a new instance of class Valid task group number validator.
     */
    public ValidTaskGroupNumberValidator() {
    }

    @Override
    public boolean isValid(ModifyOWLTaskGroupDto value, ConstraintValidatorContext context) {
        return true;
    }
}
