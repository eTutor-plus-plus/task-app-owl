package at.jku.dke.task_app.owl.dto;

import at.jku.dke.task_app.owl.data.entities.OWLTaskGroup;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link OWLTaskGroup}
 *
 * @param solution The solution in Manchester syntax.
 */
public record OWLTaskGroupDto(@NotNull String solution) implements Serializable {
}
