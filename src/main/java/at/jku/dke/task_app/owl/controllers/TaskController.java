package at.jku.dke.task_app.owl.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseTaskController;
import at.jku.dke.task_app.owl.data.entities.OWLTask;
import at.jku.dke.task_app.owl.dto.OWLTaskDto;
import at.jku.dke.task_app.owl.dto.ModifyOWLTaskDto;
import at.jku.dke.task_app.owl.services.OWLTaskService;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing {@link OWLTask}s.
 */
@RestController
public class TaskController extends BaseTaskController<OWLTask, OWLTaskDto, ModifyOWLTaskDto> {

    /**
     * Creates a new instance of class {@link TaskController}.
     *
     * @param taskService The task service.
     */
    public TaskController(OWLTaskService taskService) {
        super(taskService);
    }

    @Override
    protected OWLTaskDto mapToDto(OWLTask task) {
        return new OWLTaskDto(task.getSolution());
    }

}
