package com.adyen.mirakl.service.util;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class IsoUtil {

    private static final Map<String, String> countryCodes;

    static {
        countryCodes = new HashMap<>();
        String[] isoCountries = Locale.getISOCountries();

        for (String country : isoCountries) {
            Locale locale = new Locale("", country);
            countryCodes.put(locale.getISO3Country(), locale.getCountry());
        }
    }

    public IsoUtil() {
        //empty constructor
    }

    /**
     * Get ISO-2 Country Code from ISO-3 Country Code
     */
    public static String getIso2CountryCodeFromIso3(String iso3) {
        if (StringUtils.isNotBlank(iso3)) {
            return countryCodes.get(iso3);
        }
        return null;
    }

}
