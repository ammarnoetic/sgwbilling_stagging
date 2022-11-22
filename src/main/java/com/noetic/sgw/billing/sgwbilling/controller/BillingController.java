package com.noetic.sgw.billing.sgwbilling.controller;

import com.noetic.sgw.billing.sgwbilling.service.ChargingService;
import com.noetic.sgw.billing.sgwbilling.util.ChargeRequestProperties;
import com.noetic.sgw.billing.sgwbilling.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@RequestMapping("/charge")
public class BillingController {

    @Autowired
    private ChargingService chargingService;

    @PostMapping("/charge")
    public Response chargeRequest(@RequestBody ChargeRequestProperties req) throws Exception {
        return chargingService.processRequest(req);
    }

    @PostMapping("/renewal")
    public Response renewalChargeRequest(@RequestBody ChargeRequestProperties req) throws Exception {
        return chargingService.processRequest(req);
    }
}
