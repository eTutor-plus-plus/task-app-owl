package at.jku.dke.task_app.owl.evaluation;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.GradingDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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

            StringBuilder redundantAxiomsMessage;
            if (ontologyComparisonResult.redundantAxioms.isEmpty()) {
                redundantAxiomsMessage = new StringBuilder(this.messageSource.getMessage("owl.submission.not-redundant", null, locale));
            } else {
                redundantAxiomsMessage = new StringBuilder(this.messageSource.getMessage("owl.submission.redundant", null, locale));
                for (OWLAxiom ax : ontologyComparisonResult.redundantAxioms) {
                    redundantAxiomsMessage.append("\n").append(" [").append(renderer.render(ax)).append("] ");
                }
            }

            points = calculatePoints(ontologyComparisonResult, task.getMaxPoints());
            int correctness;
            if (points.compareTo(task.getMaxPoints()) >= 0) {
                correctness = 2;
            } else if (points.compareTo(BigDecimal.ZERO) > 0) {
                correctness = 1;
            } else {
                correctness = 0;
            }

            // Handle different evaluation modes
            switch (submission.mode()) {
                case RUN -> feedback = this.messageSource.getMessage("owl.submission.run",
                    new Object[]{submission.submission().input()}, locale);
                case DIAGNOSE -> {

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

                    if (submission.feedbackLevel() > 0) {
                        // Little feedback
                        // Add consistency criterion
                        criteria.add(new CriterionDto(
                            this.messageSource.getMessage("criterium.consistency", null, locale),
                            null,
                            ontologyComparisonResult.submittedIsConsistent,
                            submissionConsistencyMessage
                        ));
                    }
                    if (submission.feedbackLevel() > 1) {
                        // Some feedback
                        // Determine if there are any wrong classes or individuals
                        boolean correctClasses = ontologyComparisonResult.incompleteClasses.isEmpty()
                            && ontologyComparisonResult.redundantClasses.isEmpty()
                            && ontologyComparisonResult.missingClasses.isEmpty();
                        boolean correctIndividuals = ontologyComparisonResult.incompleteIndividuals.isEmpty()
                            && ontologyComparisonResult.redundantIndividuals.isEmpty()
                            && ontologyComparisonResult.missingIndividuals.isEmpty();

                        // Add wrong classes and individuals criterion
                        criteria.add(new CriterionDto(
                            this.messageSource.getMessage("criterium.classes", null, locale),
                            null,
                            correctClasses,
                            correctClasses ? this.messageSource.getMessage("criterium.classes.correct", null, locale) : this.messageSource.getMessage("criterium.classes.wrong", null, locale)
                        ));
                        criteria.add(new CriterionDto(
                            this.messageSource.getMessage("criterium.individuals", null, locale),
                            null,
                            correctIndividuals,
                            correctIndividuals ? this.messageSource.getMessage("criterium.individuals.correct", null, locale) : this.messageSource.getMessage("criterium.individuals.wrong", null, locale)
                        ));

                        // Give out the incomplete, redundant and missing classes
                        if (!ontologyComparisonResult.incompleteClasses.isEmpty()) {
                            feedback += "\n"
                                + this.messageSource.getMessage("owl.submission.incomplete-classes", null, locale)
                                + " ["
                                + ontologyComparisonResult.incompleteClasses.stream().map(cls -> cls.getIRI().getShortForm()).collect(Collectors.joining(", "))
                                + "].";
                        }
                        if (!ontologyComparisonResult.redundantClasses.isEmpty()) {
                            feedback += "\n"
                                + this.messageSource.getMessage("owl.submission.redundant-classes", null, locale)
                                + " ["
                                + ontologyComparisonResult.redundantClasses.stream().map(cls -> cls.getIRI().getShortForm()).collect(Collectors.joining(", "))
                                + "].";
                        }
                        if (!ontologyComparisonResult.missingClasses.isEmpty()) {
                            feedback += "\n"
                                + this.messageSource.getMessage("owl.submission.missing-classes", null, locale)
                                + " ["
                                + ontologyComparisonResult.missingClasses.stream().map(cls -> cls.getIRI().getShortForm()).collect(Collectors.joining(", "))
                                + "].";
                        }

                        // Give out the incomplete, redundant and missing individuals
                        if (!ontologyComparisonResult.incompleteIndividuals.isEmpty()) {
                            feedback += "\n"
                                + this.messageSource.getMessage("owl.submission.incomplete-individuals", null, locale)
                                + " ["
                                + ontologyComparisonResult.incompleteIndividuals.stream().map(ind -> ind.asOWLNamedIndividual().getIRI().getShortForm()).collect(Collectors.joining(", "))
                                + "].";
                        }
                        if (!ontologyComparisonResult.redundantIndividuals.isEmpty()) {
                            feedback += "\n"
                                + this.messageSource.getMessage("owl.submission.redundant-individuals", null, locale)
                                + " ["
                                + ontologyComparisonResult.redundantIndividuals.stream().map(ind -> ind.asOWLNamedIndividual().getIRI().getShortForm()).collect(Collectors.joining(", "))
                                + "].";
                        }
                        if (!ontologyComparisonResult.missingIndividuals.isEmpty()) {
                            feedback += "\n"
                                + this.messageSource.getMessage("owl.submission.missing-individuals", null, locale)
                                + " ["
                                + ontologyComparisonResult.missingIndividuals.stream().map(ind -> ind.asOWLNamedIndividual().getIRI().getShortForm()).collect(Collectors.joining(", "))
                                + "].";
                        }
                    }
                    if (submission.feedbackLevel() > 2) {
                        // Much feedback
                        if (!ontologyComparisonResult.missingAxioms.isEmpty()) {
                            feedback += "\n" + missingAxiomsMessage;
                        }
                        if (!ontologyComparisonResult.redundantAxioms.isEmpty()) {
                            feedback += "\n" + redundantAxiomsMessage;
                        }
                    }
                }
                case SUBMIT -> {
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
                }
                default -> throw new IllegalStateException("Unexpected evaluation mode: " + submission.mode());
            }

        } catch (OWLRuntimeException | OWLOntologyStorageException e) {
            LOG.error("Failed to parse Manchester syntax", e);
            criteria.add(new CriterionDto(
                this.messageSource.getMessage("criterium.syntax", null, locale),
                null,
                false,
                e.getMessage()
            ));
            feedback = this.messageSource.getMessage("error.syntax", null, locale);
        }

        return new GradingDto(task.getMaxPoints(), points, feedback, criteria);
    }

    private static BigDecimal calculatePoints(OntologyComparisonResult ontologyComparisonResult, BigDecimal maxPoints) {

        BigDecimal points = maxPoints;

        // No classes or individuals in solution --> no mistakes to make
        if(ontologyComparisonResult.classesInSolution.isEmpty() && ontologyComparisonResult.individualsInSolution.isEmpty()) return points;
        // No points for inconsistent submissions
        if(!ontologyComparisonResult.submittedIsConsistent) return BigDecimal.ZERO;

        BigDecimal pointsPerClass = maxPoints.divide(BigDecimal.valueOf(ontologyComparisonResult.classesInSolution.size() + ontologyComparisonResult.individualsInSolution.size()), 2, RoundingMode.HALF_UP);

        // Subtract points for wrong classes and individuals, including ones that are missing/redundant
        Set<OWLClass> wrongClasses = new HashSet<>(ontologyComparisonResult.incompleteClasses);
        wrongClasses.addAll(ontologyComparisonResult.redundantClasses);
        wrongClasses.addAll(ontologyComparisonResult.missingClasses);

        Set<OWLIndividual> wrongIndividuals = new HashSet<>(ontologyComparisonResult.incompleteIndividuals);
        wrongIndividuals.addAll(ontologyComparisonResult.redundantIndividuals);
        wrongIndividuals.addAll(ontologyComparisonResult.missingIndividuals);

        points = points.subtract(pointsPerClass.multiply(BigDecimal.valueOf(wrongClasses.size() + wrongIndividuals.size())));

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
            throw new OWLRuntimeException(e.getMessage());
        }
    }

    private OntologyComparisonResult compareOntologies(OWLOntology submittedOntology, OWLOntology solutionOntology) {
        try {
            OWLReasonerFactory factory = new Reasoner.ReasonerFactory();
            OWLReasoner subReasoner = factory.createReasoner(submittedOntology);
            OWLReasoner solReasoner = factory.createReasoner(solutionOntology);

            // Check consistency first
            boolean subIsConsistent = true;
            boolean solIsConsistent = true;
            if (!subReasoner.isConsistent()) {
                LOG.error("Submitted ontology is inconsistent");
                subIsConsistent = false;
            }
            if (!solReasoner.isConsistent()) {
                LOG.error("Solution ontology is inconsistent");
                solIsConsistent = false;
            }

            OntologyComparisonResult result = new OntologyComparisonResult();

            // Save classes and individuals in solution for calculating points later
            result.classesInSolution.addAll(solutionOntology.getClassesInSignature());
            LOG.info("Number of classes in solution: {}", result.classesInSolution);
            result.individualsInSolution.addAll(solutionOntology.getIndividualsInSignature());
            LOG.info("Number of individuals in solution: {}", result.individualsInSolution);

            // Save redundant and missing classes
            result.redundantClasses.addAll(submittedOntology.getClassesInSignature());
            result.redundantClasses.removeAll(solutionOntology.getClassesInSignature());
            LOG.info("Redundant classes: {}", result.redundantClasses);

            result.missingClasses.addAll(solutionOntology.getClassesInSignature());
            result.missingClasses.removeAll(submittedOntology.getClassesInSignature());
            LOG.info("Missing classes: {}", result.missingClasses);

            // Save redundant and missing individuals
            result.missingIndividuals.addAll(solutionOntology.getIndividualsInSignature());
            result.missingIndividuals.removeAll(submittedOntology.getIndividualsInSignature());
            LOG.info("Missing individuals: {}", result.missingIndividuals);

            result.redundantIndividuals.addAll(submittedOntology.getIndividualsInSignature());
            result.redundantIndividuals.removeAll(solutionOntology.getIndividualsInSignature());
            LOG.info("Redundant individuals: {}", result.redundantIndividuals);

            result.submittedIsConsistent = subIsConsistent;
            result.solutionIsConsistent = solIsConsistent;
            if (!subIsConsistent || !solIsConsistent) {
                return result;
            }

            Set<OWLAxiom> submittedAxioms = submittedOntology.getAxioms();
            Set<OWLAxiom> solutionAxioms = solutionOntology.getAxioms();

            // Log both ontologies' axioms
            LOG.info("Submitted ontology axioms:");
            submittedAxioms.forEach(ax -> LOG.info(ax.toString()));
            LOG.info("Solution ontology axioms:");
            solutionAxioms.forEach(ax -> LOG.info(ax.toString()));

            // Check mutual entailment
            // Store non-entailed axioms for detailed feedback
            for (OWLAxiom ax : submittedAxioms) {
                if (!solReasoner.isEntailed(ax)) {
                    result.redundantAxioms.add(ax);
                    result.incompleteClasses.addAll(ax.getClassesInSignature());
                    result.incompleteIndividuals.addAll(ax.getIndividualsInSignature());
                    LOG.info("Redundant axiom: {}", ax);
                    LOG.info("Classes involved in redundant axiom: {}", ax.getClassesInSignature());
                    LOG.info("Individuals involved in redundant axiom: {}", ax.getIndividualsInSignature());
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

            return result;

        } catch (OWLReasonerRuntimeException e) {
            LOG.error("Reasoner error: {}", e.getMessage());
            return null;
        }
    }

    private static class OntologyComparisonResult {

        private boolean submittedIsConsistent;
        private boolean solutionIsConsistent;
        private final Set<OWLAxiom> redundantAxioms = new HashSet<>();
        private final Set<OWLAxiom> missingAxioms = new HashSet<>();
        private final Set<OWLClass> incompleteClasses = new HashSet<>();
        private final Set<OWLClass> redundantClasses = new HashSet<>();
        private final Set<OWLClass> missingClasses = new HashSet<>();
        private final Set<OWLClass> classesInSolution = new HashSet<>();
        private final Set<OWLIndividual> incompleteIndividuals = new HashSet<>();
        private final Set<OWLIndividual> missingIndividuals = new HashSet<>();
        private final Set<OWLIndividual> redundantIndividuals = new HashSet<>();
        private final Set<OWLIndividual> individualsInSolution = new HashSet<>();

        public OntologyComparisonResult() {
            this.submittedIsConsistent = false;
            this.solutionIsConsistent = false;
        }
    }
}
