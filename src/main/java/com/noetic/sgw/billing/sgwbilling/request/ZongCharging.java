package com.noetic.sgw.billing.sgwbilling.request;

import com.noetic.sgw.billing.sgwbilling.config.StartConfiguration;
import com.noetic.sgw.billing.sgwbilling.entities.GamesBillingRecordEntity;
import com.noetic.sgw.billing.sgwbilling.entities.TodaysChargedMsisdnsEntity;
import com.noetic.sgw.billing.sgwbilling.entities.WeeklyChargedMsisdnsEntity;
import com.noetic.sgw.billing.sgwbilling.repository.GamesBillingRecordsRepository;
import com.noetic.sgw.billing.sgwbilling.repository.TodaysChargedMsisdnsRepository;
import com.noetic.sgw.billing.sgwbilling.repository.WeeklyChargedMsisdnsRepository;
import com.noetic.sgw.billing.sgwbilling.util.ChargeRequestProperties;
import com.noetic.sgw.billing.sgwbilling.util.Response;
import com.noetic.sgw.billing.sgwbilling.util.ResponseTypeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.query.InvalidJpaQueryMethodException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

@Service
public class ZongCharging {

    private static final Logger log = LoggerFactory.getLogger(ZongCharging.class);

    private static String SERVICE_ID_20 = "Noet20";

    public TCPClient client;

    @Autowired
    TodaysChargedMsisdnsRepository chargedMsisdnsRepository;

    @Autowired
    private StartConfiguration startConfiguration;

    @Autowired
    private GamesBillingRecordsRepository gamesBillingRecordsRepository;

    @Autowired
    private WeeklyChargedMsisdnsRepository weeklyChargedMsisdnsRepository;

    private boolean testing = false;

    public Response sendChargingRequest(ChargeRequestProperties request) throws Exception {
        System.out.println("in zongcharge");
        Response res = new Response();
        ZongMMLRequest zongMMLRequest = new ZongMMLRequest();
        String chargingAmount = "";
        String code = null;
        if (!testing) {
            System.out.println("!testing");

            chargingAmount = String.valueOf((int) request.getChargingAmount() * 100);
           // zongMMLRequest.logIn();
            String response = zongMMLRequest.deductBalance(String.valueOf(request.getMsisdn()), chargingAmount, SERVICE_ID_20);
//            log.info("CHARGING | ZONG CHARGING CLASS | ZONG RESPONSE | " + response + " | MSISDN | " + request.getMsisdn());
//            String[] zongRes = response.split("RETN=");
          //  String[] codeArr = new String[2];
//            if (zongRes.length < 2) {
//                codeArr[0] = "2001";
//            } else {
//                codeArr = zongRes[1].split(",");
//            }
           // code = codeArr[0];

            // Need to define codes here.
            // 0000 - Successfully charged
            // 1002 - User doesn't exists
            // 1001 - not enough money
            // Else - other error.

            int random = (int) (Math.random() * 4 + 1);
            System.out.println(random);

            if (random==1) {

                 code = "0000";
                if (code.equalsIgnoreCase("0000")) {
                    res.setCorrelationId(request.getCorrelationId());
                    res.setCode(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL);
                    res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL)));
                    log.info("IN ZONG CHARGING IF 0000 || ZONG RESPONSE FOR || " + request.getMsisdn() + " || CODE || " + code);

                }
            }else if(random==2) {
                code="1001";
                if (code.equalsIgnoreCase("1001")) {
                    res.setCorrelationId(request.getCorrelationId());
                    res.setCode(ResponseTypeConstants.INSUFFICIENT_BALANCE);
                    res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.INSUFFICIENT_BALANCE)));
                    log.info("IN ZONG CHARGING IF 1001 || ZONG RESPONSE FOR || " + request.getMsisdn() + " || CODE || " + code);
                }
            }else if(random==3) {
                code="1002";
                if (code.equalsIgnoreCase("1002")) {
                    res.setCorrelationId(request.getCorrelationId());
                    res.setCode(ResponseTypeConstants.SUBSCRIBER_NOT_FOUND);
                    res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.SUBSCRIBER_NOT_FOUND)));
                    log.info("IN ZONG CHARGING IF 1002 || ZONG RESPONSE FOR || " + request.getMsisdn() + " || CODE || " + code);
                }
            }
                else {
                res.setCorrelationId(request.getCorrelationId());
                res.setCode(ResponseTypeConstants.OTHER_ERROR);
                res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.OTHER_ERROR)));
                log.info("IN ZONG CHARGING IF OTHER || ZONG RESPONSE FOR || " + request.getMsisdn() + " || CODE || " + code);
            }
        } else {
            log.info("BILLING SERVICE || ZONG CHARGING || MOCK REQUEST FOR || " + request.getMsisdn());
            res.setCorrelationId(request.getCorrelationId());
            res.setCode(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL);
            res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL)));
        }
        if (request.isDcb()) {
            res.setCode(Integer.valueOf(code));
        } else {
            saveChargingRecords(res, request);
        }
        return res;
    }

    private void saveChargingRecords(Response res, ChargeRequestProperties req) {
        GamesBillingRecordEntity entity = new GamesBillingRecordEntity();
        entity.setAmount(req.getChargingAmount());
        entity.setCdate(Timestamp.valueOf(LocalDateTime.now()));

        // Handle 4 cases.
        // 1 - Charged and is renewal
        // 2 - Not charged and is renewal.
        // 3 - Charged and user initiated.
        // 4 - Not charged and user initiated.
        if (res.getCode() == ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL) {
            entity.setIsCharged(1);
            entity.setNoAttemptsMonthly(0);

            if (req.getIsRenewal() == 1) {
                entity.setIsRenewal(1);

                // No. of daily attempts in case of Renewal shouldn't be set to 0. Rather store the attempt on which user got charged.
                entity.setNoOfDailyAttempts(req.getDailyAttempts());
                log.info("Zong charging | Charged and renewal case || " + req.getMsisdn());
            } else {
                entity.setIsRenewal(0);

                // If new user, set daily attempts to 0.
                entity.setNoOfDailyAttempts(0);
                log.info("Zong charging | Charged and user initiated case || " + req.getMsisdn());
            }
        } else {
            entity.setIsCharged(0);

            if (req.getIsRenewal() == 1) {
                entity.setNoOfDailyAttempts(req.getDailyAttempts());
                entity.setNoAttemptsMonthly(req.getAttempts());
                entity.setIsRenewal(1);
                log.info("Zong charging | Not charged and renewal case || " + req.getMsisdn());
            } else {
                entity.setNoAttemptsMonthly(1);
                entity.setNoOfDailyAttempts(1);
                entity.setIsRenewal(0);
                log.info("Zong charging | Not charged and user initiated case || " + req.getMsisdn());
            }
        }

        entity.setIsPostpaid(0);
        entity.setOparatorId(req.getOperatorId().shortValue());
        entity.setShareAmount(req.getShareAmount());
        entity.setShareAmount(req.getShareAmount());
        entity.setMsisdn(req.getMsisdn());
        entity.setChargingMechanism(req.getOperatorId().shortValue());
        entity.setTaxAmount(req.getTaxAmount());
        entity.setVendorPlanId(req.getVendorPlanId().longValue());
        entity.setSubCycleId((short) req.getSubCycleId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String transactionId = Base64.getEncoder().encodeToString((LocalDateTime.now().format(formatter) + UUID.randomUUID().toString()).getBytes());
        entity.setTransactionId(transactionId);
        try {
            gamesBillingRecordsRepository.save(entity);
            log.info("CHARGING | ZONGCHARGING CLASS | RECORDS INSERTED FOR MSISDN " + req.getMsisdn());
        } catch (InvalidJpaQueryMethodException e) {
            log.info("CHARGING | ZONGCHARGING CLASS | EXCEPTION CAUGHT WHILE INSERTING RECORDS " + e.getCause());
        }

        log.info("BILLING SERVICE || ZONG CHARGING || UPDATING TODAY'S CHARGED TABLE : " + req.getMsisdn());
        updateTodaysChargedTable(res, req);
    }

    /*private void updateWeeklyChargedTable(Response res, ChargeRequestProperties req) {
        WeeklyChargedMsisdnsEntity chargedMsisdnsEntity = weeklyChargedMsisdnsRepository.findTopByMsisdn(req.getMsisdn());
        WeeklyChargedMsisdnsEntity todaysChargedMsisdnsEntity = new WeeklyChargedMsisdnsEntity();
        if (chargedMsisdnsEntity == null) {
            todaysChargedMsisdnsEntity.setCdate(Timestamp.valueOf(LocalDateTime.now()));
            if (res.getCode() == ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL) {
                log.info("BILLING SERVICE || ZONG CHARGING || " + req.getMsisdn() + " | RENEWED SUCCESSFULLY");
                todaysChargedMsisdnsEntity.setIsCharged(1);
            } else {
                log.info("BILLING SERVICE || ZONG CHARGING || " + req.getMsisdn() + " | NOT RENEWED SUCCESSFULLY");
                todaysChargedMsisdnsEntity.setIsCharged(0);
            }
            todaysChargedMsisdnsEntity.setNumberOfTries(1);
            todaysChargedMsisdnsEntity.setExpirydatetime(Timestamp.valueOf(LocalDateTime.now().plusDays(1)));
            todaysChargedMsisdnsEntity.setSubCycle(req.getSubCycleId());
            todaysChargedMsisdnsEntity.setMsisdn(req.getMsisdn());
            todaysChargedMsisdnsEntity.setVendorPlanId(req.getVendorPlanId().longValue());
            todaysChargedMsisdnsEntity.setOperatorId(req.getOperatorId());
            weeklyChargedMsisdnsRepository.save(todaysChargedMsisdnsEntity);
        } else {
            chargedMsisdnsEntity.setCdate(Timestamp.valueOf(LocalDateTime.now()));
            chargedMsisdnsEntity.setNumberOfTries(chargedMsisdnsEntity.getNumberOfTries() + 1);
            if (res.getCode() == ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL) {
                chargedMsisdnsEntity.setIsCharged(1);
            } else {
                chargedMsisdnsEntity.setIsCharged(0);
            }
            weeklyChargedMsisdnsRepository.save(chargedMsisdnsEntity);
        }

    }*/

    private void updateTodaysChargedTable(Response res, ChargeRequestProperties req) {
        TodaysChargedMsisdnsEntity chargedMsisdnsEntity = chargedMsisdnsRepository.findTopByMsisdn(req.getMsisdn());
        TodaysChargedMsisdnsEntity todaysChargedMsisdnsEntity = new TodaysChargedMsisdnsEntity();
        if (chargedMsisdnsEntity == null) {
            todaysChargedMsisdnsEntity.setCdate(Timestamp.valueOf(LocalDateTime.now()));
            if (res.getCode() == ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL) {
                log.info("BILLING SERVICE || ZONG CHARGING || " + req.getMsisdn() + " | RENEWED SUCCESSFULLY");
                todaysChargedMsisdnsEntity.setIsCharged(1);
            } else {
                log.info("BILLING SERVICE || ZONG CHARGING || " + req.getMsisdn() + " | NOT RENEWED SUCCESSFULLY");
                todaysChargedMsisdnsEntity.setIsCharged(0);
            }
            todaysChargedMsisdnsEntity.setNumberOfTries(1);
            todaysChargedMsisdnsEntity.setExpirydatetime(Timestamp.valueOf(LocalDateTime.now().plusDays(1)));
            todaysChargedMsisdnsEntity.setSubCycle(req.getSubCycleId());
            todaysChargedMsisdnsEntity.setMsisdn(req.getMsisdn());
            todaysChargedMsisdnsEntity.setVendorPlanId(req.getVendorPlanId().longValue());
            todaysChargedMsisdnsEntity.setOperatorId(req.getOperatorId());
            chargedMsisdnsRepository.save(todaysChargedMsisdnsEntity);
        } else {
            chargedMsisdnsEntity.setCdate(Timestamp.valueOf(LocalDateTime.now()));
            chargedMsisdnsEntity.setNumberOfTries(chargedMsisdnsEntity.getNumberOfTries() + 1);
            if (res.getCode() == ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL) {
                chargedMsisdnsEntity.setIsCharged(1);
            } else {
                chargedMsisdnsEntity.setIsCharged(0);
            }
            chargedMsisdnsRepository.save(chargedMsisdnsEntity);
        }

    }

    /*public void processFailed() {

        List<ChargeRequestProperties> failedRequests = new ArrayList<>();

        while (true) {
            if (!failedRequests.isEmpty()) {
                int size = failedRequests.size();
                log.info("Failed List Size " + size);
                ChargeRequestProperties requestProperties = failedRequests.remove(--size);
                try {
                    sendChargingRequest(requestProperties);
                } catch (Exception e) {
                }
            }
        }
    }*/
}
