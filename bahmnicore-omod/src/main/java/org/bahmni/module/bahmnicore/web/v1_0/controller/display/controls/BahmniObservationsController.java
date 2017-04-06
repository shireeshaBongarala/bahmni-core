package org.bahmni.module.bahmnicore.web.v1_0.controller.display.controls;

import org.apache.commons.lang3.ObjectUtils;
import org.bahmni.module.bahmnicore.extensions.BahmniExtensions;
import org.bahmni.module.bahmnicore.obs.ObservationsAdder;
import org.bahmni.module.bahmnicore.service.BahmniObsService;
import org.bahmni.module.bahmnicore.util.MiscUtils;
import org.openmrs.Concept;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.VisitService;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniObservation;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/bahmnicore/observations")
public class BahmniObservationsController extends BaseRestController {

    private static final String LATEST = "latest";
    private static final String INITIAL = "initial";
    private BahmniObsService bahmniObsService;
    private ConceptService conceptService;
    private VisitService visitService;
    private BahmniExtensions bahmniExtensions;

    @Autowired
    public BahmniObservationsController(BahmniObsService bahmniObsService, ConceptService conceptService, VisitService visitService, BahmniExtensions bahmniExtensions) {
        this.bahmniObsService = bahmniObsService;
        this.conceptService = conceptService;
        this.visitService = visitService;
        this.bahmniExtensions = bahmniExtensions;
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public Collection<BahmniObservation> get(@RequestParam(value = "patientUuid", required = true) String patientUUID,
                                             @RequestParam(value = "concept", required = true) List<String> rootConceptNames,
                                             @RequestParam(value = "scope", required = false) String scope,
                                             @RequestParam(value = "numberOfVisits", required = false) Integer numberOfVisits,
                                             @RequestParam(value = "obsIgnoreList", required = false) List<String> obsIgnoreList,
                                             @RequestParam(value = "filterObsWithOrders", required = false, defaultValue = "true") Boolean filterObsWithOrders ) throws ParseException {

        List<Concept> rootConcepts = MiscUtils.getConceptsForNames(rootConceptNames, conceptService);

        Collection<BahmniObservation> observations;
        if (ObjectUtils.equals(scope, LATEST)) {
            observations = bahmniObsService.getLatest(patientUUID, rootConcepts, numberOfVisits, obsIgnoreList, filterObsWithOrders, null);
        } else if (ObjectUtils.equals(scope, INITIAL)) {
            observations = bahmniObsService.getInitial(patientUUID, rootConcepts, numberOfVisits, obsIgnoreList, filterObsWithOrders, null);
        } else {
            observations = bahmniObsService.observationsFor(patientUUID, rootConcepts, numberOfVisits, obsIgnoreList, filterObsWithOrders, null, null, null);
        }

        sendObsToGroovyScript(getConceptNames(rootConcepts), observations);

        return observations;
    }

    @RequestMapping(method = RequestMethod.GET, params = {"visitUuid"})
    @ResponseBody
    public Collection<BahmniObservation> get(@RequestParam(value = "visitUuid", required = true) String visitUuid,
                                             @RequestParam(value = "scope", required = false) String scope,
                                             @RequestParam(value = "concept", required = false) List<String> conceptNames,
                                             @RequestParam(value = "obsIgnoreList", required = false) List<String> obsIgnoreList,
                                             @RequestParam(value = "filterObsWithOrders", required = false, defaultValue = "true") Boolean filterObsWithOrders) {

        Visit visit = visitService.getVisitByUuid(visitUuid);
        if (ObjectUtils.equals(scope, INITIAL)) {
            return bahmniObsService.getInitialObsByVisit(visit, MiscUtils.getConceptsForNames(conceptNames, conceptService), obsIgnoreList, filterObsWithOrders);
        } else if (ObjectUtils.equals(scope, LATEST)) {
            return bahmniObsService.getLatestObsByVisit(visit, MiscUtils.getConceptsForNames(conceptNames, conceptService), obsIgnoreList, filterObsWithOrders);
        } else {
            // Sending conceptName and obsIgnorelist, kinda contradicts, since we filter directly on concept names (not on root concept)
            return bahmniObsService.getObservationForVisit(visitUuid, conceptNames, MiscUtils.getConceptsForNames(obsIgnoreList, conceptService), filterObsWithOrders, null);
        }
    }

    @RequestMapping(method = RequestMethod.GET, params = {"encounterUuid"})
    @ResponseBody
    public Collection<BahmniObservation> get(@RequestParam(value = "encounterUuid", required = true) String encounterUuid,
                                             @RequestParam(value = "concept", required = false) List<String> conceptNames) {
        return bahmniObsService.getObservationsForEncounter(encounterUuid, conceptNames);
    }

    @RequestMapping(method = RequestMethod.GET, params = {"patientProgramUuid"})
    @ResponseBody
    public Collection<BahmniObservation> get(@RequestParam(value = "patientProgramUuid", required = true) String patientProgramUuid,
                                             @RequestParam(value = "concept", required = false) List<String> rootConceptNames,
                                             @RequestParam(value = "scope", required = false) String scope) throws ParseException {
        
        Collection<BahmniObservation> observations;
        if (ObjectUtils.equals(scope, LATEST)) {
            observations = bahmniObsService.getLatestObservationsForPatientProgram(patientProgramUuid, rootConceptNames);
        } else if (ObjectUtils.equals(scope, INITIAL)) {
            observations = bahmniObsService.getInitialObservationsForPatientProgram(patientProgramUuid, rootConceptNames);
        } else {
            observations = bahmniObsService.getObservationsForPatientProgram(patientProgramUuid, rootConceptNames);
        }
        sendObsToGroovyScript(rootConceptNames, observations);
        return observations;
    }

    @RequestMapping(method = RequestMethod.GET, params = {"observationUuid"})
    @ResponseBody
    public BahmniObservation get(@RequestParam(value = "observationUuid") String observationUuid,
                                 @RequestParam(value = "revision", required = false) String revision) {
        if (ObjectUtils.equals(revision, LATEST)) {
            return bahmniObsService.getRevisedBahmniObservationByUuid(observationUuid);
        }
        return bahmniObsService.getBahmniObservationByUuid(observationUuid);
    }

    private void sendObsToGroovyScript(List<String> questions, Collection<BahmniObservation> observations) throws ParseException {
        ObservationsAdder observationsAdder = (ObservationsAdder) bahmniExtensions.getExtension("observationsAdder", "CurrentMonthOfTreatment.groovy");
        if (observationsAdder != null)
            observationsAdder.addObservations(observations, questions);
    }

    private List<String> getConceptNames(Collection<Concept> concepts) {
        List<String> conceptNames = new ArrayList<>();
        for (Concept concept : concepts) {
            conceptNames.add(concept.getName().getName());
        }
        return conceptNames;
    }
}
