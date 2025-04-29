package at.jku.dke.task_app.owl.dto;

import at.jku.dke.task_app.owl.data.entities.OWLTask;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link OWLTask}
 *
 * @param solution The solution.
 */
public record OWLTaskDto(@NotNull Integer solution) implements Serializable {
}
