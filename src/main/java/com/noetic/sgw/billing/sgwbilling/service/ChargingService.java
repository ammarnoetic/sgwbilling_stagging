package com.noetic.sgw.billing.sgwbilling.service;

import com.noetic.sgw.billing.sgwbilling.config.StartConfiguration;
import com.noetic.sgw.billing.sgwbilling.request.*;
import com.noetic.sgw.billing.sgwbilling.util.ChargeRequestProperties;
import com.noetic.sgw.billing.sgwbilling.util.MoRequestProperties;
import com.noetic.sgw.billing.sgwbilling.util.MoResponse;
import com.noetic.sgw.billing.sgwbilling.util.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChargingService {

    Logger logger = LoggerFactory.getLogger(ChargingService.class);

    @Autowired
    StartConfiguration startConfiguration;
    @Autowired
    UcipRequest ucipRequest;
    @Autowired
    ZongCharging zongCharging;
    @Autowired
    ZongMoCharging zongMoCharging;
    @Autowired
    TelenorCharging telenorCharging;
    @Autowired
    JazzCharging jazzCharging;

    public Response processRequest(ChargeRequestProperties req) throws Exception {
        System.out.println("inprocess request");
        long operatorId = req.getOperatorId();
        System.out.println("opid");




        if (startConfiguration.getTelenor() == operatorId) {
            System.out.println("checking tel");
            return telenorCharging.chargeRequest(req);
        }

        else if (startConfiguration.getJazz() == operatorId) {
            System.out.println("checking jazz");
//            return  null;
            return jazzCharging.jazzChargeRequest(req);
        }
        else if (startConfiguration.getZong() == operatorId) {
            System.out.println("checking zong");
            return zongCharging.sendChargingRequest(req);
        }
        else
            return null;
    }

    public MoResponse processMoChargeRequest(MoRequestProperties req) throws Exception {
        if (req.getOperatorId() == startConfiguration.getJazz() || req.getOperatorId() == startConfiguration.getWarid())
            return processJazz(req);
        else if (req.getOperatorId() == startConfiguration.getZong())
            return zongMoCharging.sendChargingRequest(req);
        else
            return null;
    }

    private MoResponse processJazz(MoRequestProperties req) throws InterruptedException {
        return ucipRequest.ucipRequest(req);
    }
}