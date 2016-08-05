package org.bahmni.module.bahmnicore.web.v1_0.controller;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.APIException;
import org.openmrs.module.rulesengine.domain.DosageRequest;
import org.openmrs.module.rulesengine.domain.Dose;
import org.openmrs.module.rulesengine.engine.RulesEngine;
import org.openmrs.module.rulesengine.rule.BSABasedDoseRule;
import org.openmrs.module.rulesengine.rule.WeightBasedDoseRule;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/bahmnicore/calculateDose")
public class DoseCalculatorController extends BaseRestController {

    @Autowired
    private RulesEngine rulesEgine;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public Dose calculateDose(@RequestParam(value = "dosageRequest") String dosageRequest) throws Exception {
      //  return new Dose("test",50, Dose.DoseUnit.mg);

        ObjectMapper objectMapper=new ObjectMapper();
        DosageRequest request= objectMapper.readValue(dosageRequest,DosageRequest.class);
        return rulesEgine.calculateDose(request);
    }
}
