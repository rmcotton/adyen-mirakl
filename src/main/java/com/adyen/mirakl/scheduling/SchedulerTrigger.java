package com.adyen.mirakl.scheduling;


import com.adyen.mirakl.service.RetryEmailService;
import com.adyen.mirakl.service.ShopService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Profile({"dev","prod"})
public class SchedulerTrigger {

    @Resource
    private ShopService shopService;

    @Resource
    private RetryEmailService retryEmailService;

    @Scheduled(cron = "${application.shopUpdaterCron}")
    public void runShopUpdates(){
        shopService.processUpdatedShops();
    }

    @Scheduled(cron = "${application.emailRetryCron}")
    public void retryEmails(){
        retryEmailService.retryFailedEmails();
    }

    @Scheduled(cron = "${application.removeSentEmailsCron}")
    public void removeSentEmails(){
        retryEmailService.removeSentEmails();
    }


}
