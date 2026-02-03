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
    @Size(max = 30000)
    @Column(name = "solution", nullable = false, length = 30000)
    private String solution;

    @NotNull
    @Size(max = 10000)
    @Column(name = "pointsPerClass", nullable = false, length = 10000)
    private String pointsPerClass;

    @NotNull
    @Column(name = "pointsPerRedundantAxiom", nullable = false)
    private int pointsPerRedundantAxiom;

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
    public OWLTask(String solution, String pointsPerClass, int pointsPerRedundantAxiom) {
        this.solution = solution;
        this.pointsPerClass = pointsPerClass;
        this.pointsPerRedundantAxiom = pointsPerRedundantAxiom;
    }

    /**
     * Creates a new instance of class {@link OWLTask}.
     *
     * @param maxPoints The maximum points.
     * @param status    The status.
     * @param taskGroup The task group.
     * @param solution  The solution ontology in Manchester syntax.
     */
    public OWLTask(BigDecimal maxPoints, TaskStatus status, OWLTaskGroup taskGroup, String solution, String pointsPerClass, int pointsPerRedundantAxiom) {
        super(maxPoints, status, taskGroup);
        this.solution = solution;
        this.pointsPerClass = pointsPerClass;
        this.pointsPerRedundantAxiom = pointsPerRedundantAxiom;
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
    public OWLTask(Long id, BigDecimal maxPoints, TaskStatus status, OWLTaskGroup taskGroup, String solution, String pointsPerClass, int pointsPerRedundantAxiom) {
        super(id, maxPoints, status, taskGroup);
        this.solution = solution;
        this.pointsPerClass = pointsPerClass;
        this.pointsPerRedundantAxiom = pointsPerRedundantAxiom;
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
     * Gets the points deducted per wrong class and individual.
     *
     * @return The points deducted per wrong class and individual.
     */
    public String getPointsPerClass() {
        return pointsPerClass;
    }

    /**
     * Gets the points deducted per redundant axiom.
     *
     * @return The points deducted per redundant axiom.
     */
    public int getPointsPerRedundantAxiom() {
        return pointsPerRedundantAxiom;
    }

    /**
     * Sets the solution ontology in Manchester syntax.
     *
     * @param solution The solution ontology.
     */
    public void setSolution(String solution) {
        this.solution = solution;
    }

    /**
     * Sets the pointsPerClass.
     *
     * @param pointsPerClass The points per class.
     */
    public void setPointsPerClass(String pointsPerClass) {
        this.pointsPerClass = pointsPerClass;
    }

    /**
     * Sets the pointsPerRedundantAxiom.
     *
     * @param pointsPerRedundantAxiom The points per redundant axiom.
     */
    public void setPointsPerRedundantAxiom(int pointsPerRedundantAxiom) {
        this.pointsPerRedundantAxiom = pointsPerRedundantAxiom;
    }
}
