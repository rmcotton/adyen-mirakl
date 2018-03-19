package com.adyen.mirakl.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.adyen.mirakl.config.MailTemplateService;
import com.adyen.model.marketpay.ErrorFieldType;
import com.google.common.collect.ImmutableList;
import com.mirakl.client.mmp.domain.shop.MiraklShop;

@Service
@Transactional
public class InvalidFieldsNotificationService {

    private final Logger log = LoggerFactory.getLogger(InvalidFieldsNotificationService.class);

    // error codes that are safe to be sent to seller
    // @todo: update with adyen-java-api constants
    private final static List<Integer> sellerErrorCodes = ImmutableList.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 18);

    @Resource
    private MailTemplateService mailTemplateService;

    public List<String> getErrorsFromInvalidFields(List<ErrorFieldType> invalidFields) {
        if (CollectionUtils.isEmpty(invalidFields)) {
            return null;
        }

        return invalidFields.stream().map(InvalidFieldsNotificationService::errorFieldTypeToString).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void handleErrorsInResponse(MiraklShop shop, List<ErrorFieldType> invalidFields) {
        List<String> errors = getErrorsFromInvalidFields(invalidFields);

        if (errors.isEmpty()) {
            log.info("Empty list of errors");
            return;
        }

        boolean sendToOperator = ! invalidFields.stream().allMatch(errorFieldType -> sellerErrorCodes.contains(errorFieldType.getErrorCode()));

        if (sendToOperator) {
            // notify operator
            mailTemplateService.sendOperatorEmailWithErrors(shop, errors);
            // notify seller with generic message
            mailTemplateService.sendGenericSellerEmail(shop);
        } else {
            // notify seller with specific message
            mailTemplateService.sendSellerEmailWithErrors(shop, errors);
        }
    }

    private static String errorFieldTypeToString(ErrorFieldType errorFieldType) {
        StringBuilder sb = new StringBuilder();
        switch (errorFieldType.getErrorCode()) {
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 9:
            case 10:
            case 11:
            case 13:
            case 14:
            case 15:
            case 16:
            case 18:
                return errorFieldType.getErrorDescription();
            case 8:
                return sb.append("Bank details are invalid: ").append(errorFieldType.getErrorDescription()).toString();
            default:
                sb.append(errorFieldType.getErrorDescription());
                if (errorFieldType.getFieldType().getField() != null) {
                    sb.append(": ").append(errorFieldType.getFieldType().getField());
                }
                return sb.toString();
        }
    }

    public MailTemplateService getMailTemplateService() {
        return mailTemplateService;
    }

    public void setMailTemplateService(MailTemplateService mailTemplateService) {
        this.mailTemplateService = mailTemplateService;
    }
}
