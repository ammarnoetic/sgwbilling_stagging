package com.noetic.sgw.billing.sgwbilling.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noetic.sgw.billing.sgwbilling.config.StartConfiguration;
import com.noetic.sgw.billing.sgwbilling.entities.MoBilledRecordsEntity;
import com.noetic.sgw.billing.sgwbilling.repository.GamesBillingRecordsRepository;
import com.noetic.sgw.billing.sgwbilling.repository.MoBilledRecordsRepository;
import com.noetic.sgw.billing.sgwbilling.repository.WeeklyChargedMsisdnsRepository;
import com.noetic.sgw.billing.sgwbilling.util.*;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ZongMoCharging {

    private static final Logger log = LoggerFactory.getLogger(ZongCharging.class);

    private String serviceId = "";
    private String[] zongRes;
    @Autowired
    RequestSender requestSender;
    private ZongMMLRequest zongMMLRequest = new ZongMMLRequest();
    private MoResponse res = new MoResponse();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private boolean testing = false;
    @Autowired
    private StartConfiguration startConfiguration;
    @Autowired
    private MoBilledRecordsRepository moBilledRecordsRepository;

    public MoResponse sendChargingRequest(MoRequestProperties request) throws Exception {
        String charginAmount = "";

        Integer shortcode = Integer.valueOf(request.getShortcode());
        if (shortcode == 3441) {
            serviceId = "Noet01";
        } else if (shortcode == 3443) {
            serviceId = "Noet05";
        } else if (shortcode == 3444) {
            serviceId = "Noet10";
        } else if (shortcode == 3445) {
            serviceId = "Noet25";
        }
        if(!testing) {
			//old code
        	/*
			 * zongMMLRequest.logIn(); charginAmount = String.valueOf((int)
			 * request.getChargingAmount() * 100); String response =
			 * zongMMLRequest.deductBalance(String.valueOf(request.getMsisdn()),
			 * charginAmount, serviceId);
			 * 
			 * log.info("CHARGING | ZONGCHARGING CLASS | ZONG RESPONSE | " + response);
			 * zongRes = response.split("RETN=");
			 */
        		//added by habib 
      
//        	OkHttpClient client = new OkHttpClient();
//        			MediaType mediaType = MediaType.parse("application/json");
//        			RequestBody body = RequestBody.create(mediaType, "{\n    \"msisdn\":"+request.getMsisdn()+",\n    \"amount\":"+request.getChargingAmount()+",\n    \"operatorId\":"+request.getOperatorId()+",\n    \"transactionid\":"+request.getCorrelationId()+"\n}");
//        			Request requests = new Request.Builder()
//        			  .url("http://192.168.127.57:9080/chargezong")
//        			  .method("POST", body)
//        			  .addHeader("Content-Type", "application/json")
//        			  .build();
//        			com.squareup.okhttp.Response responses = client.newCall(requests).execute();
//        			  String resStr = responses.body().string();
        			  
        			  
//        			  String url = "http://192.168.127.57:9080/chargezong";
            String url = "http://localhost/chargezong";

        			// create an instance of RestTemplate
        			RestTemplate restTemplate = new RestTemplate();

        			// create headers
        			HttpHeaders headers = new HttpHeaders();
        			// set `content-type` header
        			headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        			// request body parameters
        			Map<String, Object> map = new HashMap<>();
        			map.put("msisdn", request.getMsisdn());
        			map.put("amount", request.getChargingAmount());
        			map.put("operatorId", request.getOperatorId());
        			map.put("transactionid", request.getCorrelationId());

        			// build the request
        			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);

        			// send POST request
        			ResponseEntity<MoResponse> response = restTemplate.postForEntity(url, entity, MoResponse.class);

        			// check response
        			if (response.getStatusCode() == HttpStatus.OK) {
        			    System.out.println("Request Successful");
        			    System.out.println(response.getBody().getCode());
        			} else {
        			    System.out.println("Request Failed");
        			    System.out.println(response.getBody().getCode());
        			    System.out.println(response.getStatusCode());
        			}

        			  
              //  String[] codeArr = zongRes[1].split(",");
               // String code = codeArr[0];
        			String code=Integer.toString(response.getBody().getCode());
                log.info("CHARGING | ZONGCHARGING CLASS | ZONG MML RESPONSE CODE | " + code);

                if (code.equalsIgnoreCase("0")) {
                    res.setCorrelationId(request.getCorrelationId());
                    res.setCode(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL);
                    res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL)));
                } else {
                    res.setCorrelationId(request.getCorrelationId());
                    res.setCode(ResponseTypeConstants.INSUFFICIENT_BALANCE);
                    res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.INSUFFICIENT_BALANCE)));
                }
        }else {
            Thread.sleep(100l);
            log.info("BILLING SERVICE || ZONG CHARGING || MOCK REQUEST FOR || "+request.getMsisdn());
            res.setCorrelationId(request.getCorrelationId());
            res.setCode(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL);
            res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL)));
        }
        saveChargingRecords(res,request,zongRes[1]);
        return res;
    }

    private void saveChargingRecords(MoResponse moResponse, MoRequestProperties requestProperties, String transactionId){

        MoBilledRecordsEntity moBilledRecordsEntity = new MoBilledRecordsEntity();
        moBilledRecordsEntity.setMsisdn(requestProperties.getMsisdn());
        moBilledRecordsEntity.setOperatorId((int) requestProperties.getOperatorId());
        moBilledRecordsEntity.setChargingMechanism(requestProperties.getChargingMechanism());
        moBilledRecordsEntity.setOriginalSmsId(333l);
        moBilledRecordsEntity.setPartnerPlanId((int) requestProperties.getPartnerPlanId());
        moBilledRecordsEntity.setChargedAmount(requestProperties.getChargingAmount());
        moBilledRecordsEntity.setTaxAmount(requestProperties.getTaxAmount());
        moBilledRecordsEntity.setChargeTime(Timestamp.valueOf(LocalDateTime.now()));
        moBilledRecordsEntity.setTransactionId(transactionId);
        if(moResponse.getCode() == ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL){
            moBilledRecordsEntity.setIsCharged(1);
            moBilledRecordsEntity.setChargingResponse("CHARGED SUCCESSFULLY");
        }else if(moResponse.getCode() == ResponseTypeConstants.INSUFFICIENT_BALANCE){
            moBilledRecordsEntity.setIsCharged(0);
            moBilledRecordsEntity.setChargingResponse("INSUFFICIENT BALANCE");
        }else if(moResponse.getCode() == ResponseTypeConstants.SUBSCRIBER_NOT_FOUND){
            moBilledRecordsEntity.setIsCharged(0);
            moBilledRecordsEntity.setChargingResponse("SUBSCRIBER NOT FOUNT");
        }else {
            moBilledRecordsEntity.setIsCharged(0);
            moBilledRecordsEntity.setChargingResponse("OTHER ERROR");
        }
        try {

            ObjectMapper objectMapper = new ObjectMapper();
            Object object=null;
            try {
                object = objectMapper.writeValueAsString(moBilledRecordsEntity);
            } catch (JsonProcessingException e) {
            	log.error("Error in JSON processing Converting To String"+e);
            };
            log.info("Sending TO QUeue For Saving Records IN Charging Records");
            requestSender.send("sgwbillingsavechargingZongmml",object);

       // gamesBillingRecordsRepository.save(entity);
            log.info("BILLING SERVICE || Zong CHARGING || RECORDS Send To Queue FOR MSISDN "+requestProperties.getMsisdn());
    } catch (Exception e) {
    	log.info("BILLING SERVICE || Zong CHARGING || EXCEPTION CAUGHT WHILE INSERTING RECORDS "+e.getCause());
    }
        moBilledRecordsRepository.save(moBilledRecordsEntity);

    }
}
