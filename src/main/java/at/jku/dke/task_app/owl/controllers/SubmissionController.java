package at.jku.dke.task_app.owl.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseSubmissionController;
import at.jku.dke.task_app.owl.data.entities.OWLSubmission;
import at.jku.dke.task_app.owl.dto.OWLSubmissionDto;
import at.jku.dke.task_app.owl.services.OWLSubmissionService;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing {@link OWLSubmission}s.
 */
@RestController
public class SubmissionController extends BaseSubmissionController<OWLSubmissionDto> {
    /**
     * Creates a new instance of class {@link SubmissionController}.
     *
     * @param submissionService The input service.
     */
    public SubmissionController(OWLSubmissionService submissionService) {
        super(submissionService);
    }
}
