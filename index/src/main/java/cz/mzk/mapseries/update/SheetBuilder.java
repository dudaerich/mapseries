package cz.mzk.mapseries.update;

import cz.mzk.mapseries.dao.SheetDAO;
import cz.mzk.mapseries.oai.marc.MarcIdentifier;
import cz.mzk.mapseries.oai.marc.MarcRecord;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.io.PrintStream;
import java.util.Optional;

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
        MarcIdentifier marcId = MarcIdentifier.fromString(contentDefinition.getSheets());
        Optional<String> sheetId = marcRecord.get(marcId);
        
        if (sheetId.isPresent()) {
            String result = applyGroovyTransformation(sheetId.get(), contentDefinition.getGroupBy());
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }
    
    private String applyGroovyTransformation(String value, String script) {
        Binding binding = new Binding();
        binding.setVariable("field", value);
        GroovyShell shell = new GroovyShell(binding);
        return shell.evaluate(script).toString();
    }
    
    private String getTitle() {
        MarcIdentifier marcId = new MarcIdentifier("245", "a");
        return marcRecord.get(marcId).orElse("Unknown");
    }
    
    private String getYear() {
        MarcIdentifier marcId = new MarcIdentifier("490", "v");
        String year = marcRecord.get(marcId).orElse("");
        if (year.contains(",")) {
            int comma = year.indexOf(',');
            year = year.substring(comma + 1).trim();
        }
        return year;
    }
    
    private String getDigitalLibraryUrl() {
        MarcIdentifier marcId = new MarcIdentifier("911", "u");
        return marcRecord.get(marcId).orElse("");
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
        if (!marcRecord.hasControlField("001")) {
            log.println(String.format("[WARN] following record has no controlfield 001: %s", marcRecord));
            return "";
        } else {
            return String.format("https://vufind.mzk.cz/Record/MZK01-%s", marcRecord.getControlField("001"));
        }
    }
    
}
