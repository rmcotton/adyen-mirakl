package com.adyen.mirakl.domain;

import com.adyen.mirakl.service.util.MiraklDataExtractionUtil;
import com.adyen.model.marketpay.CreateAccountHolderRequest;
import com.mirakl.client.mmp.domain.common.MiraklAdditionalFieldValue;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreetDetails {

    private final static Logger log = LoggerFactory.getLogger(StreetDetails.class);

    private String streetName;
    private String houseNumberOrName;

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public static StreetDetails createStreetDetailsFromSingleLine(String miraklHouseNumberOrName, String street, Pattern houseNumberPattern) {
        StreetDetails streetDetails = new StreetDetails();
        //Mirakl house number or name takes precedence
        if(StringUtils.isNotBlank(miraklHouseNumberOrName) || houseNumberPattern == null){
            streetDetails.setHouseNumberOrName(miraklHouseNumberOrName);
            streetDetails.setStreetName(street);
        }
        if(street == null) {
            return streetDetails;
        }
        if(houseNumberPattern == null){
            return streetDetails;
        }

        Matcher matcher = houseNumberPattern.matcher(street);
        if (matcher.find()) {
            String houseNumber = matcher.group(1);
            StringBuilder sb = new StringBuilder();
            sb.append(street.substring(0, matcher.start()));
            if (matcher.end() + 1 < street.length()) {
                sb.append(street.substring(matcher.end()));
            }
            streetDetails.setStreetName(sb.toString());
            streetDetails.setHouseNumberOrName(houseNumber);
        } else {
            streetDetails.setStreetName(street);
        }

        return streetDetails;
    }

    public static String extractHouseNumberOrNameFromAdditionalFields(final List<MiraklAdditionalFieldValue> additionalFieldValues){
        final CreateAccountHolderRequest.LegalEntityEnum legalEntityType = MiraklDataExtractionUtil.getLegalEntityFromShop(additionalFieldValues);
        if(CreateAccountHolderRequest.LegalEntityEnum.BUSINESS.equals(legalEntityType)){
            return MiraklDataExtractionUtil.extractTextFieldFromAdditionalFields(additionalFieldValues, "adyen-business-housenumber");
        }else if(CreateAccountHolderRequest.LegalEntityEnum.INDIVIDUAL.equals(legalEntityType)){
            return MiraklDataExtractionUtil.extractTextFieldFromAdditionalFields(additionalFieldValues, "adyen-individual-housenumber");
        }
        log.warn("Unable to extract house number or name from additional fields");
        return null;
    }

    public String getHouseNumberOrName() {
        return houseNumberOrName;
    }

    private void setHouseNumberOrName(String houseNumberOrName) {
        this.houseNumberOrName = houseNumberOrName;
    }
}
