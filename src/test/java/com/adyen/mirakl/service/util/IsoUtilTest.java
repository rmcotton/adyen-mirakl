package com.adyen.mirakl.service.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class IsoUtilTest {


    @Test
    public void testGetIso2CountryCode() {
        assertEquals("GB", IsoUtil.getIso2CountryCodeFromIso3("GBR"));
    }

}
