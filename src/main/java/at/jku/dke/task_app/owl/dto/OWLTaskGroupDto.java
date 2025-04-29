package at.jku.dke.task_app.owl.dto;

import at.jku.dke.task_app.owl.data.entities.OWLTaskGroup;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link OWLTaskGroup}
 *
 * @param minNumber The minimum number.
 * @param maxNumber The maximum number.
 */
public record OWLTaskGroupDto(@NotNull Integer minNumber, @NotNull Integer maxNumber) implements Serializable {
}
