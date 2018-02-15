package com.adyen.mirakl.startup;

import com.adyen.model.marketpay.notification.*;
import com.adyen.service.Notification;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

@Component
@ConfigurationProperties("adyenNotificationsConfig")
public class AdyenStartupValidator implements ApplicationListener<ContextRefreshedEvent> {

    private final Logger log = LoggerFactory.getLogger(AdyenStartupValidator.class);

    @Resource
    private Notification adyenNotification;

    private List<CreateNotificationConfigurationRequest> notificationConfigurationDetails;

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {

        System.out.println(notificationConfigurationDetails);
        System.out.println(adyenNotification);

        try {

            final GetNotificationConfigurationListResponse notificationConfigurationList = adyenNotification.getNotificationConfigurationList();
            final CreateNotificationConfigurationRequest createNotificationConfigurationRequest = new CreateNotificationConfigurationRequest();
            final NotificationConfigurationDetails configurationDetails = new NotificationConfigurationDetails();
            final NotificationEventConfiguration element = new NotificationEventConfiguration();
            element.setEventType(NotificationEventConfiguration.EventTypeEnum.ACCOUNT_HOLDER_CREATED);
            element.setIncludeMode(NotificationEventConfiguration.IncludeModeEnum.INCLUDE);
            configurationDetails.setActive(true);
            configurationDetails.setDescription("description");
            configurationDetails.setEventConfigs(ImmutableList.of(element));
            configurationDetails.setMessageFormat(NotificationConfigurationDetails.MessageFormatEnum.JSON);
            configurationDetails.setNotifyURL("");
            createNotificationConfigurationRequest.setConfigurationDetails(configurationDetails);
            final CreateNotificationConfigurationResponse notificationConfiguration = adyenNotification.createNotificationConfiguration(createNotificationConfigurationRequest);


        } catch (Exception e) {
            throw new IllegalStateException("TODO",e);
        }

    }


    public List<CreateNotificationConfigurationRequest> getNotificationConfigurationDetails() {
        return notificationConfigurationDetails;
    }

    public void setNotificationConfigurationDetails(final List<CreateNotificationConfigurationRequest> notificationConfigurationDetails) {
        this.notificationConfigurationDetails = notificationConfigurationDetails;
    }
}
