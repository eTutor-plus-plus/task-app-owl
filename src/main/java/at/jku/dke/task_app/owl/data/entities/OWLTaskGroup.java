package at.jku.dke.task_app.owl.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTaskGroup;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Represents an owl task group.
 * <p>
 * It is also possible to create tasks without task types. Tasks of type owl would not need a task group.
 * Here a task group is only used for demonstration.
 */
@Entity
@Table(name = "task_group")
public class   OWLTaskGroup extends BaseTaskGroup {

    /**
     * Creates a new instance of class {@link OWLTaskGroup}.
     */
    public OWLTaskGroup() {
    }

    /**
     * Creates a new instance of class {@link OWLTaskGroup}.
     *
     * @param status    The status.
     */
    public OWLTaskGroup(TaskStatus status) {
        super(status);
    }

    /**
     * Creates a new instance of class {@link OWLTaskGroup}.
     *
     * @param id        The id.
     * @param status    The status.
     */
    public OWLTaskGroup(Long id, TaskStatus status) {
        super(id, status);
    }
}
