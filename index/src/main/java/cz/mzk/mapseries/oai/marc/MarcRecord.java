package cz.mzk.mapseries.oai.marc;

import java.util.ArrayList;
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

    public boolean hasControlField(String tag) {
        return controlFields.containsKey(tag);
    }

    public String getControlField(String tag) {
        return controlFields.get(tag);
    }

    public void addDataField(String tag, MarcDataField dataField) {
        if (!dataFields.containsKey(tag)) {
            dataFields.put(tag, new ArrayList<>());
        }
        dataFields.get(tag).add(dataField);
    }

    public boolean hasDataField(String tag) {
        return dataFields.containsKey(tag);
    }

    public List<MarcDataField> getDataFields(String tag) {
        return dataFields.get(tag);
    }
    
    public Optional<String> get(MarcIdentifier id) {
        if (!hasDataField(id.getField())) {
            return Optional.empty();
        }
        
        List<MarcDataField> fields = getDataFields(id.getField());
        
        for (MarcDataField dataField : fields) {
            if (dataField.hasSubfield(id.getSubfield())) {
                return Optional.of(dataField.getSubfield(id.getSubfield()));
            }
        }
        
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "MarcRecord{" +
                "controlFields=" + controlFields +
                ", dataFields=" + dataFields +
                '}';
    }
}
