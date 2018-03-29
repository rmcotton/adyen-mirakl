package com.adyen.mirakl.service.util;

import com.adyen.mirakl.startup.MiraklStartupValidator;
import com.adyen.model.marketpay.CreateAccountHolderRequest;
import com.mirakl.client.mmp.domain.additionalfield.MiraklAdditionalFieldType;
import com.mirakl.client.mmp.domain.common.MiraklAdditionalFieldValue;

import java.util.Arrays;
import java.util.List;

public final class MiraklDataExtractionUtil {

    public MiraklDataExtractionUtil() {
        //empty constructor
    }

    public static CreateAccountHolderRequest.LegalEntityEnum getLegalEntityFromShop(List<MiraklAdditionalFieldValue> additionalFields) {
        MiraklAdditionalFieldValue.MiraklValueListAdditionalFieldValue additionalFieldValue = (MiraklAdditionalFieldValue.MiraklValueListAdditionalFieldValue) additionalFields
            .stream()
            .filter(field -> isListWithCode(field, MiraklStartupValidator.CustomMiraklFields.ADYEN_LEGAL_ENTITY_TYPE))
            .findAny()
            .orElseThrow(() -> new RuntimeException("Legal entity not found"));

        return Arrays.stream(CreateAccountHolderRequest.LegalEntityEnum.values())
            .filter(legalEntityEnum -> legalEntityEnum.toString().equalsIgnoreCase(additionalFieldValue.getValue()))
            .findAny()
            .orElseThrow(() -> new RuntimeException("Invalid legal entity: " + additionalFieldValue.toString()));
    }

    private static boolean isListWithCode(MiraklAdditionalFieldValue additionalFieldValue, MiraklStartupValidator.CustomMiraklFields field) {
        return MiraklAdditionalFieldType.LIST.equals(additionalFieldValue.getFieldType()) && field.toString().equalsIgnoreCase(additionalFieldValue.getCode());
    }

    public static String extractTextFieldFromAdditionalFields(final List<MiraklAdditionalFieldValue> additionalFieldValues, String key){
        return additionalFieldValues.stream()
            .filter(MiraklAdditionalFieldValue.MiraklStringAdditionalFieldValue.class::isInstance)
            .map(MiraklAdditionalFieldValue.MiraklStringAdditionalFieldValue.class::cast)
            .filter(x -> key.equalsIgnoreCase(x.getCode()))
            .map(MiraklAdditionalFieldValue.MiraklAbstractAdditionalFieldWithSingleValue::getValue)
            .findAny().orElse(null);
    }
}
