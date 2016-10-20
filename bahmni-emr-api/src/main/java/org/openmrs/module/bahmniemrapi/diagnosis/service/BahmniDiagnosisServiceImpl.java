package org.openmrs.module.bahmniemrapi.diagnosis.service;

import org.openmrs.Obs;
import org.openmrs.module.bahmniemrapi.diagnosis.contract.BahmniDiagnosisRequest;
import org.openmrs.module.bahmniemrapi.diagnosis.helper.BahmniDiagnosisMetadata;
import org.openmrs.module.emrapi.diagnosis.DiagnosisServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import static org.openmrs.module.emrapi.encounter.domain.EncounterTransaction.*;

@Service("bahmniDiagnosisService")
public class BahmniDiagnosisServiceImpl extends DiagnosisServiceImpl {

    @Autowired
    BahmniDiagnosisMetadata bahmniDiagnosisMetadata;

    @Override
    public Obs buildDiagnosisObs(Diagnosis etDiagnosis) {
        Obs diagnosisObsGroup = super.buildDiagnosisObs(etDiagnosis);
        bahmniDiagnosisMetadata.addBahmniDiagnosisMetadataToDiagnosisObsGroup(diagnosisObsGroup,
                (BahmniDiagnosisRequest) etDiagnosis);
        return diagnosisObsGroup;
    }
}
