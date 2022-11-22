package com.noetic.sgw.billing.sgwbilling.request;


import com.noetic.sgw.billing.sgwbilling.config.StartConfiguration;
import com.noetic.sgw.billing.sgwbilling.entities.GamesBillingRecordEntity;
import com.noetic.sgw.billing.sgwbilling.entities.TodaysChargedMsisdnsEntity;
import com.noetic.sgw.billing.sgwbilling.repository.GamesBillingRecordsRepository;
import com.noetic.sgw.billing.sgwbilling.repository.TodaysChargedMsisdnsRepository;
import com.noetic.sgw.billing.sgwbilling.util.ChargeRequestProperties;
import com.noetic.sgw.billing.sgwbilling.util.Response;
import com.noetic.sgw.billing.sgwbilling.util.ResponseTypeConstants;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class JazzCharging {

    Logger logger = LoggerFactory.getLogger(JazzCharging.class);

    @Autowired
    GamesBillingRecordsRepository gamesBillingRecordsRepository;
    @Autowired
    StartConfiguration startConfiguration;
    @Autowired
    TodaysChargedMsisdnsRepository chargedMsisdnsRepository;
    @Autowired
    Environment env;

    private boolean notTesting = true;
    private String methodName = "UpdateBalanceAndDate";
    private String transactionCurrency = "PKR";
    private String originNodeType = "EXT";
    private String originHostName = "GNcasual";
    private String transactionType = "GNcasual";
    private String transactionCode = "GNcasual";
    private String externalData1 = "GNcasual_VAS";
    private String externalData2 = "GNcasual_VAS";



    public Response jazzChargeRequest(ChargeRequestProperties request) throws InterruptedException, ExecutionException,
             IOException {

        System.out.println("in jazzcharge");
        // New Variables whose scope is changed from class/instance to local.
        String[] recArray = new String[2];
        int responseCode = -1;
        String status = "Fail";
        HttpResponse<String> httpResponse = null;
        String originTimeStamp = "";
        String transID = "";
        Response res = new Response();

        if (!isHardCodedMsisdn(request.getMsisdn())) {
            System.out.println("isHardCodedMsisdn");

            if (request.getIsRenewal() == 0) {
                if (checkPostPaid(String.valueOf(request.getMsisdn()))) {
                    System.out.println("checkPostPaid");
                    Response response = new Response();
                    response.setCode(ResponseTypeConstants.IS_POSTPAID);
                    response.setMsg(ResponseTypeConstants.IS_POSTPAID_MSG);
                    response.setCorrelationId(request.getCorrelationId());
                    saveChargingRecords(response, request, "postPaid-transId");
                    return response;
                }
            }
            Date date = new Date(System.currentTimeMillis());
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
            TimeZone PKT = TimeZone.getTimeZone("Asia/Karachi");
            dateFormat.setTimeZone(PKT);
//            this.originTimeStamp = dateFormat.format(date);
            originTimeStamp = dateFormat.format(date);
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String transactionID = new Random().nextInt(9999 - 1000) + now.format(formatter);
            String chargeAmount = "";
            String subscriberNumber = "";
            boolean isAlreadyCharged = false;
            if (Long.toString(request.getMsisdn()).startsWith("92")) {
                subscriberNumber = Long.toString(request.getMsisdn()).substring(2);
            } else if (Long.toString(request.getMsisdn()).startsWith("0")) {
                subscriberNumber = Long.toString(request.getMsisdn()).substring(1);
            } else if (Long.toString(request.getMsisdn()).startsWith("920")) {
                subscriberNumber = Long.toString(request.getMsisdn()).substring(3);
            } else {
                subscriberNumber = Long.toString(request.getMsisdn());
            }
            DecimalFormat decimalFormatter1 = new DecimalFormat("0.##");
            DecimalFormat decimalFormatter = new DecimalFormat("#");
            Double adjustmentAmount = Double.valueOf(decimalFormatter.format(request.getChargingAmount())) + Double.valueOf(decimalFormatter1.format(request.getTaxAmount()));
            chargeAmount = decimalFormatter.format(adjustmentAmount * 100);
            System.out.println("chsrging amount");
            logger.info("BILLING SERVICE || JAZZ CHARGING || TOTAL AMOUNT || " + chargeAmount);
//            String inputXML = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n" +
//                    "<methodCall>\n" +
//                    "<methodName>" + this.methodName + "</methodName>\n" +
//                    "<params>\n" +
//                    "<param>\n" +
//                    "<value>\n" +
//                    "<struct>\n" +
//                    "<member>\n" +
//                    "<name>originNodeType</name>\n" +
//                    "<value><string>" + this.originNodeType + "</string></value>\n" +
//                    "</member>\n" +
//                    "<member>\n" +
//                    "<name>originHostName</name>\n" +
//                    "<value><string>" + this.originHostName + "</string></value>\n" +
//                    "</member>\n" +
//                    "<member>\n" +
//                    "<name>originTransactionID</name>\n" +
//                    "<value><string>" + transactionID + "</string></value>\n" +
//                    "</member>\n" +
//                    "<member>\n" +
//                    "<name>transactionType</name>\n" +
//                    "<value><string>" + this.transactionType + "</string></value>\n" +
//                    "</member>\n" +
//                    "<member>\n" +
//                    "<name>transactionCode</name>\n" +
//                    "<value><string>" + this.transactionCode + "</string></value>\n" +
//                    "</member>\n" +
//                    "<member>\n" +
//                    "<name>externalData1</name>\n" +
//                    "<value><string>" + this.externalData1 + "</string></value>\n" +
//                    "</member>\n" +
//                    "<member>\n" +
//                    "<name>externalData2</name>\n" +
//                    "<value><string>" + this.externalData2 + "</string></value>\n" +
//                    "</member>\n" +
//                    "<member>\n" +
//                    "<name>originTimeStamp</name>\n" +
//                    "<value><dateTime.iso8601>" + originTimeStamp + "+0500</dateTime.iso8601></value>\n" +
//                    "</member>\n" +
//                    "<member>\n" +
//                    "<name>transactionCurrency</name>\n" +
//                    "<value><string>" + this.transactionCurrency + "</string></value>\n" +
//                    "</member>\n" +
//                    "<member>\n" +
//                    "<name>subscriberNumber</name>\n" +
//                    "<value><string>" + subscriberNumber + "</string></value>\n" +
//                    "</member>\n" +
//                    "<member>\n" +
//                    "<name>adjustmentAmountRelative</name>\n" +
//                    "<value><string>-" + chargeAmount + "</string></value>\n" +
//                    "</member>\n" +
//                    "</struct>\n" +
//                    "</value>\n" +
//                    "</param>\n" +
//                    "</params>\n" +
//                    "</methodCall>";

            if (!isAlreadyCharged) {
                try {
                    System.out.println("!isAlreadyCharged");

//                    // Changing host ip from 10.13.32.156 to 10.13.32.179 - Dated: 2021-08-11 -
//                    httpResponse = Unirest.post(env.getProperty("jazz.api"))
//                            .header("Authorization", env.getProperty("jazz.api.authorization"))
//                            .header("Content-Type", "text/xml")
//                            .header("User-Agent", "UGw Server/4.3/1.0")
//                            .header("Cache-Control", "no-cache")
//                            .header("Pragma", "no-cache")
//                            .header("Host", "10.13.32.179:10010")
//                            .header("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2")
//                            .header("Connection", "keep-alive")
//                            .body(inputXML).asString();
//
//                    recArray = xmlConversion(httpResponse.getBody());
//                    System.out.println(httpResponse.getStatus());
                }
                catch (UnirestException e) {
                    logger.info("Response +" + httpResponse);
                    logger.error("Error while sending request " + e.getStackTrace());
                }
                transID = recArray[0];

                if (recArray[1] != null) {
                    responseCode = Integer.valueOf(recArray[1]);
                    logger.info("JAZZ CHARGING || JAZZ RESPONSE FOR || " + request.getMsisdn() + " || Array val " + Integer.valueOf(recArray[1]) + " || rc " + responseCode + " || LN:199");
                }


                 responseCode = (int) (Math.random() * 4 + 1);
                System.out.println(responseCode);


//                if (httpResponse == null) {
//                    System.out.println("httpResponse == null");
//                    res.setCorrelationId(request.getCorrelationId());
//                    res.setCode(Integer.parseInt(ResponseTypeConstants.REMOTE_SERVER_CONNECTION_ERROR));
//                    res.setMsg(startConfiguration.getResultStatusDescription(ResponseTypeConstants.REMOTE_SERVER_CONNECTION_ERROR));
//                    logger.info(String.format("RESPONSE CODE FORBIDDEN-%s", subscriberNumber));
//                }
                 if (responseCode == 1) {
                    System.out.println("responseCode == 0");
                    res.setCorrelationId(request.getCorrelationId());
                    res.setCode(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL);
                    res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL)));
                    logger.info("IN JAZZ CHARGING IF 0 || JAZZ RESPONSE FOR || " + request.getMsisdn() + " || " + responseCode + " || LN:211");
                } else if (responseCode == 2) {
                    System.out.println("responseCode == 102");
                    res.setCorrelationId(request.getCorrelationId());
                    res.setCode(ResponseTypeConstants.SUBSCRIBER_NOT_FOUND);
                    res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.SUBSCRIBER_NOT_FOUND)));
                    logger.info("IN JAZZ CHARGING IF 102 || JAZZ RESPONSE FOR || " + request.getMsisdn() + " || " + responseCode + " || LN:216");
                } else if (responseCode == 3) {
                    System.out.println("responseCode == 124");
                    res.setCorrelationId(request.getCorrelationId());
                    res.setCode(ResponseTypeConstants.INSUFFICIENT_BALANCE);
                    res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.INSUFFICIENT_BALANCE)));
                    logger.info("IN JAZZ CHARGING IF 124 || JAZZ RESPONSE FOR || " + request.getMsisdn() + " || " + responseCode + " || LN:221");
                } else {
                    res.setCorrelationId(request.getCorrelationId());
                    res.setCode(ResponseTypeConstants.OTHER_ERROR);
                    res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.OTHER_ERROR)));
                    logger.info("IN JAZZ CHARGING ELSE BLOCK || JAZZ RESPONSE FOR || " + request.getMsisdn() + " || " + responseCode + " || LN:226");
                }
                if (status != null)
                    logger.info("JAZZ CHARGING ||  JAZZ RESPONSE FOR || " + request.getMsisdn() + " RESPONSE MESSAGE || " + res.getMsg() + " || LN:229");
            } else {
                res.setCorrelationId(request.getCorrelationId());
                res.setCode(ResponseTypeConstants.ALREADY_CHARGED);
                res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.ALREADY_CHARGED)));
                logger.info("JAZZ CHARGING ||  JAZZ RESPONSE FOR || " + request.getMsisdn() + " ALREADY CHARGED");
            }
        } else {
            logger.info("BILLING SERVICE || JAZZ CHARGING || HARDCODED MSISDN REQUEST || " + request.getMsisdn());
//            responseCode = 0;
//            if (!request.isDcb()) {
//                saveChargingRecords(res, request, transID);
//            }
            res.setCorrelationId(request.getCorrelationId());
            res.setCode(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL);
            res.setMsg(startConfiguration.getResultStatusDescription(Integer.toString(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL)));
        }
        if (request.isDcb()) {
            res.setCode(responseCode);
        } else {
            saveChargingRecords(res, request, transID);
        }
        return res;

    }

    /**
     * @param res
     * @param req
     * @param transactionId
     * @desc save Charging Records for Games
     */
    private void saveChargingRecords(Response res, ChargeRequestProperties req, String transactionId) {
        GamesBillingRecordEntity entity = new GamesBillingRecordEntity();
        entity.setAmount(req.getChargingAmount());
        entity.setCdate(Timestamp.valueOf(LocalDateTime.now()));

        logger.info("JAZZ CHARGING || MSISDN || " + req.getMsisdn() + " IN saveChargingRecords() || " + res.getCode() + " || LN:265");

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
                logger.info("Charged and renewal case || " + req.getMsisdn());
            } else {
                entity.setIsRenewal(0);

                // If new user, set daily attempts to 0.
                entity.setNoOfDailyAttempts(0);
                logger.info("Charged and user initiated case || " + req.getMsisdn());
            }

        } else {
            entity.setIsCharged(0);

            if (req.getIsRenewal() == 1) {
                entity.setNoOfDailyAttempts(req.getDailyAttempts());
                entity.setNoAttemptsMonthly(req.getAttempts());
                entity.setIsRenewal(1);
                logger.info("Not charged and renewal case || " + req.getMsisdn());
            } else {
                entity.setNoAttemptsMonthly(1);
                entity.setNoOfDailyAttempts(1);
                entity.setIsRenewal(0);
                logger.info("Not charged and user initiated case || " + req.getMsisdn());
            }
        }

        if (res.getCode() == ResponseTypeConstants.IS_POSTPAID) {
            entity.setIsPostpaid(1);
        } else {
            entity.setIsPostpaid(0);
        }



        entity.setOparatorId(req.getOperatorId().shortValue());
        entity.setShareAmount(req.getShareAmount());
        entity.setShareAmount(req.getShareAmount());
        entity.setMsisdn(req.getMsisdn());
        entity.setChargingMechanism(req.getOperatorId().shortValue());
        entity.setTaxAmount(req.getTaxAmount());
        entity.setVendorPlanId(req.getVendorPlanId().longValue());
        entity.setSubCycleId((short) req.getSubCycleId());
        entity.setTransactionId(transactionId);

        // Custom logic for postpaid numbers
        // This is added for invoicing purposes.
        if (isHardCodedMsisdn(req.getMsisdn())) {
            entity.setIsPostpaid(1);
            entity.setTransactionId("postPaid-transId");
        }

        /*try {

                ObjectMapper objectMapper = new ObjectMapper();
                Object object=null;
                try {
                    object = objectMapper.writeValueAsString(entity);
                } catch (JsonProcessingException e) {
                    logger.error("Error in JSON processing Converting To String"+e);
                };
                logger.info("Sending TO QUeue For Saving Records IN Charging Records");
                requestSender.send("sgwbillingsavecharging",object);

           // gamesBillingRecordsRepository.save(entity);
            logger.info("BILLING SERVICE || JAZZ CHARGING || RECORDS INSERTED FOR MSISDN "+req.getMsisdn());
        } catch (Exception e) {
            logger.info("BILLING SERVICE || JAZZ CHARGING || EXCEPTION CAUGHT WHILE INSERTING RECORDS "+e.getCause());
        }*/

        logger.info("BILLING SERVICE || JAZZ CHARGING || MSISDN REECORD INSERTED" + req.getMsisdn() + " || response code" + entity.getIsCharged());
        gamesBillingRecordsRepository.save(entity);

//        Commenting this to add the charged user to the Today's charged table as well.
//        And check if double charged or not.
       /* if(req.getIsRenewal()==1){
            logger.info("BILLING SERVICE || JAZZ CHARGING || UPDATING TODAYS CHARGED TABLE");
            updateTodaysChargedTable(res,req);
        }*/

        logger.info("BILLING SERVICE || JAZZ CHARGING || UPDATING TODAY'S CHARGED TABLE : " + req.getMsisdn());
        updateTodaysChargedTable(res, req);
    }

    /**
     * @param msisdn
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws JSONException
     * @throws IOException
     * @desc check if number is post-Paid
     */
    public boolean checkPostPaid(String msisdn) throws InterruptedException, ExecutionException, IOException {
        System.out.println("checking postpaid");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDateTime now = LocalDateTime.now();
        String transactionID = new Random().nextInt(9999 - 1000) + now.format(formatter);
        PostPaidOrPrePaidCheckService postPaidOrPrePaidCheckService = new PostPaidOrPrePaidCheckService(msisdn, transactionID);
        return postPaidOrPrePaidCheckService.isPostPaid();
    }

    /**
     * @param res
     * @param req
     * @desc update todays Charged Table
     */
    private void updateTodaysChargedTable(Response res, ChargeRequestProperties req) {
        TodaysChargedMsisdnsEntity chargedMsisdnsEntity = chargedMsisdnsRepository.findTopByMsisdn(req.getMsisdn());

        if (chargedMsisdnsEntity == null) {
            TodaysChargedMsisdnsEntity todaysChargedMsisdnsEntity = new TodaysChargedMsisdnsEntity();
            todaysChargedMsisdnsEntity.setCdate(Timestamp.valueOf(LocalDateTime.now()));
            if (res.getCode() == ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL) {
                logger.info("BILLING SERVICE || JAZZ CHARGING || " + req.getMsisdn() + " | RENEWED SUCCESSFULLY");
                todaysChargedMsisdnsEntity.setIsCharged(1);
            } else {
                logger.info("BILLING SERVICE || JAZZ CHARGING || " + req.getMsisdn() + " | NOT RENEWED SUCCESSFULLY");
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

    /**
     * @param xml
     * @return
     * @desc parse XML and get response
     */
    protected String[] xmlConversion(String xml) {
        String[] retArray = new String[2];
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            InputSource src = new InputSource();
            src.setCharacterStream(new StringReader(xml));

            Document doc = docBuilder.parse(src);

            // normalize text representation
            doc.getDocumentElement().normalize();

            NodeList listOfPersons = doc.getElementsByTagName("member");

            System.out.println(listOfPersons.getLength());

            if (listOfPersons.getLength() == 2) {

                Node firstPersonNode11 = listOfPersons.item(0);

                if (firstPersonNode11.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) firstPersonNode11;

                    retArray[0] = eElement.getElementsByTagName("value").item(0).getTextContent();
                }    //end of if clause

                // Return Response Code
                Node firstPersonNode22 = listOfPersons.item(1);
                if (firstPersonNode22.getNodeType() == Node.ELEMENT_NODE) {

                    Element firstPersonElement22 = (Element) firstPersonNode22;

                    retArray[1] = firstPersonElement22.getElementsByTagName("value").item(0).getTextContent();
                } //end of if clause

            } else {
                //Return Transaction ID
                Node firstPersonNode = listOfPersons.item(16);
                if (firstPersonNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element firstPersonElement = (Element) firstPersonNode;

                    //-------
                    NodeList lastNameList = firstPersonElement.getElementsByTagName("string");
                    Element lastNameElement = (Element) lastNameList.item(0);

                    NodeList textLNList = lastNameElement.getChildNodes();
                    System.out.println("Para 1 Value : " + ((Node) textLNList.item(0)).getNodeValue().trim());
                    retArray[0] = ((Node) textLNList.item(0)).getNodeValue().trim();

                } //End Transaction IF
                //Return Response Code
                Node firstPersonNode1 = listOfPersons.item(17);
                if (firstPersonNode1.getNodeType() == Node.ELEMENT_NODE) {

                    Element firstPersonElement1 = (Element) firstPersonNode1;

                    NodeList lastNameList = firstPersonElement1.getElementsByTagName("i4");
                    Element lastNameElement = (Element) lastNameList.item(0);

                    NodeList textLNList = lastNameElement.getChildNodes();
                    retArray[1] = ((Node) textLNList.item(0)).getNodeValue().trim();

                } //End Response Code IF
            }
        } catch (SAXParseException err) {
            logger.info("BILLING SERVICE || JAZZ CHARGING || ** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
            logger.info("BILLING SERVICE || JAZZ CHARGING || SAXEXCEPTION | " + err.getMessage());

        } catch (SAXException e) {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return retArray;
    }

    public boolean isHardCodedMsisdn(long msisdn) {
        // List of hard coded MSISDNS - Business requirement
        // Add logs
        Long[] msisdns = new Long[]{923455777709L, 923055553444L, 923458558800L, 923002082221L, 923005552275L, 923455003231L};
        List<Long> listOfMsisdns = new ArrayList<Long>(Arrays.asList(msisdns));

        if (listOfMsisdns.contains(msisdn)) {
            System.out.println("This msisdn found in the list :: " + msisdn);
            return true;
        }

        return false;
    }


}