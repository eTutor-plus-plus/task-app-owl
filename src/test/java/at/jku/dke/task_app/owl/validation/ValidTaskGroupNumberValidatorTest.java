package at.jku.dke.task_app.owl.validation;

import at.jku.dke.task_app.owl.dto.ModifyOWLTaskGroupDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidTaskGroupNumberValidatorTest {

    @Test
    void isValidCorrectOrder() {
        // Arrange
        ValidTaskGroupNumberValidator validTaskGroupNumberValidator = new ValidTaskGroupNumberValidator();
        ModifyOWLTaskGroupDto modifyOWLTaskGroupDto = new ModifyOWLTaskGroupDto(1, 2);

        // Act
        boolean result = validTaskGroupNumberValidator.isValid(modifyOWLTaskGroupDto, null);

        // Assert
        assertTrue(result);
    }

    @Test
    void isValidSameValue() {
        // Arrange
        ValidTaskGroupNumberValidator validTaskGroupNumberValidator = new ValidTaskGroupNumberValidator();
        ModifyOWLTaskGroupDto modifyOWLTaskGroupDto = new ModifyOWLTaskGroupDto(2, 2);

        // Act
        boolean result = validTaskGroupNumberValidator.isValid(modifyOWLTaskGroupDto, null);

        // Assert
        assertFalse(result);
    }

    @Test
    void isValidIncorrectOrder() {
        // Arrange
        ValidTaskGroupNumberValidator validTaskGroupNumberValidator = new ValidTaskGroupNumberValidator();
        ModifyOWLTaskGroupDto modifyOWLTaskGroupDto = new ModifyOWLTaskGroupDto(2, 1);

        // Act
        boolean result = validTaskGroupNumberValidator.isValid(modifyOWLTaskGroupDto, null);

        // Assert
        assertFalse(result);
    }
}
