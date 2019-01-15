package cz.mzk.mapseries.oai.marc;

import java.util.Objects;
import java.util.Optional;
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
    private final String ind1;
    private final String ind2;

    private MarcIdentifier(Builder builder) {
        field = builder.field;
        subfield = builder.subfield;
        ind1 = builder.ind1;
        ind2 = builder.ind2;
    }
    
    public static MarcIdentifier fromString(String str) {
        Objects.requireNonNull(str);
        
        Matcher m = MARC_ID_PATTERN.matcher(str);

        if (m.matches()) {
            if (m.groupCount() == 2) {
                String field = m.group(1);
                String subfield = m.group(2);
                return new MarcIdentifier.Builder()
                        .withField(field)
                        .withSubfield(subfield)
                        .build();
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

    public Optional<String> getInd1() {
        return Optional.ofNullable(ind1);
    }

    public Optional<String> getInd2() {
        return Optional.ofNullable(ind2);
    }

    @Override
    public String toString() {
        return field + subfield;
    }

    public static class Builder {

        private String field;
        private String subfield;
        private String ind1;
        private String ind2;

        public Builder withField(String field) {
            Objects.requireNonNull(field);
            this.field = field;
            return this;
        }

        public Builder withSubfield(String subfield) {
            Objects.requireNonNull(subfield);
            this.subfield = subfield;
            return this;
        }

        public Builder withIndicator1(String ind1) {
            Objects.requireNonNull(ind1);
            this.ind1 = ind1;
            return this;
        }

        public Builder withIndicator2(String ind2) {
            Objects.requireNonNull(ind2);
            this.ind2 = ind2;
            return this;
        }

        public MarcIdentifier build() {
            Objects.requireNonNull(field);
            Objects.requireNonNull(subfield);

            return new MarcIdentifier(this);
        }



    }
}
