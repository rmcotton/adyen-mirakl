package com.adyen.mirakl.service.util;

import com.adyen.model.Address;
import com.adyen.model.Name;
import com.adyen.model.marketpay.PersonalData;
import com.adyen.model.marketpay.PhoneNumber;
import com.adyen.model.marketpay.ShareholderContact;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mirakl.client.mmp.domain.common.MiraklAdditionalFieldValue;
import com.mirakl.client.mmp.domain.shop.MiraklShop;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ShopUtil {

    private static final String ADYEN_UBO = "adyen-ubo";
    private static final String FIRSTNAME = "firstname";
    private static final String LASTNAME = "lastname";
    private static final String CIVILITY = "civility";
    private static final String EMAIL = "email";
    private static final String COUNTRY = "country";
    private static final String STREET = "street";
    private static final String HOUSE_NUMBER_OR_NAME = "houseNumberOrName";
    private static final String CITY = "city";
    private static final String POSTAL_CODE = "postalCode";
    private static final String STATE_OR_PROVINCE = "stateOrProvince";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String PHONE_NUMBER = "phoneNumber";

    public final static Map<String, Name.GenderEnum> CIVILITY_TO_GENDER = ImmutableMap.<String, Name.GenderEnum>builder().put("Mr", Name.GenderEnum.MALE)
        .put("Mrs", Name.GenderEnum.FEMALE)
        .put("Miss", Name.GenderEnum.FEMALE)
        .build();

    private ShopUtil() {
    }

    /**
     * Extract shareholder contact data in a adyen format from a mirakl shop
     * @param shop mirakl shop
     * @param maxUbos number of ubos to be extracted e.g. 4
     * @return share holder contacts to send to adyen
     */
    public static List<ShareholderContact> extractUbos(final MiraklShop shop, Integer maxUbos) {
        Map<String, String> extractedKeysFromMirakl = shop.getAdditionalFieldValues()
            .stream()
            .filter(MiraklAdditionalFieldValue.MiraklAbstractAdditionalFieldWithSingleValue.class::isInstance)
            .map(MiraklAdditionalFieldValue.MiraklAbstractAdditionalFieldWithSingleValue.class::cast)
            .collect(Collectors.toMap(MiraklAdditionalFieldValue::getCode,
                MiraklAdditionalFieldValue.MiraklAbstractAdditionalFieldWithSingleValue::getValue));

        ImmutableList.Builder<ShareholderContact> builder = ImmutableList.builder();
        generateKeys(maxUbos).forEach((i, keys) -> {
            String firstName = extractedKeysFromMirakl.getOrDefault(keys.get(FIRSTNAME), "");
            String lastName = extractedKeysFromMirakl.getOrDefault(keys.get(LASTNAME), "");
            String civility = extractedKeysFromMirakl.getOrDefault(keys.get(CIVILITY), "");
            String email = extractedKeysFromMirakl.getOrDefault(keys.get(EMAIL), "");
            String country = extractedKeysFromMirakl.getOrDefault(keys.get(COUNTRY), "");
            String street = extractedKeysFromMirakl.getOrDefault(keys.get(STREET), "");
            String houseNumberOrName = extractedKeysFromMirakl.getOrDefault(keys.get(HOUSE_NUMBER_OR_NAME), "");
            String city = extractedKeysFromMirakl.getOrDefault(keys.get(CITY), "");
            String postalCode = extractedKeysFromMirakl.getOrDefault(keys.get(POSTAL_CODE), "");
            String stateOrProvince = extractedKeysFromMirakl.getOrDefault(keys.get(STATE_OR_PROVINCE), "");
            String dateOfBirth = extractedKeysFromMirakl.getOrDefault(keys.get(DATE_OF_BIRTH), "");
            String phoneNumber = extractedKeysFromMirakl.getOrDefault(keys.get(PHONE_NUMBER), "");

            if (allMandatoryFieldsAvailable(firstName, lastName, civility, email)) {
                ShareholderContact shareholderContact = new ShareholderContact();
                Name name = new Name();
                name.setFirstName(firstName);
                name.setLastName(lastName);
                name.setGender(CIVILITY_TO_GENDER.getOrDefault(civility, Name.GenderEnum.UNKNOWN));
                shareholderContact.setName(name);
                shareholderContact.setEmail(email);
                final PhoneNumber phoneNumberWrapper = new PhoneNumber();
                phoneNumberWrapper.setPhoneNumber(phoneNumber);
                shareholderContact.setPhoneNumber(phoneNumberWrapper);
                final Address address = new Address();
                address.setCountry(country);
                address.setStreet(street);
                address.setHouseNumberOrName(houseNumberOrName);
                address.setCity(city);
                address.setPostalCode(postalCode);
                address.setStateOrProvince(stateOrProvince);
                shareholderContact.setAddress(address);
                final PersonalData personalData = new PersonalData();
                personalData.setDateOfBirth(dateOfBirth);
                shareholderContact.setPersonalData(personalData);
                builder.add(shareholderContact);
            }
        });
        return builder.build();
    }

    private static boolean allMandatoryFieldsAvailable(final String firstName, final String lastName, final String civility, final String email) {
        return ImmutableList.of(firstName, lastName, civility, email).stream().noneMatch(StringUtils::isBlank);
    }

    private static Map<Integer, Map<String, String>> generateKeys(Integer maxUbos) {
        return IntStream.rangeClosed(1, maxUbos).mapToObj(i -> {
            final Map<Integer, Map<String, String>> grouped = new HashMap<>();
            grouped.put(i, new ImmutableMap.Builder<String, String>()
                    .put(FIRSTNAME, ADYEN_UBO + String.valueOf(i) + "-firstname")
                    .put(LASTNAME,ADYEN_UBO + String.valueOf(i) + "-lastname")
                    .put(CIVILITY,ADYEN_UBO + String.valueOf(i) + "-civility")
                    .put(EMAIL,ADYEN_UBO + String.valueOf(i) + "-email")
                    .put(COUNTRY,ADYEN_UBO + String.valueOf(i) + "-country")
//                    .put(STREET,ADYEN_UBO + String.valueOf(i) + "-street")
//                    .put(HOUSE_NUMBER_OR_NAME,ADYEN_UBO + String.valueOf(i) + "-houseNumberOrName")
//                    .put(CITY,ADYEN_UBO + String.valueOf(i) + "-city")
//                    .put(POSTAL_CODE,ADYEN_UBO + String.valueOf(i) + "-postalCode")
//                    .put(STATE_OR_PROVINCE,ADYEN_UBO + String.valueOf(i) + "-stateOrProvince")
//                    .put(DATE_OF_BIRTH,ADYEN_UBO + String.valueOf(i) + "-dateOfBirth")
//                    .put(PHONE_NUMBER,ADYEN_UBO + String.valueOf(i) + "-phoneNumber")
                    .build());
            return grouped;
        }).reduce((x, y) -> {
            x.put(y.entrySet().iterator().next().getKey(), y.entrySet().iterator().next().getValue());
            return x;
        }).orElseThrow(() -> new IllegalStateException("UBOs must exist, number found: " + maxUbos));
    }
}
