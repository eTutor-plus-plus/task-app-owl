package at.jku.dke.task_app.owl.dto;

import at.jku.dke.task_app.owl.validation.ValidTaskGroupNumber;

import java.io.Serializable;

/**
 * This class represents a data transfer object for modifying an owl task group.
 */
@ValidTaskGroupNumber
public record ModifyOWLTaskGroupDto() implements Serializable {
}
