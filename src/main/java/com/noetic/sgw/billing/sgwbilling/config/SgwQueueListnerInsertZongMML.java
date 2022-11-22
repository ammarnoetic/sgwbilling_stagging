package com.noetic.sgw.billing.sgwbilling.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.noetic.sgw.billing.sgwbilling.entities.GamesBillingRecordEntity;
import com.noetic.sgw.billing.sgwbilling.repository.GamesBillingRecordsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Service
public class SgwQueueListnerInsertZongMML {

    Logger log = LoggerFactory.getLogger( SgwQueueListnerInsertZongMML.class.getName());

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private GamesBillingRecordsRepository gamesBillingRecordsRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = {"sgwbillingsavechargingZongmmlQueue"})
    public void receive(String msg) {

        try {
            log.info("Queue Listner Recieved Message for sgwbillingsavechargingZongmmlQueue");

            GamesBillingRecordEntity customMessage = objectMapper.readValue(msg, GamesBillingRecordEntity.class);
            gamesBillingRecordsRepository.save(customMessage);
        
            log.info("BILLING SERVICE || Zong CHARGING || RECORDS INSERTED FOR MSISDN "+customMessage.getMsisdn());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
