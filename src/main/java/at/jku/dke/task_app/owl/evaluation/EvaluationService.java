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
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.OWLReasonerRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
            StringDocumentTarget target = new StringDocumentTarget();
            submittedOntology.getOWLOntologyManager().saveOntology(submittedOntology, new ManchesterSyntaxDocumentFormat(), target);
            LOG.info("Submitted: \n{}", target);
            StringDocumentTarget target2 = new StringDocumentTarget();
            submittedOntology.getOWLOntologyManager().saveOntology(solutionOntology, new ManchesterSyntaxDocumentFormat(), target2);
            LOG.info("Solution: \n{}", target2);

            // Compare ontologies
            OntologyComparisonResult ontologyComparisonResult = compareOntologies(submittedOntology, solutionOntology);

            // Prepare feedback message parts
            assert ontologyComparisonResult != null;
            String submissionConsistencyMessage = this.messageSource.getMessage(ontologyComparisonResult.submittedIsConsistent ? "owl.submission.consistent" : "owl.submission.inconsistent", null, locale);
            String solutionConsistencyMessage = this.messageSource.getMessage(ontologyComparisonResult.solutionIsConsistent ? "owl.solution.consistent" : "owl.solution.inconsistent", null, locale);
            StringBuilder subEntailsMessage;
            if (ontologyComparisonResult.submittedEntailsSolution) {
                subEntailsMessage = new StringBuilder(this.messageSource.getMessage("owl.submission.entails", null, locale));
            } else {
                subEntailsMessage = new StringBuilder(this.messageSource.getMessage("owl.submission.doesnt-entail", null, locale));
                for (OWLAxiom ax : ontologyComparisonResult.nonEntailedSubmittedAxioms) {
                    subEntailsMessage.append("\n").append(ax.toString());
                }
            }

            StringBuilder solEntailsMessage;
            if (ontologyComparisonResult.solutionEntailsSubmitted) {
                solEntailsMessage = new StringBuilder(this.messageSource.getMessage("owl.solution.entails", null, locale));
            } else {
                solEntailsMessage = new StringBuilder(this.messageSource.getMessage("owl.solution.doesnt-entail", null, locale));
                for (OWLAxiom ax : ontologyComparisonResult.nonEntailedSolutionAxioms) {
                    solEntailsMessage.append("\n").append(ax.toString());
                }
            }
            StringBuilder classMismatchMessage = new StringBuilder(this.messageSource.getMessage("owl.class.mismatch", null, locale));
            if (!ontologyComparisonResult.classMismatches.isEmpty()) {
                for (OWLClass cls : ontologyComparisonResult.classMismatches) {
                    classMismatchMessage.append("\n").append(cls.toString());
                }
            }
            StringBuilder objectPropMismatchMessage = new StringBuilder(this.messageSource.getMessage("owl.object-prop.mismatch", null, locale));
            if (!ontologyComparisonResult.objectPropertyMismatches.isEmpty()) {
                for (OWLObjectProperty prop : ontologyComparisonResult.objectPropertyMismatches) {
                    objectPropMismatchMessage.append("\n").append(prop.toString());
                }
            }
            StringBuilder dataPropMismatchMessage = new StringBuilder(this.messageSource.getMessage("owl.data-prop.mismatch", null, locale));
            if (!ontologyComparisonResult.dataPropertyMismatches.isEmpty()) {
                for (OWLDataProperty prop : ontologyComparisonResult.dataPropertyMismatches) {
                    dataPropMismatchMessage.append("\n").append(prop.toString());
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

                    boolean needsFurtherFeedback = false;
                    switch (correctness) {
                        case 0: {
                            feedback = this.messageSource.getMessage("owl.submission.incorrect", null, locale);
                            needsFurtherFeedback = true;
                            break;
                        }
                        case 1: {
                            feedback = this.messageSource.getMessage("owl.submission.partially-correct", null, locale);
                            needsFurtherFeedback = true;
                            break;
                        }
                        case 2: {
                            feedback = this.messageSource.getMessage("owl.submission.correct", null, locale);
                            break;
                        }
                    }

                    if (needsFurtherFeedback) {
                        if (!ontologyComparisonResult.submittedIsConsistent) feedback += "\n" + submissionConsistencyMessage;
                        if (!ontologyComparisonResult.solutionIsConsistent) feedback += "\n" + solutionConsistencyMessage;
                        if (!ontologyComparisonResult.submittedEntailsSolution) feedback += "\n" + subEntailsMessage;
                        if (!ontologyComparisonResult.solutionEntailsSubmitted) feedback += "\n" + solEntailsMessage;
                        if (!ontologyComparisonResult.classMismatches.isEmpty()) feedback += "\n" + classMismatchMessage;
                        if (!ontologyComparisonResult.objectPropertyMismatches.isEmpty()) feedback += "\n" + objectPropMismatchMessage;
                        if (!ontologyComparisonResult.dataPropertyMismatches.isEmpty()) feedback += "\n" + dataPropMismatchMessage;
                    }

                    // TODO
                    if (submission.feedbackLevel() > 0) {
                        addDetailedFeedback(criteria, submittedOntology, solutionOntology, locale, points);
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

        } catch (OWLRuntimeException e) {
            LOG.error("Failed to parse Manchester syntax", e);
            criteria.add(new CriterionDto(
                this.messageSource.getMessage("criterium.syntax", null, locale),
                null,
                false,
                e.getMessage()
            ));
            feedback = this.messageSource.getMessage("error.syntax", null, locale);
        } catch (OWLOntologyStorageException e) {
            System.err.println("Failed to render ontology: " + e.getMessage());
            feedback = this.messageSource.getMessage("error.syntax", null, locale);
        }

        return new GradingDto(task.getMaxPoints(), points, feedback, criteria);
    }

    private static BigDecimal calculatePoints(OntologyComparisonResult ontologyComparisonResult, BigDecimal maxPoints) {

        BigDecimal points = BigDecimal.ZERO;

        // 20%
        if (ontologyComparisonResult.submittedIsConsistent) {
            points = points.add(new BigDecimal("0.20"));
        }

        // 40%
        if (ontologyComparisonResult.submittedEntailsSolution) {
            points = points.add(new BigDecimal("0.20"));
        }
        if (ontologyComparisonResult.solutionEntailsSubmitted) {
            points = points.add(new BigDecimal("0.20"));
        }

        // 40%
        if (ontologyComparisonResult.classMismatches.isEmpty()) {
            points = points.add(new BigDecimal("0.15"));
        }
        if (ontologyComparisonResult.objectPropertyMismatches.isEmpty()) {
            points = points.add(new BigDecimal("0.15"));
        }
        if (ontologyComparisonResult.dataPropertyMismatches.isEmpty()) {
            points = points.add(new BigDecimal("0.10"));
        }

        return points.multiply(maxPoints);
    }

    // Parses Manchester Syntax String Input into OWL Ontology
    private OWLOntology parseManchesterSyntax(String manchesterSyntaxInput) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        try {
            // TODO Validate Syntax

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
            result.submittedIsConsistent = subIsConsistent;
            result.solutionIsConsistent = solIsConsistent;
            if (!subIsConsistent || !solIsConsistent) {
                return result;
            }

            // Precompute inferences
            subReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY,
                InferenceType.OBJECT_PROPERTY_HIERARCHY,
                InferenceType.DATA_PROPERTY_HIERARCHY);
            solReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY,
                InferenceType.OBJECT_PROPERTY_HIERARCHY,
                InferenceType.DATA_PROPERTY_HIERARCHY);

            // Log both ontology structures
            LOG.info("Submitted ontology structure:");
            submittedOntology.logicalAxioms().forEach(ax -> LOG.info(ax.toString()));

            LOG.info("Solution ontology structure:");
            solutionOntology.logicalAxioms().forEach(ax -> LOG.info(ax.toString()));

            // Check mutual entailment
            Set<OWLAxiom> submittedAxioms = submittedOntology.getAxioms();
            Set<OWLAxiom> solutionAxioms = solutionOntology.getAxioms();

            result.submittedEntailsSolution = solReasoner.isEntailed(submittedAxioms);
            result.solutionEntailsSubmitted = subReasoner.isEntailed(solutionAxioms);

            // Store non-entailed axioms for detailed feedback
            for (OWLAxiom ax : submittedAxioms) {
                if (!subReasoner.isEntailed(ax)) {
                    result.nonEntailedSubmittedAxioms.add(ax);
                }
            }
            for (OWLAxiom ax : solutionAxioms) {
                if (!solReasoner.isEntailed(ax)) {
                    result.nonEntailedSolutionAxioms.add(ax);
                }
            }

            // Compare classes
            Set<OWLClass> allClasses = submittedOntology.classesInSignature().collect(Collectors.toSet());
            allClasses.addAll(solutionOntology.classesInSignature().collect(Collectors.toSet()));
            for (OWLClass cls : allClasses) {
                if (cls.isOWLNothing()) continue;

                Set<OWLClass> submittedSuper = subReasoner.getSuperClasses(cls, false).entities().collect(Collectors.toSet());
                Set<OWLClass> solutionSuper = solReasoner.getSuperClasses(cls, false).entities().collect(Collectors.toSet());
                Set<OWLClass> submittedEq = subReasoner.getEquivalentClasses(cls).entities().collect(Collectors.toSet());
                Set<OWLClass> solutionEq = solReasoner.getEquivalentClasses(cls).entities().collect(Collectors.toSet());

                LOG.info("Class: {} - Comparing hierarchies:", cls);
                LOG.info("Submitted super classes: {}", submittedSuper);
                LOG.info("Solution super classes: {}", solutionSuper);
                LOG.info("Submitted equivalent classes: {}", submittedEq);
                LOG.info("Solution equivalent classes: {}", solutionEq);



                if (!submittedSuper.equals(solutionSuper) || !submittedEq.equals(solutionEq)) {
                    LOG.info("Hierarchies don't match for class: {}", cls);
                    result.classMismatches.add(cls);
                }
            }

            // Compare object properties
            Set<OWLObjectProperty> allObjProps = submittedOntology.objectPropertiesInSignature().collect(Collectors.toSet());
            allObjProps.addAll(solutionOntology.objectPropertiesInSignature().collect(Collectors.toSet()));
            for (OWLObjectProperty prop : allObjProps) {
                Set<OWLObjectPropertyExpression> submittedSuper = subReasoner.getSuperObjectProperties(prop, false).entities().collect(Collectors.toSet());
                Set<OWLObjectPropertyExpression> solutionSuper = solReasoner.getSuperObjectProperties(prop, false).entities().collect(Collectors.toSet());
                Set<OWLObjectPropertyExpression> submittedEq = subReasoner.getEquivalentObjectProperties(prop).entities().collect(Collectors.toSet());
                Set<OWLObjectPropertyExpression> solutionEq = solReasoner.getEquivalentObjectProperties(prop).entities().collect(Collectors.toSet());

                LOG.info("Object Property: {} - Comparing hierarchies:", prop);
                LOG.info("Submitted super object properties: {}", submittedSuper);
                LOG.info("Solution super object properties: {}", solutionSuper);
                LOG.info("Submitted equivalent object properties: {}", submittedEq);
                LOG.info("Solution equivalent object properties: {}", solutionEq);

                if (!submittedSuper.equals(solutionSuper) || !submittedEq.equals(solutionEq)) {
                    LOG.info("Hierarchies don't match for object property: {}", prop);
                    result.objectPropertyMismatches.add(prop);
                }
            }

            // Compare data properties
            Set<OWLDataProperty> allDataProps = submittedOntology.dataPropertiesInSignature().collect(Collectors.toSet());
            allDataProps.addAll(solutionOntology.dataPropertiesInSignature().collect(Collectors.toSet()));
            for (OWLDataProperty prop : allDataProps) {
                Set<OWLDataProperty> submittedSuper = subReasoner.getSuperDataProperties(prop, false).entities().collect(Collectors.toSet());
                Set<OWLDataProperty> solutionSuper = solReasoner.getSuperDataProperties(prop, false).entities().collect(Collectors.toSet());
                Set<OWLDataProperty> submittedEq = subReasoner.getEquivalentDataProperties(prop).entities().collect(Collectors.toSet());
                Set<OWLDataProperty> solutionEq = solReasoner.getEquivalentDataProperties(prop).entities().collect(Collectors.toSet());

                LOG.info("Data Property: {} - Comparing hierarchies:", prop);
                LOG.info("Submitted super data properties: {}", submittedSuper);
                LOG.info("Solution super data properties: {}", solutionSuper);
                LOG.info("Submitted equivalent data properties: {}", submittedEq);
                LOG.info("Solution equivalent data properties: {}", solutionEq);

                if (!submittedSuper.equals(solutionSuper) || !submittedEq.equals(solutionEq)) {
                    LOG.info("Hierarchies don't match for data property: {}", prop);
                    result.dataPropertyMismatches.add(prop);
                }
            }

            return result;

        } catch (OWLReasonerRuntimeException e) {
            LOG.error("Reasoner error: {}", e.getMessage());
            return null;
        }
    }

    private static class OntologyComparisonResult {
        private final Set<OWLClass> classMismatches = new HashSet<>();
        private final Set<OWLObjectProperty> objectPropertyMismatches = new HashSet<>();
        private final Set<OWLDataProperty> dataPropertyMismatches = new HashSet<>();
        private boolean submittedIsConsistent;
        private boolean solutionIsConsistent;
        private boolean submittedEntailsSolution;
        private boolean solutionEntailsSubmitted;
        private final Set<OWLAxiom> nonEntailedSubmittedAxioms = new HashSet<>();
        private final Set<OWLAxiom> nonEntailedSolutionAxioms = new HashSet<>();

        public OntologyComparisonResult() {
            this.submittedIsConsistent = false;
            this.solutionIsConsistent = false;
            this.submittedEntailsSolution = false; // is updated during comparison
            this.solutionEntailsSubmitted = false; // is updated during comparison
        }
    }

    private boolean areEquivalentClassHierarchies(OWLClass owlClass, OWLReasoner r1, OWLReasoner r2) {
        return r1.getEquivalentClasses(owlClass).equals(r2.getEquivalentClasses(owlClass)) &&
               r1.getSuperClasses(owlClass).equals(r2.getSuperClasses(owlClass)) &&
               r1.getSubClasses(owlClass).equals(r2.getSubClasses(owlClass));
    }

    private boolean areEquivalentObjectPropertyHierarchies(OWLObjectProperty objProp, OWLReasoner r1, OWLReasoner r2) {
        return r1.getEquivalentObjectProperties(objProp).equals(r2.getEquivalentObjectProperties(objProp)) &&
               r1.getSuperObjectProperties(objProp).equals(r2.getSuperObjectProperties(objProp)) &&
               r1.getSubObjectProperties(objProp).equals(r2.getSubObjectProperties(objProp));
    }

    private boolean areEquivalentDataPropertyHierarchies(OWLDataProperty dataProp, OWLReasoner r1, OWLReasoner r2) {
        return r1.getEquivalentDataProperties(dataProp).equals(r2.getEquivalentDataProperties(dataProp)) &&
               r1.getSuperDataProperties(dataProp).equals(r2.getSuperDataProperties(dataProp)) &&
               r1.getSubDataProperties(dataProp).equals(r2.getSubDataProperties(dataProp));
    }


    private void addDetailedFeedback(List<CriterionDto> criteria,
                                     OWLOntology submitted,
                                     OWLOntology solution,
                                     Locale locale,
                                     BigDecimal points) {
        try {
            // Create reasoner factory and reasoners
            OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
            OWLReasoner submittedReasoner = reasonerFactory.createReasoner(submitted);
            OWLReasoner solutionReasoner = reasonerFactory.createReasoner(solution);

            // Check ontology consistency
            boolean isConsistent = submittedReasoner.isConsistent() && solutionReasoner.isConsistent();
            if (!isConsistent) {
                criteria.add(new CriterionDto(
                    this.messageSource.getMessage("criterium.consistency", null, locale),
                    BigDecimal.ZERO,
                    false,
                    this.messageSource.getMessage("criterium.consistency.invalid", null, locale)
                ));
                return;
            }

            // Compare class hierarchies (40% of points)
            boolean classesMatch = true;
            for (OWLClass cls : solution.getClassesInSignature()) {
                if (!areEquivalentClassHierarchies(cls, submittedReasoner, solutionReasoner)) {
                    classesMatch = false;
                    break;
                }
            }
            criteria.add(new CriterionDto(
                this.messageSource.getMessage("criterium.classes", null, locale),
                classesMatch ? points.multiply(BigDecimal.valueOf(0.4)) : BigDecimal.ZERO,
                classesMatch,
                this.messageSource.getMessage(classesMatch ?
                    "criterium.classes.match" : "criterium.classes.mismatch", null, locale)
            ));

            // Compare object property hierarchies (30% of points)
            boolean objectPropertiesMatch = true;
            for (OWLObjectProperty prop : solution.getObjectPropertiesInSignature()) {
                if (!areEquivalentObjectPropertyHierarchies(prop, submittedReasoner, solutionReasoner)) {
                    objectPropertiesMatch = false;
                    break;
                }
            }
            criteria.add(new CriterionDto(
                this.messageSource.getMessage("criterium.object.properties", null, locale),
                objectPropertiesMatch ? points.multiply(BigDecimal.valueOf(0.3)) : BigDecimal.ZERO,
                objectPropertiesMatch,
                this.messageSource.getMessage(objectPropertiesMatch ?
                    "criterium.object.properties.match" : "criterium.object.properties.mismatch", null, locale)
            ));

            // Compare data property hierarchies (30% of points)
            boolean dataPropertiesMatch = true;
            for (OWLDataProperty prop : solution.getDataPropertiesInSignature()) {
                if (!areEquivalentDataPropertyHierarchies(prop, submittedReasoner, solutionReasoner)) {
                    dataPropertiesMatch = false;
                    break;
                }
            }
            criteria.add(new CriterionDto(
                this.messageSource.getMessage("criterium.data.properties", null, locale),
                dataPropertiesMatch ? points.multiply(BigDecimal.valueOf(0.3)) : BigDecimal.ZERO,
                dataPropertiesMatch,
                this.messageSource.getMessage(dataPropertiesMatch ?
                    "criterium.data.properties.match" : "criterium.data.properties.mismatch", null, locale)
            ));

        } catch (Exception e) {
            LOG.error("Error during detailed feedback generation", e);
            criteria.add(new CriterionDto(
                this.messageSource.getMessage("criterium.error", null, locale),
                BigDecimal.ZERO,
                false,
                e.getMessage()
            ));
        }
    }
}
