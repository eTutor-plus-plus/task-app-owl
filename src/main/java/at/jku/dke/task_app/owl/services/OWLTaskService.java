package at.jku.dke.task_app.owl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskService;
import at.jku.dke.task_app.owl.data.entities.OWLTask;
import at.jku.dke.task_app.owl.data.repositories.OWLTaskGroupRepository;
import at.jku.dke.task_app.owl.data.repositories.OWLTaskRepository;
import at.jku.dke.task_app.owl.dto.ModifyOWLTaskDto;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * This class provides methods for managing {@link OWLTask}s.
 */
@Service
public class OWLTaskService extends BaseTaskService<OWLTask, ModifyOWLTaskDto> {

    private final MessageSource messageSource;

    /**
     * Creates a new instance of class {@link OWLTaskService}.
     *
     * @param repository          The task repository.
     * @param taskGroupRepository The task group repository.
     * @param messageSource       The message source.
     */
    public OWLTaskService(OWLTaskRepository repository, OWLTaskGroupRepository taskGroupRepository, MessageSource messageSource) {
        super(repository);
        this.messageSource = messageSource;
    }

    @Override
    protected OWLTask createTask(long id, ModifyTaskDto<ModifyOWLTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("owl"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        return new OWLTask(modifyTaskDto.additionalData().solution(), modifyTaskDto.additionalData().pointsPerClass(), modifyTaskDto.additionalData().pointsPerRedundantAxiom(), modifyTaskDto.additionalData().pointsPerUndefinedClass());
    }

    @Override
    protected void updateTask(OWLTask task, ModifyTaskDto<ModifyOWLTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("owl"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        task.setSolution(modifyTaskDto.additionalData().solution());
        task.setPointsPerClass(modifyTaskDto.additionalData().pointsPerClass());
        task.setPointsPerRedundantAxiom(modifyTaskDto.additionalData().pointsPerRedundantAxiom());
        task.setPointsPerUndefinedClass(modifyTaskDto.additionalData().pointsPerUndefinedClass());
    }

    @Override
    protected TaskModificationResponseDto mapToReturnData(OWLTask task, boolean create) {
        return new TaskModificationResponseDto(
            this.messageSource.getMessage("defaultTaskDescription", null, Locale.GERMAN),
            this.messageSource.getMessage("defaultTaskDescription", null, Locale.ENGLISH)
        );
    }
}
