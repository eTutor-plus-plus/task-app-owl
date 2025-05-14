package at.jku.dke.task_app.owl.data.entities;

import at.jku.dke.etutor.task_app.dto.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OWLTaskGroupTest {

    @Test
    void testConstructor1() {
        // Arrange
        final String expectedSolution = "Class: Person\nSubClassOf: Human\nDisjointWith: Animal";

        // Act
        OWLTaskGroup OWLTaskGroup = new OWLTaskGroup(expectedSolution);
        String actualSolution = OWLTaskGroup.getSolution();

        // Assert
        assertEquals(expectedSolution, actualSolution);
    }

    @Test
    void testConstructor2() {
        // Arrange
        final TaskStatus status = TaskStatus.APPROVED;
        final String expectedSolution = "Class: Person\nSubClassOf: Human\nDisjointWith: Animal";

        // Act
        OWLTaskGroup OWLTaskGroup = new OWLTaskGroup(status, expectedSolution);
        TaskStatus actualStatus = OWLTaskGroup.getStatus();
        String actualSolution = OWLTaskGroup.getSolution();

        // Assert
        assertEquals(status, actualStatus);
        assertEquals(expectedSolution, actualSolution);
    }

    @Test
    void testConstructor3() {
        // Arrange
        final long expectedId = 21;
        final TaskStatus status = TaskStatus.APPROVED;
        final String expectedSolution = "Class: Person\nSubClassOf: Human\nDisjointWith: Animal";

        // Act
        OWLTaskGroup OWLTaskGroup = new OWLTaskGroup(expectedId, status, expectedSolution);
        long actualId = OWLTaskGroup.getId();
        TaskStatus actualStatus = OWLTaskGroup.getStatus();
        String actualSolution = OWLTaskGroup.getSolution();

        // Assert
        assertEquals(expectedId, actualId);
        assertEquals(status, actualStatus);
        assertEquals(expectedSolution, actualSolution);
    }

    @Test
    void testGetSetSolution() {
        // Arrange
        OWLTaskGroup OWLTaskGroup = new OWLTaskGroup();
        final String expected = "Class: Person\nSubClassOf: Human\nDisjointWith: Animal";

        // Act
        OWLTaskGroup.setSolution(expected);
        String actual = OWLTaskGroup.getSolution();

        // Assert
        assertEquals(expected, actual);
    }
}
