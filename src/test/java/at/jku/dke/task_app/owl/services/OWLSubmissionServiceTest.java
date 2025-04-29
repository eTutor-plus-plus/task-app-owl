package at.jku.dke.task_app.owl.services;

import at.jku.dke.etutor.task_app.dto.SubmissionMode;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.owl.data.entities.OWLSubmission;
import at.jku.dke.task_app.owl.dto.OWLSubmissionDto;
import at.jku.dke.task_app.owl.evaluation.EvaluationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OWLSubmissionServiceTest {

    @Test
    void createSubmissionEntity() {
        // Arrange
        SubmitSubmissionDto<OWLSubmissionDto> dto = new SubmitSubmissionDto<>("test-user", "test-quiz", 3L, "de", SubmissionMode.SUBMIT, 2, new OWLSubmissionDto("33"));
        OWLSubmissionService service = new OWLSubmissionService(null, null, null);

        // Act
        OWLSubmission submission = service.createSubmissionEntity(dto);

        // Assert
        assertEquals(dto.submission().input(), submission.getSubmission());
    }

    @Test
    void mapSubmissionToSubmissionData() {
        // Arrange
        OWLSubmission submission = new OWLSubmission("33");
        OWLSubmissionService service = new OWLSubmissionService(null, null, null);

        // Act
        OWLSubmissionDto dto = service.mapSubmissionToSubmissionData(submission);

        // Assert
        assertEquals(submission.getSubmission(), dto.input());
    }

    @Test
    void evaluate() {
        // Arrange
        var evalService = mock(EvaluationService.class);
        SubmitSubmissionDto<OWLSubmissionDto> dto = new SubmitSubmissionDto<>("test-user", "test-quiz", 3L, "de", SubmissionMode.SUBMIT, 2, new OWLSubmissionDto("33"));
        OWLSubmissionService service = new OWLSubmissionService(null, null, evalService);

        // Act
        var result = service.evaluate(dto);

        // Assert
        verify(evalService).evaluate(dto);
    }

}
