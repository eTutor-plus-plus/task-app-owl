package at.jku.dke.task_app.owl.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskService;
import at.jku.dke.task_app.owl.data.entities.OWLTask;
import at.jku.dke.task_app.owl.data.repositories.OWLTaskRepository;
import at.jku.dke.task_app.owl.dto.ModifyOWLTaskDto;
import at.jku.dke.task_app.owl.evaluation.EvaluationService;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static at.jku.dke.task_app.owl.evaluation.EvaluationService.checkForRedundancy;
import static at.jku.dke.task_app.owl.evaluation.EvaluationService.parsePointsPerClass;

/**
 * This class provides methods for managing {@link OWLTask}s.
 */
@Service
public class OWLTaskService extends BaseTaskService<OWLTask, ModifyOWLTaskDto> {

    private final MessageSource messageSource;

    /**
     * Creates a new instance of class {@link OWLTaskService}.
     *
     * @param repository          The task repository.
     * @param messageSource       The message source.
     */
    public OWLTaskService(OWLTaskRepository repository, MessageSource messageSource) {
        super(repository);
        this.messageSource = messageSource;
    }

    private static boolean[] checkSolutionOntology(ModifyTaskDto<ModifyOWLTaskDto> modifyTaskDto) throws OWLOntologyCreationException {
        /*
        results array contents:
        [0] = true if ontology is consistent
        [1] = true if pointsPerClass could be parsed
        [2] = true if pointsPerClass has correct identifiers
        [3] = true if ontology has no redundant axioms
        */
        boolean[] results = new boolean[4];
        results[0] = true;
        results[1] = true;
        results[2] = true;
        results[3] = true;

        // Check syntax
        OWLOntology solutionOntology = EvaluationService.parseManchesterSyntax(modifyTaskDto.additionalData().solution());

        OWLReasonerFactory factory = new Reasoner.ReasonerFactory();
        OWLReasoner solReasoner = factory.createReasoner(solutionOntology);

        // Check consistency
        results[0] = solReasoner.isConsistent();

        // Check correct points allocation
        try {
            Map<String, Integer> pointsPerClass = parsePointsPerClass(modifyTaskDto.additionalData().pointsPerClass());

            // Compare pointsPerClass identifiers to those in solutionOntology
            Set<String> legalIdentifiers = solutionOntology.getClassesInSignature().stream().map(OWLClass::getIRI).map(IRI::getShortForm).collect(Collectors.toSet());
            legalIdentifiers.addAll(solutionOntology.getIndividualsInSignature().stream().map(OWLIndividual::asOWLNamedIndividual).map(OWLNamedIndividual::getIRI).map(IRI::getShortForm).collect(Collectors.toSet()));

            for (String s : pointsPerClass.keySet()) {
                if (!legalIdentifiers.contains(s)) {
                    results[2] = false;
                    break;
                }
            }

        } catch (Exception e) {
            results[1] = false;
        }

        // Check for redundant axioms
        results[3] = checkForRedundancy(solutionOntology.getAxioms(), factory).isEmpty();

        solReasoner.dispose();

        return results;
    }

    @Override
    protected OWLTask createTask(long id, ModifyTaskDto<ModifyOWLTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("owl"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");

        // Validate ontology before creation
        try {
            boolean[] checkSolutionOntologyResults = checkSolutionOntology(modifyTaskDto);

            if (!checkSolutionOntologyResults[0]) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The solution ontology is not consistent.");
            }
            if (!checkSolutionOntologyResults[1]) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The points deducted per wrong class or individual field could not be parsed.");
            }
            if (!checkSolutionOntologyResults[2]) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The points deducted per wrong class or individual field contains a mismatched identifier.");
            }
            if (!checkSolutionOntologyResults[3]) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The solution ontology has redundant axioms.");
            }

        } catch (OWLRuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Manchester Syntax. " + e.getMessage());
        } catch (OWLOntologyCreationException e) {
            String x = e.getMessage();
            //find the keyword line and save the next number after it, which indicates the line where the error is
            String line = x.substring(x.indexOf("line ") + 5);
            line = line.substring(0, line.indexOf("column")-1);
            int lineNumber = Integer.parseInt(line);
            lineNumber = lineNumber - 7; // Subtract 7 because of the added header lines
            String message = "Invalid Manchester Syntax. Error at line " + lineNumber + ".";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return new OWLTask(modifyTaskDto.additionalData().solution(), modifyTaskDto.additionalData().pointsPerClass(), modifyTaskDto.additionalData().pointsPerRedundantAxiom(), modifyTaskDto.additionalData().pointsPerUndefinedClass(), modifyTaskDto.additionalData().pointsPerAxiomWithoutEntity());
    }

    @Override
    protected void updateTask(OWLTask task, ModifyTaskDto<ModifyOWLTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("owl"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");

        // Validate ontology before creation
        try {
            boolean[] checkSolutionOntologyResults = checkSolutionOntology(modifyTaskDto);

            if (!checkSolutionOntologyResults[0]) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The solution ontology is not consistent.");
            }
            if (!checkSolutionOntologyResults[1]) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The points deducted per wrong class or individual field could not be parsed.");
            }
            if (!checkSolutionOntologyResults[2]) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The points deducted per wrong class or individual field contains a mismatched identifier.");
            }
            if (!checkSolutionOntologyResults[3]) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The solution ontology has redundant axioms.");
            }

        } catch (OWLRuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Manchester Syntax. " + e.getMessage());
        } catch (OWLOntologyCreationException e) {
            String x = e.getMessage();
            //find the keyword line and save the next number after it, which indicates the line where the error is
            String line = x.substring(x.indexOf("line ") + 5);
            line = line.substring(0, line.indexOf("column")-1);
            int lineNumber = Integer.parseInt(line);
            lineNumber = lineNumber - 7; // Subtract 7 because of the added header lines
            String message = "Invalid Manchester Syntax. Error at line " + lineNumber + ".";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        task.setSolution(modifyTaskDto.additionalData().solution());
        task.setPointsPerClass(modifyTaskDto.additionalData().pointsPerClass());
        task.setPointsPerRedundantAxiom(modifyTaskDto.additionalData().pointsPerRedundantAxiom());
        task.setPointsPerUndefinedClass(modifyTaskDto.additionalData().pointsPerUndefinedClass());
        task.setPointsPerAxiomWithoutEntity(modifyTaskDto.additionalData().pointsPerAxiomWithoutEntity());
    }

    @Override
    protected TaskModificationResponseDto mapToReturnData(OWLTask task, boolean create) {
        return new TaskModificationResponseDto(
            this.messageSource.getMessage("defaultTaskDescription", null, Locale.GERMAN),
            this.messageSource.getMessage("defaultTaskDescription", null, Locale.ENGLISH)
        );
    }
}
