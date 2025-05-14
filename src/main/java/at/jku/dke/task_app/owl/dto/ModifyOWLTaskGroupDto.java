package at.jku.dke.task_app.owl.dto;

import at.jku.dke.task_app.owl.validation.ValidTaskGroupNumber;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * This class represents a data transfer object for modifying an owl task group.
 *
 * @param solution The solution in Manchester syntax.
 */
@ValidTaskGroupNumber
public record ModifyOWLTaskGroupDto(@NotNull String solution) implements Serializable {
}
