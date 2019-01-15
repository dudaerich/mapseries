package cz.mzk.mapseries.oai.marc;

import java.util.*;

/**
 * @author Erich Duda <dudaerich@gmail.com>
 */
public class MarcDataField {

    private final String ind1;

    private final String ind2;

    private final Map<String, String> subfields;

    private MarcDataField(Builder builder) {
        this.ind1 = builder.ind1;
        this.ind2 = builder.ind2;
        this.subfields = Collections.unmodifiableMap(builder.subfields);
    }

    public Optional<String> getInd1() {
        return Optional.ofNullable(ind1);
    }

    public Optional<String> getInd2() {
        return Optional.ofNullable(ind2);
    }

    public Optional<String> getSubfield(String code) {
        return Optional.ofNullable(subfields.get(code));
    }

    @Override
    public String toString() {
        return "MarcDataField{" +
                "ind1='" + ind1 + '\'' +
                ", ind2='" + ind2 + '\'' +
                ", subfields=" + subfields +
                '}';
    }

    static class Builder {

        private String ind1;

        private String ind2;

        private final Map<String, String> subfields = new HashMap<>();

        Builder withIndicator1(String ind1) {
            Objects.requireNonNull(ind1);
            this.ind1 = ind1;
            return this;
        }

        Builder withIndicator2(String ind2) {
            Objects.requireNonNull(ind2);
            this.ind2 = ind2;
            return this;
        }

        Builder withSubfield(String name, String value) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);
            subfields.put(name, value);
            return this;
        }

        MarcDataField build() {
            return new MarcDataField(this);
        }

    }
}
