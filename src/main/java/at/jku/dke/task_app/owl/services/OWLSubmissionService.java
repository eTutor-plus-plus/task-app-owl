package at.jku.dke.task_app.owl.services;

import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.etutor.task_app.services.BaseSubmissionService;
import at.jku.dke.task_app.owl.data.entities.OWLSubmission;
import at.jku.dke.task_app.owl.data.entities.OWLTask;
import at.jku.dke.task_app.owl.data.repositories.OWLSubmissionRepository;
import at.jku.dke.task_app.owl.data.repositories.OWLTaskRepository;
import at.jku.dke.task_app.owl.dto.OWLSubmissionDto;
import at.jku.dke.task_app.owl.evaluation.EvaluationService;
import org.springframework.stereotype.Service;

/**
 * This class provides methods for managing {@link OWLSubmission}s.
 */
@Service
public class OWLSubmissionService extends BaseSubmissionService<OWLTask, OWLSubmission, OWLSubmissionDto> {

    private final EvaluationService evaluationService;

    /**
     * Creates a new instance of class {@link OWLSubmissionService}.
     *
     * @param submissionRepository The input repository.
     * @param taskRepository       The task repository.
     * @param evaluationService    The evaluation service.
     */
    public OWLSubmissionService(OWLSubmissionRepository submissionRepository, OWLTaskRepository taskRepository, EvaluationService evaluationService) {
        super(submissionRepository, taskRepository);
        this.evaluationService = evaluationService;
    }

    @Override
    protected OWLSubmission createSubmissionEntity(SubmitSubmissionDto<OWLSubmissionDto> submitSubmissionDto) {
        return new OWLSubmission(submitSubmissionDto.submission().input());
    }

    @Override
    protected GradingDto evaluate(SubmitSubmissionDto<OWLSubmissionDto> submitSubmissionDto) {
        return this.evaluationService.evaluate(submitSubmissionDto);
    }

    @Override
    protected OWLSubmissionDto mapSubmissionToSubmissionData(OWLSubmission submission) {
        return new OWLSubmissionDto(submission.getSubmission());
    }

}
