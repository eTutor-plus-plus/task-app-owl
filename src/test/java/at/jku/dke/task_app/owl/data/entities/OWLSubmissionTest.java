package at.jku.dke.task_app.owl.data.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OWLSubmissionTest {

    @Test
    void testConstructor() {
        // Arrange
        var expected = "test";

        // Act
        var submission = new OWLSubmission(expected);
        var actual = submission.getSubmission();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetSetSubmission() {
        // Arrange
        var submission = new OWLSubmission();
        var expected = "test";

        // Act
        submission.setSubmission(expected);
        var actual = submission.getSubmission();

        // Assert
        assertEquals(expected, actual);
    }

}
