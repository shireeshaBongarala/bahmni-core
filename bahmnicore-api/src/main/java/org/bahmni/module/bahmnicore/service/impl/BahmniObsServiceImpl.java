package org.bahmni.module.bahmnicore.service.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bahmni.module.bahmnicore.dao.ObsDao;
import org.bahmni.module.bahmnicore.dao.VisitDao;
import org.bahmni.module.bahmnicore.dao.impl.ObsDaoImpl;
import org.bahmni.module.bahmnicore.dao.impl.ObsDaoImpl.OrderBy;
import org.bahmni.module.bahmnicore.service.BahmniObsService;
import org.bahmni.module.bahmnicore.service.BahmniProgramWorkflowService;
import org.bahmni.module.bahmnicore.util.MiscUtils;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Person;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.VisitService;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniObservation;
import org.openmrs.module.bahmniemrapi.encountertransaction.mapper.OMRSObsToBahmniObsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Service
public class BahmniObsServiceImpl implements BahmniObsService {

    private ObsDao obsDao;
    private VisitDao visitDao;
    private OMRSObsToBahmniObsMapper omrsObsToBahmniObsMapper;
    private VisitService visitService;
    private ConceptService conceptService;
    private BahmniProgramWorkflowService programWorkflowService;
    private ObsService obsService;

    @Autowired
    public BahmniObsServiceImpl(ObsDao obsDao, OMRSObsToBahmniObsMapper omrsObsToBahmniObsMapper, VisitService visitService, ConceptService conceptService, VisitDao visitDao, BahmniProgramWorkflowService programWorkflowService, ObsService obsService) {
        this.obsDao = obsDao;
        this.omrsObsToBahmniObsMapper = omrsObsToBahmniObsMapper;
        this.visitService = visitService;
        this.conceptService = conceptService;
        this.visitDao = visitDao;
        this.programWorkflowService = programWorkflowService;
        this.obsService = obsService;
    }

    @Override
    public List<Obs> getObsForPerson(String identifier) {
        return obsDao.getNumericObsByPerson(identifier);
    }

    @Override
    public Collection<BahmniObservation> observationsFor(String patientUuid, Collection<Concept> concepts, Integer numberOfVisits,
                                                         List<String> obsIgnoreList, Boolean filterOutOrderObs, Order order, Date startDate, Date endDate) {
        if (CollectionUtils.isNotEmpty(concepts)) {
            List<String> conceptNames = getConceptNames(concepts);

            List<Obs> observations = obsDao.getObsByPatientAndVisit(patientUuid, conceptNames,
                    visitDao.getVisitIdsFor(patientUuid, numberOfVisits), -1, ObsDaoImpl.OrderBy.DESC, obsIgnoreList, filterOutOrderObs, order, startDate, endDate);

            return omrsObsToBahmniObsMapper.map(observations, concepts);
        }
        return Collections.EMPTY_LIST;
    }

    private List<String> getConceptNames(Collection<Concept> concepts) {
        List<String> conceptNames = new ArrayList<>();
        for (Concept concept : concepts) {
            conceptNames.add(concept.getName().getName());
        }
        return conceptNames;
    }

    @Override
    public Collection<BahmniObservation> observationsFor(String patientUuid, Concept rootConcept, Concept childConcept, Integer numberOfVisits, Date startDate, Date endDate, String patientProgramUuid)  {
        Collection<Encounter> encounters = programWorkflowService.getEncountersByPatientProgramUuid(patientProgramUuid);
        if (programDoesNotHaveEncounters(patientProgramUuid, encounters)) return Collections.EMPTY_LIST;

        List<Obs> observations = obsDao.getObsFor(patientUuid, rootConcept, childConcept, visitDao.getVisitIdsFor(patientUuid, numberOfVisits), encounters, startDate, endDate);

        return convertToBahmniObservation(observations);
    }

    private boolean programDoesNotHaveEncounters(String patientProgramUuid, Collection<Encounter> encounters) {
        return StringUtils.isNotEmpty(patientProgramUuid) && encounters.size() == 0;
    }

    private List<BahmniObservation> convertToBahmniObservation(List<Obs> observations) {
        List<BahmniObservation> bahmniObservations = new ArrayList<>();
        for (Obs observation : observations) {
            BahmniObservation bahmniObservation = omrsObsToBahmniObsMapper.map(observation);
            bahmniObservations.add(bahmniObservation);
        }
        return bahmniObservations;
    }

    @Override
    public Collection<BahmniObservation> getLatest(String patientUuid, Collection<Concept> concepts, Integer numberOfVisits, List<String> obsIgnoreList,
                                                   Boolean filterOutOrderObs, Order order) {
        List<Obs> latestObs = new ArrayList<>();
        if (concepts == null)
            return new ArrayList<>();
        for (Concept concept : concepts) {
            List<Obs> observations = obsDao.getObsByPatientAndVisit(patientUuid, Arrays.asList(concept.getName().getName()),
                        visitDao.getVisitIdsFor(patientUuid, numberOfVisits), -1, ObsDaoImpl.OrderBy.DESC, obsIgnoreList, filterOutOrderObs, order, null, null);
            if(CollectionUtils.isNotEmpty(observations)) {
                latestObs.addAll(getAllLatestObsForAConcept(observations));
            }
        }

        return omrsObsToBahmniObsMapper.map(latestObs, concepts);
    }

    @Override
    public Collection<BahmniObservation> getLatestObsByVisit(Visit visit, Collection<Concept> concepts, List<String> obsIgnoreList, Boolean filterOutOrderObs) {
        List<Obs> latestObs = new ArrayList<>();
        for (Concept concept : concepts) {
            latestObs.addAll(obsDao.getObsByPatientAndVisit(visit.getPatient().getUuid(), Arrays.asList(concept.getName().getName()),
                    Arrays.asList(visit.getVisitId()), 1, ObsDaoImpl.OrderBy.DESC, obsIgnoreList, filterOutOrderObs, null, null, null));
        }

        return omrsObsToBahmniObsMapper.map(latestObs, concepts);
    }

    @Override
    public Collection<BahmniObservation> getInitial(String patientUuid, Collection<Concept> conceptNames,
                                                    Integer numberOfVisits, List<String> obsIgnoreList, Boolean filterOutOrderObs, Order order) {
        List<Obs> latestObs = new ArrayList<>();
        for (Concept concept : conceptNames) {
            latestObs.addAll(obsDao.getObsByPatientAndVisit(patientUuid, Arrays.asList(concept.getName().getName()),
                    visitDao.getVisitIdsFor(patientUuid, numberOfVisits), 1, ObsDaoImpl.OrderBy.ASC, obsIgnoreList, filterOutOrderObs, order, null, null));
        }

        return omrsObsToBahmniObsMapper.map(latestObs, conceptNames);
    }

    @Override
    public Collection<BahmniObservation> getInitialObsByVisit(Visit visit, List<Concept> concepts, List<String> obsIgnoreList, Boolean filterObsWithOrders) {
        List<Obs> latestObs = new ArrayList<>();
        for (Concept concept : concepts) {
            latestObs.addAll(obsDao.getObsByPatientAndVisit(visit.getPatient().getUuid(), Arrays.asList(concept.getName().getName()),
                    Arrays.asList(visit.getVisitId()), 1, ObsDaoImpl.OrderBy.ASC, obsIgnoreList, filterObsWithOrders, null, null, null));
        }

        Collection<BahmniObservation> map = omrsObsToBahmniObsMapper.map(latestObs, concepts);
        return map;
    }

    @Override
    public List<Concept> getNumericConceptsForPerson(String personUUID) {
        return obsDao.getNumericConceptsForPerson(personUUID);
    }

    @Override
    public Collection<BahmniObservation> getLatestObsForConceptSetByVisit(String patientUuid, String conceptName, Integer visitId) {
        List<Obs> obs =  withUniqueConcepts(filterByRootConcept(obsDao.getLatestObsForConceptSetByVisit(patientUuid, conceptName, visitId), conceptName));
        return omrsObsToBahmniObsMapper.map(obs, Arrays.asList(getConceptByName(conceptName)));
    }

    @Override
    public Collection<BahmniObservation> getObservationForVisit(String visitUuid, List<String> conceptNames, Collection<Concept> obsIgnoreList, Boolean filterOutOrders, Order order) {
        Visit visit = visitService.getVisitByUuid(visitUuid);
        List<Person> persons = new ArrayList<>();
        persons.add(visit.getPatient());
        List<Obs> observations = obsDao.getObsForVisits(persons, new ArrayList<>(visit.getEncounters()),
                MiscUtils.getConceptsForNames(conceptNames, conceptService), obsIgnoreList, filterOutOrders, order);
        observations = new ArrayList<>(getObsAtTopLevelAndApplyIgnoreList(observations, conceptNames, obsIgnoreList));
        return omrsObsToBahmniObsMapper.map(observations, null);
    }

    @Override
    public Collection<BahmniObservation> getObservationsForEncounter(String encounterUuid, List<String> conceptNames) {
        List<Obs> observations = obsDao.getObsForConceptsByEncounter(encounterUuid, conceptNames);
        return omrsObsToBahmniObsMapper.map(observations, getConceptsByName(conceptNames));
    }

    @Override
    public Collection<BahmniObservation> getObservationsForPatientProgram(String patientProgramUuid, List<String> conceptNames) {
        List<Obs> observations = new ArrayList<>();
        if (conceptNames == null)
            return new ArrayList<>();
        for (String conceptName : conceptNames) {
            observations.addAll(obsDao.getObsByPatientProgramUuidAndConceptNames(patientProgramUuid, Arrays.asList(conceptName), null, ObsDaoImpl.OrderBy.DESC, null, null));
        }

        return omrsObsToBahmniObsMapper.map(observations, getConceptsByName(conceptNames));
    }

    @Override
    public Collection<BahmniObservation> getLatestObservationsForPatientProgram(String patientProgramUuid, List<String> conceptNames) {
        List<Obs> observations = new ArrayList<>();
        if (conceptNames == null)
            return new ArrayList<>();
        for (String conceptName : conceptNames) {
            List<Obs> obsByPatientProgramUuidAndConceptName = obsDao.getObsByPatientProgramUuidAndConceptNames(patientProgramUuid, Arrays.asList(conceptName), null, OrderBy.DESC, null, null);
            List<Obs> obsList = getAllLatestObsForAConcept(obsByPatientProgramUuidAndConceptName);
            if (CollectionUtils.isNotEmpty(obsList)) {
                observations.addAll(obsList);
            }
        }

        return omrsObsToBahmniObsMapper.map(observations, getConceptsByName(conceptNames));
    }

    @Override
    public Collection<BahmniObservation> getInitialObservationsForPatientProgram(String patientProgramUuid, List<String> conceptNames) {
        List<Obs> observations = new ArrayList<>();
        if (conceptNames == null)
            return new ArrayList<>();
        for (String conceptName : conceptNames) {
                observations.addAll(obsDao.getObsByPatientProgramUuidAndConceptNames(patientProgramUuid, Arrays.asList(conceptName), 1, ObsDaoImpl.OrderBy.ASC, null, null));
        }

        return omrsObsToBahmniObsMapper.map(observations, getConceptsByName(conceptNames));
    }

    @Override
    public BahmniObservation getBahmniObservationByUuid(String observationUuid) {
        Obs obs = obsService.getObsByUuid(observationUuid);
        return omrsObsToBahmniObsMapper.map(obs);
    }

    @Override
    public BahmniObservation getRevisedBahmniObservationByUuid(String observationUuid) {
        Obs obs = obsService.getObsByUuid(observationUuid);
        if (obs.getVoided()) {
            obs = getRevisionObs(obs);
        }
        return omrsObsToBahmniObsMapper.map(obs);
    }

    private Obs getRevisionObs(Obs initialObs) {
        Obs revisedObs = obsDao.getRevisionObs(initialObs);
        if (revisedObs != null && revisedObs.getVoided()) {
            revisedObs = getRevisionObs(revisedObs);
        }
        return revisedObs;
    }

    @Override
    public Collection<BahmniObservation> getObservationsForOrder(String orderUuid) {
        List<Obs> observations = obsDao.getObsForOrder(orderUuid);
        return omrsObsToBahmniObsMapper.map(observations, null);
    }

    private Concept getConceptByName(String conceptName) {
        return conceptService.getConceptByName(conceptName);
    }

    private Collection<Concept> getConceptsByName(List<String> conceptNames) {
        List<Concept> concepts = new ArrayList<>();
        for (String conceptName : conceptNames) {
            concepts.add(getConceptByName(conceptName));
        }
        return concepts;
    }

    private List<Obs> getObsAtTopLevelAndApplyIgnoreList(List<Obs> observations, List<String> topLevelConceptNames, Collection<Concept> obsIgnoreList) {
        List<Obs> topLevelObservations = new ArrayList<>();
        if (topLevelConceptNames == null) topLevelConceptNames = new ArrayList<>();

        Set<String> topLevelConceptNamesWithoutCase = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        topLevelConceptNamesWithoutCase.addAll(topLevelConceptNames);
        for (Obs o : observations) {
            if (o.getObsGroup() == null || topLevelConceptNamesWithoutCase.contains(o.getConcept().getName().getName().toLowerCase())) {
                if (!(removeIgnoredObsOrIgnoreParentItself(o, obsIgnoreList) || topLevelObservations.contains(o))) {
                    topLevelObservations.add(o);
                }
            }
        }

        return topLevelObservations;
    }

    //Removes groupmembers who are ignored, and ignore a parent if all children are in ignore list
    private boolean removeIgnoredObsOrIgnoreParentItself(Obs o, Collection<Concept> obsIgnoreList) {
        if (CollectionUtils.isNotEmpty(o.getGroupMembers()) && CollectionUtils.isNotEmpty(obsIgnoreList)) {
            int size = o.getGroupMembers().size();
            int matchCount = 0;
            Iterator<Obs> itr = o.getGroupMembers().iterator();
            while (itr.hasNext()) {
                Obs temp = itr.next();
                for (Concept concept : obsIgnoreList) {
                    if (temp.getConcept().getConceptId() == concept.getConceptId()) {
                        itr.remove();
                        matchCount++;
                    }
                }
            }
            if (matchCount == size) {
                return true;
            }
        }
        return false;
    }

    private List<Obs> filterByRootConcept(List<Obs> obs, String parentConceptName) {
        List<Obs> filteredList = new ArrayList<>();
        for (Obs ob : obs) {
            if (partOfParent(ob, parentConceptName)) {
                filteredList.add(ob);
            }
        }
        return filteredList;
    }

    private boolean partOfParent(Obs ob, String parentConceptName) {
        if (ob == null) return false;
        if (ob.getConcept().getName().getName().equals(parentConceptName)) return true;
        return partOfParent(ob.getObsGroup(), parentConceptName);
    }


    private List<Obs> withUniqueConcepts(List<Obs> observations) {
        Map<Integer, Integer> conceptToEncounterMap = new HashMap<>();
        List<Obs> filteredObservations = new ArrayList<>();
        for (Obs obs : observations) {
            Integer encounterId = conceptToEncounterMap.get(obs.getConcept().getId());
            if (encounterId == null) {
                conceptToEncounterMap.put(obs.getConcept().getId(), obs.getEncounter().getId());
                filteredObservations.add(obs);
            } else if (obs.getEncounter().getId().intValue() == encounterId.intValue()) {
                filteredObservations.add(obs);
            }
        }
        return filteredObservations;
    }

    private List<Obs> getAllLatestObsForAConcept(List<Obs> observations) {
        Map<Date, List<Obs>> obsToEncounterDateTimeMap = new TreeMap<>(Collections.<Date>reverseOrder());
        for (Obs obs : observations) {
            if (obsToEncounterDateTimeMap.get(obs.getEncounter().getEncounterDatetime()) != null) {
                obsToEncounterDateTimeMap.get(obs.getEncounter().getEncounterDatetime()).add(obs);
            } else {
                List<Obs> obsList = new ArrayList<>();
                obsList.add(obs);
                obsToEncounterDateTimeMap.put(obs.getEncounter().getEncounterDatetime(), obsList);
            }
        }
        if (CollectionUtils.isNotEmpty(obsToEncounterDateTimeMap.entrySet())) {
            return obsToEncounterDateTimeMap.entrySet().iterator().next().getValue();
        } else {
            return null;
        }
    }

}
