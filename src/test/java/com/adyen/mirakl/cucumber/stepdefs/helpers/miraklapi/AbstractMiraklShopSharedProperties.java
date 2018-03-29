package com.adyen.mirakl.cucumber.stepdefs.helpers.miraklapi;

import com.github.javafaker.Faker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.mirakl.client.mmp.request.additionalfield.MiraklRequestAdditionalFieldValue;
import com.mirakl.client.mmp.request.common.document.MiraklUploadDocument;
import com.mirakl.client.mmp.request.shop.document.MiraklUploadShopDocumentsRequest;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractMiraklShopSharedProperties {

    final static Faker FAKER = new Faker(new Locale("en-GB"));
    final static Faker FAKERNL = new Faker(new Locale("nl"));
    final static Gson GSON = new Gson();
    protected String email = "adyen-mirakl-".concat(UUID.randomUUID().toString()).concat("@mailtrap.com");
    private final Map<Integer, String> CIVILITIES = ImmutableMap.<Integer, String>builder()
        .put(1, "Mr")
        .put(2, "Mrs")
        .put(3, "Miss")
        .build();

    String maxUbos;

    String civility() {
        int randomCivility = ThreadLocalRandom.current().nextInt(1, 4);
        return CIVILITIES.get(randomCivility);
    }

    MiraklRequestAdditionalFieldValue.MiraklSimpleRequestAdditionalFieldValue createAdditionalField(String code, String value) {
        return new MiraklRequestAdditionalFieldValue.MiraklSimpleRequestAdditionalFieldValue(code, value);
    }

    MiraklUploadShopDocumentsRequest miraklUploadShopDocumentsRequest(String shopId, ImmutableList<MiraklUploadDocument> miraklUploadDocuments) {
        return new MiraklUploadShopDocumentsRequest(shopId, miraklUploadDocuments);
    }
}
