package org.bahmni.module.bahmnicoreui.mapper;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.bahmni.module.bahmnicoreui.contract.ConceptValue;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;

import java.io.IOException;
import java.util.*;

public class DiseaseSummaryDrugOrderMapper extends DiseaseSummaryMapper<List<DrugOrder>> {

    private Logger logger = Logger.getLogger(this.getClass());

    public Map<String, Map<String, ConceptValue>> map(List<DrugOrder> drugOrders, String groupBy)  {
        Map<String, Map<String, ConceptValue>> result = new LinkedHashMap<>();
        for (DrugOrder drugOrder : drugOrders) {
            String startDateTime = (RESULT_TABLE_GROUP_BY_ENCOUNTER.equals(groupBy)) ?
                    DateFormatUtils.format(drugOrder.getEncounter().getEncounterDatetime(), DATE_TIME_FORMAT) : DateFormatUtils.format(drugOrder.getEncounter().getVisit().getStartDatetime(), DATE_FORMAT);
            String conceptName = drugOrder.getDrug().getConcept().getName().getName();
            try {
                addToResultTable(result, startDateTime, conceptName, formattedDrugOrderValue(drugOrder), null, false);
            } catch (IOException e) {
                logger.error("Could not parse dosing instructions",e);
                throw new RuntimeException("Could not parse dosing instructions",e);
            }
        }
        return result;
    }

    private String formattedDrugOrderValue(DrugOrder drugOrder) throws IOException {
        String strength = drugOrder.getDrug().getStrength();
        Concept doseUnitsConcept = drugOrder.getDoseUnits();
        String doseUnit = doseUnitsConcept == null ? "" : " " + doseUnitsConcept.getName().getName();
        String dose = drugOrder.getDose() == null ? "" : drugOrder.getDose() + doseUnit;
        String frequency = getFrequency(drugOrder);
        String asNeeded = drugOrder.getAsNeeded() ? "SOS" : null;
        return concat(",", strength, dose, frequency, asNeeded);
    }

    private String getFrequency(DrugOrder drugOrder) throws IOException {
        if (drugOrder.getFrequency() == null) {
            String dosingInstructions = drugOrder.getDosingInstructions();
            Map<String, Object> instructions = hashMapForJson(dosingInstructions);
            return concat("-", getEmptyIfNull(instructions.get("morningDose")), getEmptyIfNull(instructions.get("afternoonDose")), getEmptyIfNull(instructions.get("eveningDose")));
        }
        return drugOrder.getFrequency().getName();
    }

    private Map<String, Object> hashMapForJson(String dosingInstructions) throws IOException {
        if (dosingInstructions == null || dosingInstructions.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
        TypeReference<HashMap<String, Object>> typeRef
                = new TypeReference<HashMap<String, Object>>() {
        };
        return objectMapper.readValue(dosingInstructions, typeRef);
    }

    private String concat(String separator, String... values) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                stringBuilder.append(separator).append(value);
            }
        }
        return stringBuilder.length() > 1 ? stringBuilder.substring(1) : "";
    }

}