package org.dtvkit.inputsource;

import java.util.HashMap;
import java.util.Map;

public class ISO639Data {
    private static final Map<String, String> iso6392b2t_diff = new HashMap<>();
    static {
        final String[][] entries = {
                //{ 3-letter in 2b,  3-letter in 2t},
                {"alb", "sqi"}, //Albanian
                {"arm", "hye"}, //Armenian
                {"baq", "eus"}, //Basque
                {"bur", "mya"}, //Burmese
                {"chi", "zho"}, //Chinese
                {"cze", "ces"}, //Czech
                {"dut", "nld"}, //Dutch
                {"fre", "fra"}, //French
                {"geo", "kat"}, //Georgian
                {"ger", "deu"}, //German
                {"gre", "ell"}, //Greek
                {"ice", "isl"}, //Icelandic
                {"mac", "mkd"}, //Macedonian
                {"may", "msa"}, //Malay
                {"mao", "mri"}, //Maori
                {"per", "fas"}, //Persian
                {"rum", "ron"}, //Romanian
                {"slo", "slk"}, //Slovak
                {"tib", "bod"}, //Tibetan
                {"wel", "cym"}, //Welsh
        };

        for (String[] e :entries)
        {
            iso6392b2t_diff.put(e[0], e[1]);
        }
    }

    /**
     * Try to convert iso639-2/b to iso639-2/t
     * For Android locale class does not support iso639-2/b, but some
     * dtv streams transfer language code with iso639-2/b type.
     * We store the difernent code list between iso639-2/b and iso639-2/t
     * @param lanCode (lowercase)
     * @return If lanCode in the store list, return the value of iso639-2t,
     * otherwise, return the lanCode self (Maybe is iso639-2/t or iso639-3
     * code, and this should be parsed by android locale)
     */
    public static String parse(String lanCode) {
        String iso2tCode = iso6392b2t_diff.get(lanCode);
        return (iso2tCode == null) ? lanCode : iso2tCode;
    }
}
