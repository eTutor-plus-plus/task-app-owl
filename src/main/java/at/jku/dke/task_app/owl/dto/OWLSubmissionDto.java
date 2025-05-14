package at.jku.dke.task_app.owl.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OWLSubmissionDto(
    @NotNull
    @Size(max = 10000)
    String input
) {}
