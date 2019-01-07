package cz.mzk.mapseries.oai.marc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Erich Duda <dudaerich@gmail.com>
 */
public class MarcRecord {

    private final Map<String, String> controlFields = new HashMap<>();

    private final Map<String, List<MarcDataField>> dataFields = new HashMap<>();

    public void addControlField(String tag, String value) {
        controlFields.put(tag, value);
    }

    public Optional<String> getControlField(String tag) {
        return Optional.ofNullable(controlFields.get(tag));
    }

    public void addDataField(String tag, MarcDataField dataField) {
        if (!dataFields.containsKey(tag)) {
            dataFields.put(tag, new ArrayList<>());
        }
        dataFields.get(tag).add(dataField);
    }

    public List<MarcDataField> getDataFields(String tag) {
        return dataFields.getOrDefault(tag, Collections.EMPTY_LIST);
    }

    @Override
    public String toString() {
        return "MarcRecord{" +
                "controlFields=" + controlFields +
                ", dataFields=" + dataFields +
                '}';
    }
}
