package cz.mzk.mapseries.oai.marc;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Erich Duda <dudaerich@gmail.com>
 */
public class MarcIdentifier {
    
    private static final Pattern MARC_ID_PATTERN = Pattern.compile("(\\d+)(\\w)");
    
    private final String field;
    private final String subfield;
    
    public MarcIdentifier(String field, String subfield) {
        this.field = field;
        this.subfield = subfield;
    }
    
    public static MarcIdentifier fromString(String str) {
        Objects.nonNull(str);
        
        Matcher m = MARC_ID_PATTERN.matcher(str);

        if (m.matches()) {
            if (m.groupCount() == 2) {
                String field = m.group(1);
                String subfield = m.group(2);
                return new MarcIdentifier(field, subfield);
            }
        }
        throw new RuntimeException("Incorrect format of marc field: " + str);
    }

    public String getField() {
        return field;
    }

    public String getSubfield() {
        return subfield;
    }
    
    
    
}
