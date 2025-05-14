package at.jku.dke.task_app.owl.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTaskGroup;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents an owl task group.
 * <p>
 * It is also possible to create tasks without task types. Tasks of type owl would not need a task group.
 * Here a task group is only used for demonstration.
 */
@Entity
@Table(name = "task_group")
public class   OWLTaskGroup extends BaseTaskGroup {
    @NotNull
    @Size(max = 10000)
    @Column(name = "solution", nullable = false, length = 10000)
    private String solution;

    /**
     * Creates a new instance of class {@link OWLTaskGroup}.
     */
    public OWLTaskGroup() {
    }

    /**
     * Creates a new instance of class {@link OWLTaskGroup}.
     *
     * @param solution The solution ontology in Manchester syntax.
     */
    public OWLTaskGroup(String solution) {
        this.solution = solution;
    }

    /**
     * Creates a new instance of class {@link OWLTaskGroup}.
     *
     * @param status    The status.
     * @param solution The solution ontology in Manchester syntax.
     */
    public OWLTaskGroup(TaskStatus status, String solution) {
        super(status);
        this.solution = solution;
    }

    /**
     * Creates a new instance of class {@link OWLTaskGroup}.
     *
     * @param id        The id.
     * @param status    The status.
     * @param solution The solution ontology in Manchester syntax.
     */
    public OWLTaskGroup(Long id, TaskStatus status, String solution) {
        super(id, status);
        this.solution = solution;
    }

    /**
     * Gets the solution.
     *
     * @return The solution.
     */
    public String getSolution() {
        return solution;
    }

    /**
     * Sets the solution.
     *
     * @param solution The solution.
     */
    public void setSolution(String solution) {
        this.solution = solution;
    }
}
