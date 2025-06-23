package at.jku.dke.task_app.owl.data.entities;

import at.jku.dke.etutor.task_app.dto.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OWLTaskGroupTest {

    @Test
    void testConstructor1() {
        // Arrange
        final TaskStatus status = TaskStatus.APPROVED;

        // Act
        OWLTaskGroup OWLTaskGroup = new OWLTaskGroup(status);
        TaskStatus actualStatus = OWLTaskGroup.getStatus();

        // Assert
        assertEquals(status, actualStatus);
    }

    @Test
    void testConstructor2() {
        // Arrange
        final long expectedId = 21;
        final TaskStatus status = TaskStatus.APPROVED;

        // Act
        OWLTaskGroup OWLTaskGroup = new OWLTaskGroup(expectedId, status);
        long actualId = OWLTaskGroup.getId();
        TaskStatus actualStatus = OWLTaskGroup.getStatus();

        // Assert
        assertEquals(expectedId, actualId);
        assertEquals(status, actualStatus);
    }
}
