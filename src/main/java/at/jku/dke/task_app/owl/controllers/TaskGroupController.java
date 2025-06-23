package at.jku.dke.task_app.owl.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseTaskGroupController;
import at.jku.dke.task_app.owl.data.entities.OWLTaskGroup;
import at.jku.dke.task_app.owl.dto.ModifyOWLTaskGroupDto;
import at.jku.dke.task_app.owl.dto.OWLTaskGroupDto;
import at.jku.dke.task_app.owl.services.OWLTaskGroupService;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing {@link OWLTaskGroup}s.
 */
@RestController
public class TaskGroupController extends BaseTaskGroupController<OWLTaskGroup, OWLTaskGroupDto, ModifyOWLTaskGroupDto> {

    /**
     * Creates a new instance of class {@link TaskGroupController}.
     *
     * @param taskGroupService The task group service.
     */
    public TaskGroupController(OWLTaskGroupService taskGroupService) {
        super(taskGroupService);
    }

    @Override
    protected OWLTaskGroupDto mapToDto(OWLTaskGroup taskGroup) {
        return new OWLTaskGroupDto();
    }
}
