package at.jku.dke.task_app.owl.data.entities;

import at.jku.dke.etutor.task_app.dto.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OWLTaskGroupTest {

    @Test
    void testConstructor1() {
        // Arrange
        final int expectedMinNumber = 21;
        final int expectedMaxNumber = 42;

        // Act
        OWLTaskGroup OWLTaskGroup = new OWLTaskGroup(expectedMinNumber, expectedMaxNumber);
        int actualMinNumber = OWLTaskGroup.getMinNumber();
        int actualMaxNumber = OWLTaskGroup.getMaxNumber();

        // Assert
        assertEquals(expectedMinNumber, actualMinNumber);
        assertEquals(expectedMaxNumber, actualMaxNumber);
    }

    @Test
    void testConstructor2() {
        // Arrange
        final TaskStatus status = TaskStatus.APPROVED;
        final int expectedMinNumber = 21;
        final int expectedMaxNumber = 42;

        // Act
        OWLTaskGroup OWLTaskGroup = new OWLTaskGroup(status, expectedMinNumber, expectedMaxNumber);
        TaskStatus actualStatus = OWLTaskGroup.getStatus();
        int actualMinNumber = OWLTaskGroup.getMinNumber();
        int actualMaxNumber = OWLTaskGroup.getMaxNumber();

        // Assert
        assertEquals(status, actualStatus);
        assertEquals(expectedMinNumber, actualMinNumber);
        assertEquals(expectedMaxNumber, actualMaxNumber);
    }

    @Test
    void testConstructor3() {
        // Arrange
        final long expectedId = 21;
        final TaskStatus status = TaskStatus.APPROVED;
        final int expectedMinNumber = 21;
        final int expectedMaxNumber = 42;

        // Act
        OWLTaskGroup OWLTaskGroup = new OWLTaskGroup(expectedId, status, expectedMinNumber, expectedMaxNumber);
        long actualId = OWLTaskGroup.getId();
        TaskStatus actualStatus = OWLTaskGroup.getStatus();
        int actualMinNumber = OWLTaskGroup.getMinNumber();
        int actualMaxNumber = OWLTaskGroup.getMaxNumber();

        // Assert
        assertEquals(expectedId, actualId);
        assertEquals(status, actualStatus);
        assertEquals(expectedMinNumber, actualMinNumber);
        assertEquals(expectedMaxNumber, actualMaxNumber);
    }

    @Test
    void testGetSetMinNumber() {
        // Arrange
        OWLTaskGroup OWLTaskGroup = new OWLTaskGroup();
        final int expected = 21;

        // Act
        OWLTaskGroup.setMinNumber(expected);
        int actual = OWLTaskGroup.getMinNumber();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetSetMaxNumber() {
        // Arrange
        OWLTaskGroup OWLTaskGroup = new OWLTaskGroup();
        final int expected = 21;

        // Act
        OWLTaskGroup.setMaxNumber(expected);
        int actual = OWLTaskGroup.getMaxNumber();

        // Assert
        assertEquals(expected, actual);
    }

}
