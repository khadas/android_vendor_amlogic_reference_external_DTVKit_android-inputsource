package com.droidlogic.settings;

import androidx.annotation.NonNull;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextCharSetManager {
    private Map<String, String> mDefaultCharSets = new HashMap<>();
    private Map<String, Integer> mOptions = new HashMap<>();
    private List<String> mOptionPositions = new ArrayList<>();
    private int mCurrentCharSetIndex = -1;
    private static TextCharSetManager mInstance;

    private static final String CHARACTER_LATIN_DEFAULT     = "Latin";
    private static final String CHARACTER_LATIN_POLISH      = "Polish";
    private static final String CHARACTER_LATIN_TURKISH     = "Turkish";
    private static final String CHARACTER_LATIN_SERBIAN     = "Serbian";
    private static final String CHARACTER_LATIN_RUMANIAN    = "Rumanian";
    private static final String CHARACTER_CYRILLIC          = "Cyrillic";
    private static final String CHARACTER_GREEK_TURKISH     = "Greek";
    private static final String CHARACTER_LATIN_ARABIC      = "Arabic";
    private static final String CHARACTER_HEBREW_ARABIC     = "Hebrew";
    private static final String CHARACTER_GENERIC_RUS       = "Russian";
    private static final String CHARACTER_GENERIC_PERSIAN   = "Persian";

    private static final int REGION_ID_LATIN_DEFAULT        = 0;
    private static final int REGION_ID_LATIN_POLISH         = 8;
    private static final int REGION_ID_LATIN_TURKISH        = 22;
    private static final int REGION_ID_LATIN_SERBIAN        = 24;
    private static final int REGION_ID_LATIN_RUMANIAN       = 24;
    private static final int REGION_ID_CYRILLIC             = 32;
    private static final int REGION_ID_RUSSIAN              = 36;
    private static final int REGION_ID_GREEK_TURKISH        = 48;
    private static final int REGION_ID_LATIN_ARABIC         = 71;
    private static final int REGION_ID_HEBREW_ARABIC        = 85;
    private static final int REGION_ID_GENERIC_PERSIAN      = 88;

    private final String[] charSetOptions = {
            CHARACTER_LATIN_DEFAULT,
            CHARACTER_LATIN_POLISH,
            CHARACTER_LATIN_TURKISH,
            CHARACTER_LATIN_SERBIAN,
            CHARACTER_LATIN_RUMANIAN,
            CHARACTER_CYRILLIC,
            CHARACTER_GENERIC_RUS,
            CHARACTER_GREEK_TURKISH,
            CHARACTER_LATIN_ARABIC,
            CHARACTER_HEBREW_ARABIC,
            CHARACTER_GENERIC_PERSIAN,
    };

    private final int[] charSetOptionsValues = {
            REGION_ID_LATIN_DEFAULT,
            REGION_ID_LATIN_POLISH,
            REGION_ID_LATIN_TURKISH,
            REGION_ID_LATIN_SERBIAN,
            REGION_ID_LATIN_RUMANIAN,
            REGION_ID_CYRILLIC,
            REGION_ID_RUSSIAN,
            REGION_ID_GREEK_TURKISH,
            REGION_ID_LATIN_ARABIC,
            REGION_ID_HEBREW_ARABIC,
            REGION_ID_GENERIC_PERSIAN,
    };

    private final String[][] entries = {
            {"arg", CHARACTER_LATIN_DEFAULT},  //Argentina
            {"aus", CHARACTER_LATIN_DEFAULT},  //Australia
            {"aut", CHARACTER_LATIN_DEFAULT},  //Austria
            {"aze", CHARACTER_LATIN_DEFAULT},  //Azerbaijan
            {"bhr", CHARACTER_LATIN_ARABIC},//Bahrain
            {"blr", CHARACTER_CYRILLIC},  //Belarus
            {"bel", CHARACTER_LATIN_DEFAULT},  //Belgium
            {"bol", CHARACTER_LATIN_DEFAULT},  //Bolivia
            {"bra", CHARACTER_LATIN_DEFAULT},  //Brazil
            {"bgr", CHARACTER_CYRILLIC},  //Bulgaria
            {"cmr", CHARACTER_LATIN_DEFAULT},  //Cameroon
            {"chl", CHARACTER_LATIN_DEFAULT},  //Chile
            {"chn", CHARACTER_LATIN_DEFAULT},  //China
            {"col", CHARACTER_LATIN_DEFAULT},  //Colombia
            {"cri", CHARACTER_LATIN_DEFAULT},  //Costa Rica
            {"hrv", CHARACTER_CYRILLIC},  //Croatia
            {"cze", CHARACTER_CYRILLIC},  //Czech Rep
            {"dnk", CHARACTER_LATIN_DEFAULT},  //Denmark
            {"dom", CHARACTER_LATIN_DEFAULT},  //Dominican Rep
            {"ecu", CHARACTER_LATIN_DEFAULT},  //Ecuador
            {"egy", CHARACTER_HEBREW_ARABIC}, //Egypt
            {"slv", CHARACTER_LATIN_DEFAULT},  //El Salvador
            {"est", CHARACTER_LATIN_DEFAULT},  //Estonia
            {"fin", CHARACTER_LATIN_DEFAULT},  //Finland
            {"fra", CHARACTER_LATIN_DEFAULT},  //France
            {"geo", CHARACTER_LATIN_ARABIC},//Georgia
            {"deu", CHARACTER_LATIN_DEFAULT},  //Germany
            {"gha", CHARACTER_LATIN_DEFAULT},  //Ghana
            {"grc", CHARACTER_GREEK_TURKISH},  //Greece
            {"gtm", CHARACTER_LATIN_DEFAULT},  //Guatemala
            {"hnd", CHARACTER_LATIN_DEFAULT},  //Honduras
            {"hun", CHARACTER_LATIN_DEFAULT},  //Hungary
            {"ind", CHARACTER_LATIN_DEFAULT},  //India
            {"idn", CHARACTER_LATIN_DEFAULT},  //Indonesia
            {"irn", CHARACTER_GENERIC_PERSIAN},//Iran
            {"irq", CHARACTER_LATIN_ARABIC},//Iraq
            {"irl", CHARACTER_LATIN_DEFAULT},  //Ireland
            {"isr", CHARACTER_HEBREW_ARABIC}, //Israel
            {"ita", CHARACTER_LATIN_DEFAULT},  //Italy
            {"jpn", CHARACTER_LATIN_DEFAULT},  //Japan
            {"jor", CHARACTER_LATIN_ARABIC},//Jordan
            {"ken", CHARACTER_LATIN_DEFAULT},  //Kenya
            {"kwt", CHARACTER_LATIN_ARABIC},//Kuwait
            {"lva", CHARACTER_CYRILLIC},  //Latvia
            {"lby", CHARACTER_LATIN_ARABIC},//Libya
            {"lux", CHARACTER_LATIN_DEFAULT},  //Luxembourg
            {"omn", CHARACTER_LATIN_ARABIC},//Oman
            {"mys", CHARACTER_LATIN_DEFAULT},  //Malaysia
            {"mex", CHARACTER_LATIN_DEFAULT},  //Mexico
            {"mar", CHARACTER_LATIN_ARABIC},//Morocco
            {"mmr", CHARACTER_LATIN_DEFAULT},  //Myanmar
            {"nld", CHARACTER_LATIN_DEFAULT},  //Netherlands
            {"nzl", CHARACTER_LATIN_DEFAULT},  //New Zealand
            {"nic", CHARACTER_LATIN_DEFAULT},  //Nicaragua
            {"nor", CHARACTER_LATIN_DEFAULT},  //Norway
            {"pak", CHARACTER_LATIN_ARABIC},//Pakistan
            {"pan", CHARACTER_LATIN_DEFAULT},  //Panama
            {"per", CHARACTER_LATIN_DEFAULT},  //Peru
            {"pol", CHARACTER_LATIN_POLISH},//Poland
            {"prt", CHARACTER_LATIN_DEFAULT},  //Portugal
            {"qat", CHARACTER_LATIN_ARABIC}, //Qatar
            {"rou", CHARACTER_LATIN_RUMANIAN},//Romania
            {"rus", CHARACTER_GENERIC_RUS},   //Russia
            {"sau", CHARACTER_LATIN_ARABIC},//Saudi Arabia
            {"srb", CHARACTER_LATIN_SERBIAN},//Serbia
            {"sgp", CHARACTER_LATIN_DEFAULT},  //Singapore
            {"svk", CHARACTER_LATIN_DEFAULT},  //Slovakia
            {"svn", CHARACTER_LATIN_DEFAULT},  //Slovenia
            {"zaf", CHARACTER_LATIN_DEFAULT},  //South Africa
            {"esp", CHARACTER_LATIN_DEFAULT},  //Spain
            {"swe", CHARACTER_LATIN_DEFAULT},  //Sweden
            {"che", CHARACTER_LATIN_DEFAULT},  //Switzerland
            {"tza", CHARACTER_LATIN_DEFAULT},  //Tanzania
            {"tha", CHARACTER_LATIN_DEFAULT},  //Thailand
            {"tun", CHARACTER_LATIN_ARABIC},//Tunisia
            {"tur", CHARACTER_LATIN_TURKISH},//Turkey
            {"twn", CHARACTER_LATIN_ARABIC},//Taiwan(China)
            {"are", CHARACTER_LATIN_ARABIC},//United Arab Emirates
            {"gbr", CHARACTER_LATIN_DEFAULT},  //UK
            {"ukr", CHARACTER_CYRILLIC},  //Ukraine
            {"ven", CHARACTER_LATIN_DEFAULT},  //Venezuela
            {"vnm", CHARACTER_LATIN_DEFAULT},  //Vietnam
    };

    private TextCharSetManager() {
        int positionIndex = 0;

        for (String[] e : entries) {
            mDefaultCharSets.put(e[0], e[1]);
        }
        for (String opt : charSetOptions) {
            if (positionIndex < charSetOptionsValues.length) {
                mOptions.put(opt, charSetOptionsValues[positionIndex]);
            }
            mOptionPositions.add(opt);
            positionIndex ++;
        }
    }

    public static TextCharSetManager getInstance() {
        if (mInstance == null)
            mInstance = new TextCharSetManager();
        return mInstance;
    }

    public List<String> getTextCharsetNameList() {
        return mOptionPositions;
    }

    public int getCurrentCharSetIndex() {
        return mCurrentCharSetIndex;
    }

    public void setCurrentTeletextCharsetByPosition(int position) {
        if (position < charSetOptionsValues.length) {
            mCurrentCharSetIndex = position;
            String charSet = mOptionPositions.get(mCurrentCharSetIndex);
            DtvkitGlueClient.getInstance().setRegionId(mOptions.get(charSet));
        }
    }

    public String getDefaultCharSetOfCountry(@NonNull String countryCode) {
        String charSet = mDefaultCharSets.get(countryCode);
        if (charSet == null) charSet = CHARACTER_LATIN_DEFAULT;
        return charSet;
    }

    public void updateCharSetForCountry(@NonNull String countryCode) {
        String charSet = getDefaultCharSetOfCountry(countryCode);
        mCurrentCharSetIndex = mOptionPositions.indexOf(charSet);
        DtvkitGlueClient.getInstance().setRegionId(mOptions.get(charSet));
    }

    public void updateCharSetForCurrentCountry() {
        int countryCodeInt = -1;
        try {
            JSONObject result = DtvkitGlueClient.getInstance().request(
                    "Dvb.getcurrentCountryInfos", new JSONArray());
            if (result != null) {
                JSONObject data = (JSONObject) result.get("data");
                if (data != null) {
                    countryCodeInt = data.optInt("country_code", -1);
                }
            }
        } catch (Exception e) {
        }
        if (countryCodeInt != -1) {
            String countryCode = getCountryCodeStr(countryCodeInt);
            updateCharSetForCountry(countryCode);
        }
    }

    private String getCountryCodeStr(int countryCodeInt) {
        return (String.valueOf((char)(countryCodeInt >> 16)) +
                String.valueOf((char)((countryCodeInt >> 8) & 0xff)) +
                String.valueOf((char)(countryCodeInt & 0xff)));
    }

    public int switchNextRegion() {
        int max = charSetOptions.length - 1;
        int select = mCurrentCharSetIndex += 1;
        if (select > max)
            select = 0;
        setCurrentTeletextCharsetByPosition(select);
        return select;
    }
}