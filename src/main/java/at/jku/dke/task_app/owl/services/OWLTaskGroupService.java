package at.jku.dke.task_app.owl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskGroupDto;
import at.jku.dke.etutor.task_app.dto.TaskGroupModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskGroupService;
import at.jku.dke.task_app.owl.data.entities.OWLTaskGroup;
import at.jku.dke.task_app.owl.data.repositories.OWLTaskGroupRepository;
import at.jku.dke.task_app.owl.dto.ModifyOWLTaskGroupDto;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * This class provides methods for managing {@link OWLTaskGroup}s.
 */
@Service
public class OWLTaskGroupService extends BaseTaskGroupService<OWLTaskGroup, ModifyOWLTaskGroupDto> {

    private final MessageSource messageSource;

    /**
     * Creates a new instance of class {@link OWLTaskGroupService}.
     *
     * @param repository    The task group repository.
     * @param messageSource The message source.
     */
    public OWLTaskGroupService(OWLTaskGroupRepository repository, MessageSource messageSource) {
        super(repository);
        this.messageSource = messageSource;
    }

    @Override
    protected OWLTaskGroup createTaskGroup(long id, ModifyTaskGroupDto<ModifyOWLTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("owl"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");
        return new OWLTaskGroup(modifyTaskGroupDto.additionalData().solution());
    }

    @Override
    protected void updateTaskGroup(OWLTaskGroup taskGroup, ModifyTaskGroupDto<ModifyOWLTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("owl"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");
        taskGroup.setSolution(modifyTaskGroupDto.additionalData().solution());
    }

    @Override
    protected TaskGroupModificationResponseDto mapToReturnData(OWLTaskGroup taskGroup, boolean create) {
        return new TaskGroupModificationResponseDto(
            this.messageSource.getMessage("defaultTaskGroupDescription", new Object[]{taskGroup.getSolution()}, Locale.GERMAN),
            this.messageSource.getMessage("defaultTaskGroupDescription", new Object[]{taskGroup.getSolution()}, Locale.ENGLISH));
    }
}
