package cz.mzk.mapseries.update;

import cz.mzk.mapseries.dao.SheetDAO;
import cz.mzk.mapseries.oai.marc.MarcIdentifier;
import cz.mzk.mapseries.oai.marc.MarcRecord;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author Erich Duda <dudaerich@gmail.com>
 */
public class SheetBuilder {
    
    private final ContentDefinition contentDefinition;
    private final MarcRecord marcRecord;
    private final PrintStream log;
    
    public SheetBuilder(ContentDefinition contentDefinition, MarcRecord marcRecord, PrintStream log) {
        this.contentDefinition = contentDefinition;
        this.marcRecord = marcRecord;
        this.log = log;
    }
    
    public Optional<SheetDAO> buildSheet() {
        Optional<String> sheetId = getId();
        if (!sheetId.isPresent()) {
            log.println("[WARN] getting of identificator failed for following record: " + marcRecord);
            return Optional.empty();
        }
        
        SheetDAO sheet = new SheetDAO();
        sheet.setSheetId(sheetId.get());
        sheet.setTitle(getTitle());
        sheet.setYear(getYear());
        
        String digitalLibraryUrl = getDigitalLibraryUrl();
        String thumbnailUrl = getThubmnailUrl(digitalLibraryUrl);
        
        sheet.setDigitalLibraryUrl(digitalLibraryUrl);
        sheet.setThumbnailUrl(thumbnailUrl);
        sheet.setVufindUrl(getVufindUrl());
        
        return Optional.of(sheet);
    }
    
    private Optional<String> getId() {
        MarcIdentifier sheetId = MarcIdentifier.fromString(contentDefinition.getSheets());
        
        List<String> sheetIdValues = getValues(sheetId);
        
        if (sheetIdValues.isEmpty()) {
            return Optional.empty();
        }
        
        if (sheetIdValues.size() > 1) {
            log.println("[WARN] found more than one sheet identificators: " + marcRecord);
        }
        
        String result = applyGroovyTransformation(sheetIdValues.get(0), contentDefinition.getGroupBy());
        return Optional.of(result);
    }
    
    private String applyGroovyTransformation(String value, String script) {
        Binding binding = new Binding();
        binding.setVariable("field", value);
        GroovyShell shell = new GroovyShell(binding);
        return shell.evaluate(script).toString();
    }
    
    private String getTitle() {
        MarcIdentifier marcId = new MarcIdentifier("245", "a");
        
        String title = String.join("; ", getValues(marcId));
        
        return !title.isEmpty() ? title : "Unknown";
    }
    
    private String getYear() {
        MarcIdentifier marcId = new MarcIdentifier("490", "v");
        
        List<String> years = getValues(marcId);
        
        String year;
        
        if (years.isEmpty()) {
            year = "";
        } else {
            year = years.get(0);
        }
        
        if (years.size() > 1) {
            log.println("[WARN] record contains more than one 490v fields: " + marcRecord);
        }
        
        if (year.contains(",")) {
            int comma = year.indexOf(',');
            year = year.substring(comma + 1).trim();
        }
        return year;
    }
    
    private String getDigitalLibraryUrl() {
        MarcIdentifier marcId = new MarcIdentifier("911", "u");
        List<String> urls = getValues(marcId);
        
        if (urls.isEmpty()) {
            return "";
        }
        
        if (urls.size() > 1) {
            log.println("[WARN] record contains more than one 911u fields: " + marcRecord);
        }
        
        return urls.get(0);
    }
    
    private String getThubmnailUrl(String digitalLibraryUrl) {
        if (digitalLibraryUrl.isEmpty()) {
            return "";
        } else {
            String uuid = digitalLibraryUrl.replace("http://www.digitalniknihovna.cz/mzk/uuid/uuid:", "");
            return String.format("https://kramerius.mzk.cz/search/api/v5.0/item/uuid:%s/thumb", uuid);
        }
    }
    
    private String getVufindUrl() {
        Optional<String> controlField = marcRecord.getControlField("001");
        if (!controlField.isPresent()) {
            log.println(String.format("[WARN] following record has no controlfield 001: %s", marcRecord));
            return "";
        } else {
            return String.format("https://vufind.mzk.cz/Record/MZK01-%s", controlField.get());
        }
    }
    
    private List<String> getValues(MarcIdentifier id) {
        MarcIdentifier fieldId = MarcIdentifier.fromString(contentDefinition.getField());
        
        if (fieldId.getField().equals(id.getField())) {
            return marcRecord
                    .getDataFields(fieldId.getField())
                    .stream()
                    .filter(dataField -> contentDefinition.getName().equals(dataField.getSubfield(fieldId.getSubfield()).orElse(null)))
                    .map(dataField -> dataField.getSubfield(id.getSubfield()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } else {
            return marcRecord
                    .getDataFields(id.getField())
                    .stream()
                    .map(dataField -> dataField.getSubfield(id.getSubfield()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        }
    }
    
}
