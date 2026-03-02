package at.jku.dke.task_app.owl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import at.jku.dke.task_app.owl.data.entities.OWLTask;
import at.jku.dke.task_app.owl.dto.ModifyOWLTaskDto;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OWLTaskServiceTest {

    @Test
    void createTask() {
        // Arrange
        ModifyTaskDto<ModifyOWLTaskDto> dto = new ModifyTaskDto<>(7L, BigDecimal.TEN, "owl", TaskStatus.APPROVED, new ModifyOWLTaskDto("Class: Person\nSubClassOf: Human\nDisjointWith: Animal", "Person=3, Human=2, Animal=1", 1, 1));
        OWLTaskService service = new OWLTaskService(null, null, null);

        // Act
        OWLTask task = service.createTask(3, dto);

        // Assert
        assertEquals(dto.additionalData().solution(), task.getSolution());
    }

    @Test
    void createTaskInvalidType() {
        // Arrange
        ModifyTaskDto<ModifyOWLTaskDto> dto = new ModifyTaskDto<>(7L, BigDecimal.TEN, "sql", TaskStatus.APPROVED, new ModifyOWLTaskDto("Class: Person\nSubClassOf: Human\nDisjointWith: Animal", "Person=3, Human=2, Animal=1", 1, 1));
        OWLTaskService service = new OWLTaskService(null, null, null);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> service.createTask(3, dto));
    }

    @Test
    void updateTask() {
        // Arrange
        ModifyTaskDto<ModifyOWLTaskDto> dto = new ModifyTaskDto<>(7L, BigDecimal.TEN, "owl", TaskStatus.APPROVED, new ModifyOWLTaskDto("Class: Person\nSubClassOf: Human\nDisjointWith: Animal", "Person=3, Human=2, Animal=1", 1, 1));
        OWLTaskService service = new OWLTaskService(null, null, null);
        OWLTask task = new OWLTask("Class: Apple\nSubClassOf: Fruit\nDisjointWith: Orange", "Apple=3, Fruit=2, Orange=1", 1, 1);

        // Act
        service.updateTask(task, dto);

        // Assert
        assertEquals(dto.additionalData().solution(), task.getSolution());
    }

    @Test
    void updateTaskInvalidType() {
        // Arrange
        ModifyTaskDto<ModifyOWLTaskDto> dto = new ModifyTaskDto<>(7L, BigDecimal.TEN, "sql", TaskStatus.APPROVED, new ModifyOWLTaskDto("Class: Person\nSubClassOf: Human\nDisjointWith: Animal", "Person=3, Human=2, Animal=1", 1, 1));
        OWLTaskService service = new OWLTaskService(null, null, null);
        OWLTask task = new OWLTask("Class: Apple\nSubClassOf: Fruit\nDisjointWith: Orange", "Apple=3, Fruit=2, Orange=1", 1, 1);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> service.updateTask(task, dto));
    }

    @Test
    void mapToReturnData() {
        // Arrange
        MessageSource ms = mock(MessageSource.class);
        OWLTaskService service = new OWLTaskService(null, null, ms);
        OWLTask task = new OWLTask("Class: Apple\nSubClassOf: Fruit\nDisjointWith: Orange", "Apple=3, Fruit=2, Orange=1", 1, 1);
        task.setSolution("Class: Person\nSubClassOf: Human\nDisjointWith: Animal");

        // Act
        TaskModificationResponseDto result = service.mapToReturnData(task, true);

        // Assert
        assertNotNull(result);
        verify(ms).getMessage("defaultTaskDescription", null, Locale.GERMAN);
        verify(ms).getMessage("defaultTaskDescription", null, Locale.ENGLISH);
    }

}
