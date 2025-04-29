package at.jku.dke.task_app.owl.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * This class represents a data transfer object for modifying an owl task.
 *
 * @param solution The solution.
 */
public record ModifyOWLTaskDto(@NotNull Integer solution) implements Serializable {
}
