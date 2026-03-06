package at.jku.dke.task_app.owl.evaluation;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmissionMode;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.owl.data.repositories.OWLTaskRepository;
import at.jku.dke.task_app.owl.dto.OWLSubmissionDto;
import jakarta.persistence.EntityNotFoundException;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.OWLReasonerRuntimeException;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service that evaluates OWL ontology submissions in Manchester syntax.
 */
@Service
public class EvaluationService {
    private static final Logger LOG = LoggerFactory.getLogger(EvaluationService.class);

    private final OWLTaskRepository taskRepository;
    private final MessageSource messageSource;

    /**
     * Creates a new instance of class {@link EvaluationService}.
     *
     * @param taskRepository The task repository.
     * @param messageSource The message source.
     */
    public EvaluationService(OWLTaskRepository taskRepository, MessageSource messageSource) {
        this.taskRepository = taskRepository;
        this.messageSource = messageSource;
    }

    /**
     * Evaluates an OWL ontology submission.
     *
     * @param submission The input to evaluate.
     * @return The evaluation result.
     */
    @Transactional
    public GradingDto evaluate(SubmitSubmissionDto<OWLSubmissionDto> submission) {
        long startTime = System.nanoTime();

        // find task
        var task = this.taskRepository.findById(submission.taskId()).orElseThrow(() -> new EntityNotFoundException("Task " + submission.taskId() + " does not exist."));

        // evaluate input
        LOG.info("Evaluating input for task {} with mode {} and feedback-level {}", submission.taskId(), submission.mode(), submission.feedbackLevel());
        Locale locale = Locale.of(submission.language());
        BigDecimal points = BigDecimal.ZERO;
        List<CriterionDto> criteria = new ArrayList<>();
        String feedback = "";

        try {
            LOG.info("Evaluating submission input: \n{}", submission.submission().input());
            LOG.info("Against solution: \n{}", task.getSolution());

            // Create ontologies by parsing submission and solution
            OWLOntology submittedOntology = parseManchesterSyntax(submission.submission().input());
            OWLOntology solutionOntology = parseManchesterSyntax(task.getSolution());

            // Add syntax validation criterion
            criteria.add(new CriterionDto(
                this.messageSource.getMessage("criterium.syntax", null, locale),
                null,
                true,
                this.messageSource.getMessage("criterium.syntax.valid", null, locale)
            ));

            LOG.info("Evaluating ontologies:\n");
            StringDocumentTarget submitted = new StringDocumentTarget();
            submittedOntology.getOWLOntologyManager().saveOntology(submittedOntology, new ManchesterSyntaxDocumentFormat(), submitted);
            LOG.info("Submitted: \n{}", submitted);
            StringDocumentTarget solution = new StringDocumentTarget();
            submittedOntology.getOWLOntologyManager().saveOntology(solutionOntology, new ManchesterSyntaxDocumentFormat(), solution);
            LOG.info("Solution: \n{}", solution);

            // Compare ontologies
            OntologyComparisonResult ontologyComparisonResult = compareOntologies(submittedOntology, solutionOntology);

            // fill the ontologyComparisonResult's pointsPerClass map storing all classes/individuals and their points
            String[] entries = task.getPointsPerClass().split(",");

            for (String entry : entries) {
                String[] keyValue = entry.split("=");

                String key = keyValue[0].trim();
                int value = Integer.parseInt(keyValue[1].trim());

                assert ontologyComparisonResult != null;
                ontologyComparisonResult.pointsPerClass.put(key, value);
            }

            // Prepare feedback message parts
            assert ontologyComparisonResult != null;
            String submissionConsistencyMessage = this.messageSource.getMessage(ontologyComparisonResult.submittedIsConsistent ? "criterium.consistency.consistent" : "criterium.consistency.inconsistent", null, locale);
            //String solutionConsistencyMessage = this.messageSource.getMessage(ontologyComparisonResult.solutionIsConsistent ? "owl.solution.consistent" : "owl.solution.inconsistent", null, locale);

            ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

            StringBuilder missingAxiomsMessage;
            if (ontologyComparisonResult.missingAxioms.isEmpty()) {
                missingAxiomsMessage = new StringBuilder(this.messageSource.getMessage("owl.submission.not-missing", null, locale));
            } else {
                missingAxiomsMessage = new StringBuilder(this.messageSource.getMessage("owl.submission.missing", null, locale));
                for (OWLAxiom ax : ontologyComparisonResult.missingAxioms) {
                    missingAxiomsMessage.append("\n").append(" [").append(renderer.render(ax)).append("] ");
                }
            }

            StringBuilder wrongAxiomsMessage;
            if (ontologyComparisonResult.wrongAxioms.isEmpty()) {
                wrongAxiomsMessage = new StringBuilder(this.messageSource.getMessage("owl.submission.not-wrong", null, locale));
            } else {
                wrongAxiomsMessage = new StringBuilder(this.messageSource.getMessage("owl.submission.wrong", null, locale));
                for (OWLAxiom ax : ontologyComparisonResult.wrongAxioms) {
                    wrongAxiomsMessage.append("\n").append(" [").append(renderer.render(ax)).append("] ");
                }
            }

            points = calculatePoints(ontologyComparisonResult, task.getMaxPoints(), task.getPointsPerRedundantAxiom(), task.getPointsPerUndefinedClass());
            int correctness;
            if (points.compareTo(task.getMaxPoints()) >= 0) {
                correctness = 2;
            } else if (points.compareTo(BigDecimal.ZERO) > 0) {
                correctness = 1;
            } else {
                correctness = 0;
            }

            // Handle different evaluation modes
            if (submission.mode() == SubmissionMode.RUN) {
                points = BigDecimal.ZERO;
                feedback = "";
                String identifierMessage = "";

                if (!ontologyComparisonResult.setOfWrongIdentifiers.isEmpty()) {
                    identifierMessage += "\n"
                        + " ["
                        + String.join(", ", ontologyComparisonResult.setOfWrongIdentifiers)
                        + "].";
                }

                // Add legal identifier criterion
                criteria.add(new CriterionDto(
                    this.messageSource.getMessage("criterium.identifier", null, locale),
                    null,
                    ontologyComparisonResult.correctIdentifiers,
                    ontologyComparisonResult.correctIdentifiers ? this.messageSource.getMessage("criterium.identifier.valid", null, locale) : this.messageSource.getMessage("criterium.identifier.invalid", null, locale)
                        + identifierMessage
                ));
            } else if (submission.mode() == SubmissionMode.DIAGNOSE || submission.mode() == SubmissionMode.SUBMIT){

                switch (correctness) {
                    case 0: {
                        feedback = this.messageSource.getMessage("owl.submission.incorrect", null, locale);
                        break;
                    }
                    case 1: {
                        feedback = this.messageSource.getMessage("owl.submission.partially-correct", null, locale);
                        break;
                    }
                    case 2: {
                        feedback = this.messageSource.getMessage("owl.submission.correct", null, locale);
                        break;
                    }
                }

                LOG.info("Feedback level is {}", submission.feedbackLevel());

                String identifierMessage = "";

                if (!ontologyComparisonResult.setOfWrongIdentifiers.isEmpty()) {
                    identifierMessage += "\n"
                        + " ["
                        + String.join(", ", ontologyComparisonResult.setOfWrongIdentifiers)
                        + "].";
                }

                // Add legal identifier criterion
                criteria.add(new CriterionDto(
                    this.messageSource.getMessage("criterium.identifier", null, locale),
                    null,
                    ontologyComparisonResult.correctIdentifiers,
                    ontologyComparisonResult.correctIdentifiers ? this.messageSource.getMessage("criterium.identifier.valid", null, locale) : this.messageSource.getMessage("criterium.identifier.invalid", null, locale)
                        + identifierMessage
                ));

                if (submission.feedbackLevel() > 0) {
                    // Little feedback
                    // Add consistency criterion
                    criteria.add(new CriterionDto(
                        this.messageSource.getMessage("criterium.consistency", null, locale),
                        null,
                        ontologyComparisonResult.submittedIsConsistent,
                        submissionConsistencyMessage
                    ));
                    // Is submitted ontology is inconsistent, abort early
                    if (!ontologyComparisonResult.submittedIsConsistent) return new GradingDto(task.getMaxPoints(), points, feedback, criteria);
                }
                if (submission.feedbackLevel() > 1) {
                    // Some feedback
                    // Determine if there are any wrong classes or individuals
                    boolean correctClasses = ontologyComparisonResult.incompleteClasses.isEmpty()
                        && ontologyComparisonResult.wrongClasses.isEmpty()
                        && ontologyComparisonResult.missingClasses.isEmpty();
                    boolean correctIndividuals = ontologyComparisonResult.incompleteIndividuals.isEmpty()
                        && ontologyComparisonResult.wrongIndividuals.isEmpty()
                        && ontologyComparisonResult.missingIndividuals.isEmpty();

                    // Prepare sets for showing points in criterion
                    Set<OWLClass> wrongClasses = new HashSet<>(ontologyComparisonResult.incompleteClasses);
                    wrongClasses.addAll(ontologyComparisonResult.wrongClasses);
                    wrongClasses.addAll(ontologyComparisonResult.missingClasses);

                    Set<OWLIndividual> wrongIndividuals = new HashSet<>(ontologyComparisonResult.incompleteIndividuals);
                    wrongIndividuals.addAll(ontologyComparisonResult.wrongIndividuals);
                    wrongIndividuals.addAll(ontologyComparisonResult.missingIndividuals);

                    BigDecimal pointsDeductedForClasses = BigDecimal.ZERO;
                    BigDecimal pointsDeductedForIndividuals = BigDecimal.ZERO;

                    for (OWLClass cls : wrongClasses) {
                        // if no points were defined for a class, no points are subtracted
                        pointsDeductedForClasses = pointsDeductedForClasses.add(BigDecimal.valueOf(ontologyComparisonResult.pointsPerClass.getOrDefault(cls.getIRI().getShortForm(), task.getPointsPerUndefinedClass())));
                    }

                    for (OWLIndividual individual : wrongIndividuals) {
                        // if no points were defined for an individual, no points are subtracted
                        pointsDeductedForIndividuals = pointsDeductedForIndividuals.add(BigDecimal.valueOf(ontologyComparisonResult.pointsPerClass.getOrDefault(individual.asOWLNamedIndividual().getIRI().getShortForm(), task.getPointsPerUndefinedClass())));
                    }

                    // Give out the incomplete, wrong and missing classes
                    StringBuilder classesMessage = new StringBuilder();
                    ontologyComparisonResult.incompleteClasses.removeAll(ontologyComparisonResult.wrongClasses); // Don't show classes that are already considered wrong'
                    if (!ontologyComparisonResult.incompleteClasses.isEmpty()) {
                        classesMessage.append("\n").append(this.messageSource.getMessage("owl.submission.incomplete-classes", null, locale)).append(" [");
                        for (OWLClass cls : ontologyComparisonResult.incompleteClasses) {
                            classesMessage.append(cls.getIRI().getShortForm()).append(": ");
                            classesMessage.append(-1 * ontologyComparisonResult.pointsPerClass.getOrDefault(cls.getIRI().getShortForm(), task.getPointsPerUndefinedClass()));
                            classesMessage.append(", ");
                        }
                        classesMessage.append("].");
                    }
                    if (!ontologyComparisonResult.wrongClasses.isEmpty()) {
                        classesMessage.append("\n").append(this.messageSource.getMessage("owl.submission.wrong-classes", null, locale)).append(" [");
                        for (OWLClass cls : ontologyComparisonResult.wrongClasses) {
                            classesMessage.append(cls.getIRI().getShortForm()).append(": ");
                            classesMessage.append(-1 * ontologyComparisonResult.pointsPerClass.getOrDefault(cls.getIRI().getShortForm(), task.getPointsPerUndefinedClass()));
                            classesMessage.append(", ");
                        }
                        classesMessage.append("].");
                    }
                    if (!ontologyComparisonResult.missingClasses.isEmpty()) {
                        classesMessage.append("\n").append(this.messageSource.getMessage("owl.submission.missing-classes", null, locale)).append(" [");
                        for (OWLClass cls : ontologyComparisonResult.missingClasses) {
                            classesMessage.append(cls.getIRI().getShortForm()).append(": ");
                            classesMessage.append(-1 * ontologyComparisonResult.pointsPerClass.getOrDefault(cls.getIRI().getShortForm(), task.getPointsPerUndefinedClass()));
                            classesMessage.append(", ");
                        }
                        classesMessage.append("].");
                    }

                    // Add wrong classes criterion
                    criteria.add(new CriterionDto(
                        this.messageSource.getMessage("criterium.classes", null, locale),
                        pointsDeductedForClasses.compareTo(BigDecimal.ZERO) > 0 ? pointsDeductedForClasses.multiply(BigDecimal.valueOf(-1)) : null,
                        correctClasses,
                        correctClasses ? this.messageSource.getMessage("criterium.classes.correct", null, locale) : this.messageSource.getMessage("criterium.classes.wrong", null, locale)
                            + classesMessage
                    ));

                    // Give out the incomplete, wrong and missing individuals
                    // Only show individual-criterion if there are any individuals in the solution ontology
                    if (!solutionOntology.getIndividualsInSignature().isEmpty()) {
                        StringBuilder individualsMessage = new StringBuilder();
                        ontologyComparisonResult.incompleteIndividuals.removeAll(ontologyComparisonResult.wrongIndividuals); // Don't show individuals that are already considered wrong
                        if (!ontologyComparisonResult.incompleteIndividuals.isEmpty()) {
                            individualsMessage.append("\n").append(this.messageSource.getMessage("owl.submission.incomplete-individuals", null, locale)).append(" [");
                            for (OWLIndividual ind : ontologyComparisonResult.incompleteIndividuals) {
                                individualsMessage.append(ind.asOWLNamedIndividual().getIRI().getShortForm()).append(": ");
                                individualsMessage.append(-1 * ontologyComparisonResult.pointsPerClass.getOrDefault(ind.asOWLNamedIndividual().getIRI().getShortForm(), task.getPointsPerUndefinedClass()));
                                individualsMessage.append(", ");
                            }
                            individualsMessage.append("].");
                        }
                        if (!ontologyComparisonResult.wrongIndividuals.isEmpty()) {
                            individualsMessage.append("\n").append(this.messageSource.getMessage("owl.submission.wrong-individuals", null, locale)).append(" [");
                            for (OWLIndividual ind : ontologyComparisonResult.wrongIndividuals) {
                                individualsMessage.append(ind.asOWLNamedIndividual().getIRI().getShortForm()).append(": ");
                                individualsMessage.append(-1 * ontologyComparisonResult.pointsPerClass.getOrDefault(ind.asOWLNamedIndividual().getIRI().getShortForm(), task.getPointsPerUndefinedClass()));
                                individualsMessage.append(", ");
                            }
                            individualsMessage.append("].");
                        }
                        if (!ontologyComparisonResult.missingIndividuals.isEmpty()) {
                            individualsMessage.append("\n").append(this.messageSource.getMessage("owl.submission.missing-individuals", null, locale)).append(" [");
                            for (OWLIndividual ind : ontologyComparisonResult.missingIndividuals) {
                                individualsMessage.append(ind.asOWLNamedIndividual().getIRI().getShortForm()).append(": ");
                                individualsMessage.append(-1 * ontologyComparisonResult.pointsPerClass.getOrDefault(ind.asOWLNamedIndividual().getIRI().getShortForm(), task.getPointsPerUndefinedClass()));
                                individualsMessage.append(", ");
                            }
                            individualsMessage.append("].");
                        }

                        criteria.add(new CriterionDto(
                            this.messageSource.getMessage("criterium.individuals", null, locale),
                            pointsDeductedForIndividuals.compareTo(BigDecimal.ZERO) > 0 ? pointsDeductedForIndividuals.multiply(BigDecimal.valueOf(-1)) : null,
                            correctIndividuals,
                            correctIndividuals ? this.messageSource.getMessage("criterium.individuals.correct", null, locale) : this.messageSource.getMessage("criterium.individuals.wrong", null, locale)
                                + individualsMessage
                        ));
                    }

                    // Give out redundant axioms
                    renderer.setShortFormProvider(new SimpleShortFormProvider());

                    String redundantAxiomsMessage = "";
                    if (!ontologyComparisonResult.redundantAxioms.isEmpty()) {
                        redundantAxiomsMessage += "\n"
                            + this.messageSource.getMessage("owl.submission.redundant-axioms", null, locale)
                            + " ["
                            + ontologyComparisonResult.redundantAxioms.stream().map(renderer::render).collect(Collectors.joining(", "))
                            + "].";
                    }

                    criteria.add(new CriterionDto(
                        this.messageSource.getMessage("criterium.redundantAxioms", null, locale),
                        !ontologyComparisonResult.redundantAxioms.isEmpty() ? BigDecimal.valueOf(-1L * ontologyComparisonResult.redundantAxioms.size() * task.getPointsPerRedundantAxiom()) : null,
                        ontologyComparisonResult.redundantAxioms.isEmpty(),
                        ontologyComparisonResult.redundantAxioms.isEmpty() ? this.messageSource.getMessage("criterium.redundantAxioms.correct", null, locale) : this.messageSource.getMessage("criterium.redundantAxioms.wrong", null, locale)
                            + redundantAxiomsMessage
                    ));
                }
                if (submission.feedbackLevel() > 2) {
                    // Much feedback
                    if (!ontologyComparisonResult.missingAxioms.isEmpty()) {
                        criteria.add(new CriterionDto(
                            this.messageSource.getMessage("criterium.missingAxioms", null, locale),
                            null,
                            false,
                            "" + missingAxiomsMessage
                        ));
                    }
                    if (!ontologyComparisonResult.wrongAxioms.isEmpty()) {
                        criteria.add(new CriterionDto(
                            this.messageSource.getMessage("criterium.wrongAxioms", null, locale),
                            null,
                            false,
                            "" + wrongAxiomsMessage
                        ));
                    }
                }
            } else throw new IllegalStateException("Unexpected evaluation mode: " + submission.mode());

        } catch (OWLRuntimeException | OWLOntologyStorageException e) {
            LOG.error("Failed to parse Manchester syntax", e);
            criteria.add(new CriterionDto(
                this.messageSource.getMessage("criterium.syntax", null, locale),
                null,
                false,
                e.getMessage()
            ));
            feedback = this.messageSource.getMessage("owl.submission.incorrect", null, locale);
        }

        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        LOG.info("Execution took {} ms", duration);

        return new GradingDto(task.getMaxPoints(), points, feedback, criteria);
    }

    private static BigDecimal calculatePoints(OntologyComparisonResult ontologyComparisonResult, BigDecimal maxPoints, int pointsPerAxiom, int pointsPerUndefinedClass) {

        BigDecimal points = maxPoints;

        // No classes or individuals in solution --> no mistakes to make
        if(ontologyComparisonResult.classesInSolution.isEmpty() && ontologyComparisonResult.individualsInSolution.isEmpty()) return points;
        // No points for inconsistent submissions
        if(!ontologyComparisonResult.submittedIsConsistent) return BigDecimal.ZERO;

        // Subtract points for wrong classes and individuals, including ones that are missing
        Set<OWLClass> wrongClasses = new HashSet<>(ontologyComparisonResult.incompleteClasses);
        wrongClasses.addAll(ontologyComparisonResult.wrongClasses);
        wrongClasses.addAll(ontologyComparisonResult.missingClasses);

        Set<OWLIndividual> wrongIndividuals = new HashSet<>(ontologyComparisonResult.incompleteIndividuals);
        wrongIndividuals.addAll(ontologyComparisonResult.wrongIndividuals);
        wrongIndividuals.addAll(ontologyComparisonResult.missingIndividuals);

        int subtract;
        for (OWLClass cls : wrongClasses) {
            // if no points were defined for a class, the default value for undefined classes is subtracted
            subtract = ontologyComparisonResult.pointsPerClass.getOrDefault(cls.getIRI().getShortForm(), pointsPerUndefinedClass);
            points = points.subtract(BigDecimal.valueOf(subtract));
        }

        for (OWLIndividual individual : wrongIndividuals) {
            // if no points were defined for an individual, the default value for undefined classes is subtracted
            subtract = ontologyComparisonResult.pointsPerClass.getOrDefault(individual.asOWLNamedIndividual().getIRI().getShortForm(), pointsPerUndefinedClass);
            points = points.subtract(BigDecimal.valueOf(subtract));
        }

        // Subtract points for redundant axioms
        points = points.subtract(BigDecimal.valueOf((long) ontologyComparisonResult.redundantAxioms.size() * pointsPerAxiom));

        points = points.setScale(0, RoundingMode.FLOOR); // Always round down

        return points.max(BigDecimal.ZERO);
    }

    // Parses Manchester Syntax String Input into OWL Ontology
    private OWLOntology parseManchesterSyntax(String manchesterSyntaxInput) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        try {
            // Add required ontology headers
            String completeOntology = """
                Prefix: owl: <http://www.w3.org/2002/07/owl#>
                Prefix: rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                Prefix: rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                Prefix: xsd: <http://www.w3.org/2001/XMLSchema#>
                Prefix: : <https://example.org/onto#>

                Ontology: <https://example.org/onto>
                """ + manchesterSyntaxInput;

            StringDocumentSource documentSource = new StringDocumentSource(
                completeOntology,
                IRI.create("https://example.org/onto"),
                new ManchesterSyntaxDocumentFormat(),
                "UTF-8"
            );

            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(documentSource);

            LOG.info("Successfully parsed ontology:");
            LOG.info("Classes: {}", ontology.classesInSignature().collect(Collectors.toSet()));
            LOG.info("Logical axioms: {}", ontology.logicalAxioms().collect(Collectors.toSet()));
            LOG.info("All axioms: {}", ontology.axioms().collect(Collectors.toSet()));

            return ontology;
        } catch (OWLOntologyCreationException e) {
            LOG.error("Invalid Manchester syntax", e);
            String x = e.getMessage();
            //find the keyword line and save the next number after it, which indicates the line where the error is
            String line = x.substring(x.indexOf("line ") + 5);
            line = line.substring(0, line.indexOf("column")-1);
            int lineNumber = Integer.parseInt(line);
            lineNumber = lineNumber - 6; // Subtract 6 because of the added header lines
            String message = "Invalid Manchester Syntax. Error at line " + lineNumber + ".";

            throw new OWLRuntimeException(message);
        }
    }

    private OntologyComparisonResult compareOntologies(OWLOntology submittedOntology, OWLOntology solutionOntology) {
        try {
            OntologyComparisonResult result = new OntologyComparisonResult();

            // Check for illegal identifiers
            Set<String> legalIdentifiers = solutionOntology.getClassesInSignature().stream().map(OWLClass::getIRI).map(IRI::getShortForm).collect(Collectors.toSet());
            legalIdentifiers.addAll(solutionOntology.getIndividualsInSignature().stream().map(OWLIndividual::asOWLNamedIndividual).map(OWLNamedIndividual::getIRI).map(IRI::getShortForm).collect(Collectors.toSet()));
            legalIdentifiers.addAll(solutionOntology.getObjectPropertiesInSignature().stream().map(OWLObjectProperty::getIRI).map(IRI::getShortForm).collect(Collectors.toSet()));
            legalIdentifiers.addAll(solutionOntology.getDataPropertiesInSignature().stream().map(OWLDataProperty::getIRI).map(IRI::getShortForm).collect(Collectors.toSet()));

            for (OWLClass cls : submittedOntology.getClassesInSignature()) {
                if (!legalIdentifiers.contains(cls.getIRI().getShortForm())) {
                    result.correctIdentifiers = false;
                    result.setOfWrongIdentifiers.add(cls.getIRI().getShortForm());
                }
            }
            for (OWLIndividual ind : submittedOntology.getIndividualsInSignature()) {
                if (!legalIdentifiers.contains(ind.asOWLNamedIndividual().getIRI().getShortForm())) {
                    result.correctIdentifiers = false;
                    result.setOfWrongIdentifiers.add(ind.asOWLNamedIndividual().getIRI().getShortForm());
                }
            }
            for (OWLObjectProperty prop : submittedOntology.getObjectPropertiesInSignature()) {
                if (!legalIdentifiers.contains(prop.getIRI().getShortForm())) {
                    result.correctIdentifiers = false;
                    result.setOfWrongIdentifiers.add(prop.getIRI().getShortForm());
                }
            }
            for (OWLDataProperty prop : submittedOntology.getDataPropertiesInSignature()) {
                if (!legalIdentifiers.contains(prop.getIRI().getShortForm())) {
                    result.correctIdentifiers = false;
                    result.setOfWrongIdentifiers.add(prop.getIRI().getShortForm());
                }
            }

            OWLReasonerFactory factory = new Reasoner.ReasonerFactory();
            OWLReasoner subReasoner = factory.createReasoner(submittedOntology);
            OWLReasoner solReasoner = factory.createReasoner(solutionOntology);

            // Save classes and individuals in solution for calculating points later
            result.classesInSolution.addAll(solutionOntology.getClassesInSignature());
            LOG.info("Number of classes in solution: {}", result.classesInSolution);
            result.individualsInSolution.addAll(solutionOntology.getIndividualsInSignature());
            LOG.info("Number of individuals in solution: {}", result.individualsInSolution);

            // Check consistency
            result.submittedIsConsistent = subReasoner.isConsistent();
            LOG.error("Submitted ontology is consistent: {}", result.submittedIsConsistent);
            result.solutionIsConsistent = solReasoner.isConsistent();
            LOG.error("Solution ontology is consistent: {}", result.solutionIsConsistent);

            if (!result.submittedIsConsistent || !result.solutionIsConsistent) {
                return result;
            }

            // Save wrong and missing classes
            result.wrongClasses.addAll(submittedOntology.getClassesInSignature());
            result.wrongClasses.removeAll(solutionOntology.getClassesInSignature());
            LOG.info("Redundant classes: {}", result.wrongClasses);

            result.missingClasses.addAll(solutionOntology.getClassesInSignature());
            result.missingClasses.removeAll(submittedOntology.getClassesInSignature());
            LOG.info("Missing classes: {}", result.missingClasses);

            // Save wrong and missing individuals
            result.missingIndividuals.addAll(solutionOntology.getIndividualsInSignature());
            result.missingIndividuals.removeAll(submittedOntology.getIndividualsInSignature());
            LOG.info("Missing individuals: {}", result.missingIndividuals);

            result.wrongIndividuals.addAll(submittedOntology.getIndividualsInSignature());
            result.wrongIndividuals.removeAll(solutionOntology.getIndividualsInSignature());
            LOG.info("Redundant individuals: {}", result.wrongIndividuals);


            Set<OWLAxiom> submittedAxioms = submittedOntology.getAxioms();
            Set<OWLAxiom> solutionAxioms = solutionOntology.getAxioms();

            // Log both ontologies' axioms
            LOG.info("Submitted ontology axioms:");
            submittedAxioms.forEach(ax -> LOG.info(ax.toString()));
            LOG.info("Solution ontology axioms:");
            solutionAxioms.forEach(ax -> LOG.info(ax.toString()));

            // Check for redundancy in submitted ontology
            result.redundantAxioms.addAll(checkForRedundancy(submittedAxioms, factory));

            // Check mutual entailment
            // Store non-entailed axioms for detailed feedback
            for (OWLAxiom ax : submittedAxioms) {
                if (!solReasoner.isEntailed(ax)) {
                    result.wrongAxioms.add(ax);
                    result.incompleteClasses.addAll(ax.getClassesInSignature());
                    result.incompleteIndividuals.addAll(ax.getIndividualsInSignature());
                    LOG.info("Redundant axiom: {}", ax);
                    LOG.info("Classes involved in wrong axiom: {}", ax.getClassesInSignature());
                    LOG.info("Individuals involved in wrong axiom: {}", ax.getIndividualsInSignature());
                }
            }

            for (OWLAxiom ax : solutionAxioms) {
                if (!subReasoner.isEntailed(ax)) {
                    result.missingAxioms.add(ax);
                    result.incompleteClasses.addAll(ax.getClassesInSignature());
                    result.incompleteIndividuals.addAll(ax.getIndividualsInSignature());
                    LOG.info("Missing axiom: {}", ax);
                    LOG.info("Classes involved in missing axiom: {}", ax.getClassesInSignature());
                    LOG.info("Individuals involved in missing axiom: {}", ax.getIndividualsInSignature());
                }
            }

            LOG.info("Wrong submitted classes: {}", result.incompleteClasses);

            subReasoner.dispose();
            solReasoner.dispose();

            return result;

        } catch (OWLReasonerRuntimeException e) {
            LOG.error("Reasoner error: {}", e.getMessage());
            return null;
        }
    }

    private Set<OWLAxiom> checkForRedundancy(Set<OWLAxiom> submittedAxioms, OWLReasonerFactory factory) {
        // Remove each submitted axiom from a copy of the set of submitted axioms and then check if the set is still entailed by the new reasoner
        OWLOntologyManager tempManager = OWLManager.createOWLOntologyManager();
        OWLOntology temp;
        Set<OWLAxiom> tempAxioms = new HashSet<>();
        OWLReasoner reasoner;
        Set<OWLAxiom> redundantAxioms = new HashSet<>();

        try {
            OWLOntology subOntology = tempManager.createOntology(submittedAxioms);
            OWLReasoner subReasoner = factory.createReasoner(subOntology);

            for (OWLAxiom ax : submittedAxioms) {

                if (ax.isOfType(AxiomType.DECLARATION)) continue;

                tempAxioms.addAll(submittedAxioms);
                tempAxioms.remove(ax);
                temp = tempManager.createOntology(tempAxioms);
                reasoner = factory.createReasoner(temp);
                if (reasoner.isEntailed(submittedAxioms) && subReasoner.isEntailed(tempAxioms)) redundantAxioms.add(ax);
                reasoner.dispose();
            }
        } catch (OWLOntologyCreationException e) {
            LOG.error("Failed to create temporary ontology", e);
        }
        return redundantAxioms;
    }

    private static class OntologyComparisonResult {

        private boolean submittedIsConsistent;
        private boolean solutionIsConsistent;
        private final Set<OWLAxiom> wrongAxioms = new HashSet<>();
        private final Set<OWLAxiom> missingAxioms = new HashSet<>();
        private final Set<OWLClass> incompleteClasses = new HashSet<>();
        private final Set<OWLClass> wrongClasses = new HashSet<>();
        private final Set<OWLClass> missingClasses = new HashSet<>();
        private final Set<OWLClass> classesInSolution = new HashSet<>();
        private final Set<OWLIndividual> incompleteIndividuals = new HashSet<>();
        private final Set<OWLIndividual> missingIndividuals = new HashSet<>();
        private final Set<OWLIndividual> wrongIndividuals = new HashSet<>();
        private final Set<OWLIndividual> individualsInSolution = new HashSet<>();
        private final Map<String, Integer> pointsPerClass = new HashMap<>();
        private final Set<OWLAxiom> redundantAxioms = new HashSet<>();
        private boolean correctIdentifiers;
        private final Set<String> setOfWrongIdentifiers = new HashSet<>();

        public OntologyComparisonResult() {
            this.submittedIsConsistent = false;
            this.solutionIsConsistent = false;
            this.correctIdentifiers = true;
        }
    }
}
