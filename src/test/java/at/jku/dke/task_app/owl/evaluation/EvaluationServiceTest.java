package at.jku.dke.task_app.owl.evaluation;

import at.jku.dke.etutor.task_app.dto.SubmissionMode;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import at.jku.dke.task_app.owl.DatabaseSetupExtension;
import at.jku.dke.task_app.owl.data.entities.OWLTask;
import at.jku.dke.task_app.owl.data.entities.OWLTaskGroup;
import at.jku.dke.task_app.owl.data.repositories.OWLTaskGroupRepository;
import at.jku.dke.task_app.owl.data.repositories.OWLTaskRepository;
import at.jku.dke.task_app.owl.dto.OWLSubmissionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(DatabaseSetupExtension.class)
class EvaluationServiceTest {

    @Autowired
    private EvaluationService evaluationService;
    @Autowired
    private OWLTaskGroupRepository taskGroupRepository;
    @Autowired
    private OWLTaskRepository taskRepository;
    private long taskId;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        taskGroupRepository.deleteAll();

        var taskGroup = taskGroupRepository.save(new OWLTaskGroup(1L, TaskStatus.APPROVED));
        var task = taskRepository.save(new OWLTask(1L, BigDecimal.TEN, TaskStatus.APPROVED, taskGroup,
            "Class: Person\nSubClassOf: Human\nDisjointWith: Animal"));
        this.taskId = task.getId();
    }

    @Test
    void evaluateRun() {
        // Arrange
        SubmitSubmissionDto<OWLSubmissionDto> dto = new SubmitSubmissionDto<>("test-user", "test-assignment", taskId,
            "en", SubmissionMode.RUN, 3, new OWLSubmissionDto("Class: Person\nSubClassOf: Human\nDisjointWith: Animal"));

        // Act
        var result = evaluationService.evaluate(dto);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO.stripTrailingZeros(), result.points().stripTrailingZeros());
        assertEquals(BigDecimal.TEN.stripTrailingZeros(), result.maxPoints().stripTrailingZeros());
        assertEquals("Submitted OWL ontology: Class: Person\nSubClassOf: Human\nDisjointWith: Animal", result.generalFeedback());
        /*assertTrue(result.criteria().stream().anyMatch(x ->
            x.name().equals("criterium.syntax") &&
            x.feedback().equals("criterium.syntax.valid")));*/
        assertEquals(1, result.criteria().size());
    }

    @Test
    void evaluateSubmitValid() {
        // Arrange
        SubmitSubmissionDto<OWLSubmissionDto> dto = new SubmitSubmissionDto<>("test-user", "test-assignment", taskId,
            "en", SubmissionMode.SUBMIT, 3, new OWLSubmissionDto("Class: Person\nSubClassOf: Human\nDisjointWith: Animal"));

        // Act
        var result = evaluationService.evaluate(dto);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.TEN.stripTrailingZeros(), result.points().stripTrailingZeros());
        assertEquals(BigDecimal.TEN.stripTrailingZeros(), result.maxPoints().stripTrailingZeros());
        assertEquals("The ontology is correct.", result.generalFeedback());
        /*assertTrue(result.criteria().stream().anyMatch(x ->
            x.name().equals("criterium.syntax") &&
            x.feedback().equals("criterium.syntax.valid")));*/
        assertEquals(1, result.criteria().size());
    }

    @Test
    void evaluateSubmitInvalidSyntax() {
        // Arrange
        SubmitSubmissionDto<OWLSubmissionDto> dto = new SubmitSubmissionDto<>("test-user", "test-assignment", taskId,
            "en", SubmissionMode.SUBMIT, 3, new OWLSubmissionDto("Claaa: PersonSubClassOf: Human\nDisjointWith: Animal"));

        // Act
        var result = evaluationService.evaluate(dto);

        // Assert
        assertNotNull(result);
        //assertTrue(result.criteria().stream().anyMatch(x -> x.name().equals("criterium.syntax")));
        assertEquals("Invalid Manchester syntax.", result.generalFeedback());
        assertEquals(BigDecimal.ZERO.stripTrailingZeros(), result.points().stripTrailingZeros());
        assertEquals(BigDecimal.TEN.stripTrailingZeros(), result.maxPoints().stripTrailingZeros());
        assertEquals(1, result.criteria().size());
    }

    @Test
    void evaluateDiagnoseValid() {
        // Arrange
        SubmitSubmissionDto<OWLSubmissionDto> dto = new SubmitSubmissionDto<>("test-user", "test-assignment", taskId,
            "en", SubmissionMode.DIAGNOSE, 3, new OWLSubmissionDto("Class: Person\nSubClassOf: Human\nDisjointWith: Animal"));

        // Act
        var result = evaluationService.evaluate(dto);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.TEN.stripTrailingZeros(), result.points().stripTrailingZeros());
        assertEquals(BigDecimal.TEN.stripTrailingZeros(), result.maxPoints().stripTrailingZeros());
        assertEquals("The ontology is correct.", result.generalFeedback());
        assertEquals(4, result.criteria().size()); // Syntax + Class hierarchy + Object properties + Data properties
        /*assertTrue(result.criteria().stream().anyMatch(x ->
            x.name().equals("criterium.syntax") &&
            x.feedback().equals("criterium.syntax.valid")));
        assertTrue(result.criteria().stream().anyMatch(x ->
            x.name().equals("criterium.classes") &&
            x.feedback().equals("criterium.classes.match")));*/
    }

    @Test
    void evaluateDiagnoseInvalidSyntax() {
        // Arrange
        SubmitSubmissionDto<OWLSubmissionDto> dto = new SubmitSubmissionDto<>("test-user", "test-assignment", taskId,
            "en", SubmissionMode.DIAGNOSE, 3, new OWLSubmissionDto("Claaa: Person\nSubClassOf: Human\nDisjointWith: Animal"));

        // Act
        var result = evaluationService.evaluate(dto);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO.stripTrailingZeros(), result.points().stripTrailingZeros());
        assertEquals(BigDecimal.TEN.stripTrailingZeros(), result.maxPoints().stripTrailingZeros());
        assertEquals("Invalid Manchester syntax.", result.generalFeedback());
        assertEquals(1, result.criteria().size());
        //assertTrue(result.criteria().stream().anyMatch(x -> x.name().equals("criterium.syntax")));
    }

    @Test
    void evaluateDiagnoseNoFeedback() {
        // Arrange
        SubmitSubmissionDto<OWLSubmissionDto> dto = new SubmitSubmissionDto<>("test-user", "test-assignment", taskId,
            "en", SubmissionMode.DIAGNOSE, 0, new OWLSubmissionDto("Class: Orange\nSubClassOf: Human\nDisjointWith: Animal"));

        // Act
        var result = evaluationService.evaluate(dto);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO.stripTrailingZeros(), result.points().stripTrailingZeros());
        assertEquals(BigDecimal.TEN.stripTrailingZeros(), result.maxPoints().stripTrailingZeros());
        assertEquals("Your solution is incorrect.", result.generalFeedback());
        assertEquals(1, result.criteria().size());
        assertTrue(result.criteria().stream().anyMatch(x ->
            x.name().equals("criterium.syntax") &&
            x.feedback().equals("criterium.syntax.valid")));
    }
}
