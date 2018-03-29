package com.adyen.mirakl.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.adyen.constants.ErrorTypeCodes;
import com.adyen.model.marketpay.ErrorFieldType;
import com.google.common.collect.ImmutableList;
import com.mirakl.client.mmp.domain.shop.MiraklShop;

@Service
@Transactional
public class InvalidFieldsNotificationService {

    private final Logger log = LoggerFactory.getLogger(InvalidFieldsNotificationService.class);

    // error codes that are safe to be sent to seller
    private final static List<Integer> sellerErrorCodes = ImmutableList.of(ErrorTypeCodes.EMAIL_INVALID,
                                                                           ErrorTypeCodes.COUNTRY_INVALID,
                                                                           ErrorTypeCodes.CONTAINS_NUMBERS,
                                                                           ErrorTypeCodes.WEB_ADDRESS_INVALID,
                                                                           ErrorTypeCodes.INVALID_DATE_FORMAT,
                                                                           ErrorTypeCodes.DATE_OUT_OF_RANGE,
                                                                           ErrorTypeCodes.BANK_DETAILS_INVALID,
                                                                           ErrorTypeCodes.POSTAL_CODE_INVALID,
                                                                           ErrorTypeCodes.STATE_CODE_INVALID,
                                                                           ErrorTypeCodes.STATE_CODE_UNKNOWN,
                                                                           ErrorTypeCodes.PHONE_NUMBER_INVALID,
                                                                           ErrorTypeCodes.PHONE_NUMBER_TOO_SHORT,
                                                                           ErrorTypeCodes.COUNTRY_NOT_SUPPORTED,
                                                                           ErrorTypeCodes.INVALID_CURRENCY,
                                                                           ErrorTypeCodes.BANK_CODE_UNKNOWN);

    @Resource
    private MailTemplateService mailTemplateService;

    public List<String> getErrorsFromInvalidFields(List<ErrorFieldType> invalidFields) {
        if (CollectionUtils.isEmpty(invalidFields)) {
            return new ArrayList<>();
        }

        return invalidFields.stream().map(InvalidFieldsNotificationService::errorFieldTypeToString).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void handleErrorsInResponse(MiraklShop shop, List<ErrorFieldType> invalidFields) {
        List<ErrorFieldType> invalidFieldsForSeller = invalidFields.stream().filter(errorFieldType -> sellerErrorCodes.contains(errorFieldType.getErrorCode())).collect(Collectors.toList());
        List<ErrorFieldType> invalidFieldsForOperator = invalidFields.stream().filter(errorFieldType -> ! sellerErrorCodes.contains(errorFieldType.getErrorCode())).collect(Collectors.toList());

        List<String> sellerErrors = getErrorsFromInvalidFields(invalidFieldsForSeller);
        List<String> operatorErrors = getErrorsFromInvalidFields(invalidFieldsForOperator);

        if (! sellerErrors.isEmpty()) {
            // notify seller with specific message
            log.debug("Sending {} error(s) to Seller {}", sellerErrors.size(), shop.getId());
            mailTemplateService.sendSellerEmailWithErrors(shop, sellerErrors);
        }

        if (! operatorErrors.isEmpty()) {
            // notify operator
            log.debug("Sending {} error(s) to Operator", operatorErrors.size());
            mailTemplateService.sendOperatorEmailWithErrors(shop, operatorErrors);
        }
    }

    private static String errorFieldTypeToString(ErrorFieldType errorFieldType) {
        StringBuilder sb = new StringBuilder();

        final List<Integer> errorDescriptionCodes = ImmutableList.of(ErrorTypeCodes.EMAIL_INVALID,
                                                                     ErrorTypeCodes.COUNTRY_INVALID,
                                                                     ErrorTypeCodes.CONTAINS_NUMBERS,
                                                                     ErrorTypeCodes.WEB_ADDRESS_INVALID,
                                                                     ErrorTypeCodes.INVALID_DATE_FORMAT,
                                                                     ErrorTypeCodes.DATE_OUT_OF_RANGE,
                                                                     ErrorTypeCodes.POSTAL_CODE_INVALID,
                                                                     ErrorTypeCodes.STATE_CODE_INVALID,
                                                                     ErrorTypeCodes.STATE_CODE_UNKNOWN,
                                                                     ErrorTypeCodes.PHONE_NUMBER_INVALID,
                                                                     ErrorTypeCodes.PHONE_NUMBER_TOO_SHORT,
                                                                     ErrorTypeCodes.COUNTRY_NOT_SUPPORTED,
                                                                     ErrorTypeCodes.INVALID_CURRENCY,
                                                                     ErrorTypeCodes.BANK_CODE_UNKNOWN);

        if (errorDescriptionCodes.contains(errorFieldType.getErrorCode())) {
            return errorFieldType.getErrorDescription();
        }
        if (ErrorTypeCodes.BANK_DETAILS_INVALID.equals(errorFieldType.getErrorCode())) {
            return sb.append("Bank details are invalid: ").append(errorFieldType.getErrorDescription()).toString();
        }

        sb.append(errorFieldType.getErrorDescription());
        if (errorFieldType.getFieldType().getField() != null) {
            sb.append(": ").append(errorFieldType.getFieldType().getField());
        }
        return sb.toString();
    }

    public MailTemplateService getMailTemplateService() {
        return mailTemplateService;
    }

    public void setMailTemplateService(MailTemplateService mailTemplateService) {
        this.mailTemplateService = mailTemplateService;
    }
}
