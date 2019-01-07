package cz.mzk.mapseries.oai.marc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Erich Duda <dudaerich@gmail.com>
 */
public class MarcDataField {

    private String ind1;

    private String ind2;

    private final Map<String, String> subfields = new HashMap<>();

    public String getInd1() {
        return ind1;
    }

    public void setInd1(String ind1) {
        this.ind1 = ind1;
    }

    public String getInd2() {
        return ind2;
    }

    public void setInd2(String ind2) {
        this.ind2 = ind2;
    }

    public void addSubfield(String code, String value) {
        subfields.put(code, value);
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
}
