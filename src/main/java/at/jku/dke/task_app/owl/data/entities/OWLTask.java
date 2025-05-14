package at.jku.dke.task_app.owl.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTaskInGroup;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Represents an OWL task that contains a solution ontology in Manchester syntax.
 */
@Entity
@Table(name = "task")
public class OWLTask extends BaseTaskInGroup<OWLTaskGroup> {

    @NotNull
    @Size(max = 10000)
    @Column(name = "solution", nullable = false, length = 10000)
    private String solution;

    /**
     * Creates a new instance of class {@link OWLTask}.
     */
    public OWLTask() {
    }

    /**
     * Creates a new instance of class {@link OWLTask}.
     *
     * @param solution The solution ontology in Manchester syntax.
     */
    public OWLTask(String solution) {
        this.solution = solution;
    }

    /**
     * Creates a new instance of class {@link OWLTask}.
     *
     * @param maxPoints The maximum points.
     * @param status    The status.
     * @param taskGroup The task group.
     * @param solution  The solution ontology in Manchester syntax.
     */
    public OWLTask(BigDecimal maxPoints, TaskStatus status, OWLTaskGroup taskGroup, String solution) {
        super(maxPoints, status, taskGroup);
        this.solution = solution;
    }

    /**
     * Creates a new instance of class {@link OWLTask}.
     *
     * @param id        The identifier.
     * @param maxPoints The maximum points.
     * @param status    The status.
     * @param taskGroup The task group.
     * @param solution  The solution ontology in Manchester syntax.
     */
    public OWLTask(Long id, BigDecimal maxPoints, TaskStatus status, OWLTaskGroup taskGroup, String solution) {
        super(id, maxPoints, status, taskGroup);
        this.solution = solution;
    }

    /**
     * Gets the solution ontology in Manchester syntax.
     *
     * @return The solution ontology.
     */
    public String getSolution() {
        return solution;
    }

    /**
     * Sets the solution ontology in Manchester syntax.
     *
     * @param solution The solution ontology.
     */
    public void setSolution(String solution) {
        this.solution = solution;
    }
}
