package cz.mzk.mapseries.update;

import cz.mzk.mapseries.dao.SheetDAO;
import cz.mzk.mapseries.oai.marc.MarcDataField;
import cz.mzk.mapseries.oai.marc.MarcIdentifier;
import cz.mzk.mapseries.oai.marc.MarcRecord;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        sheet.setAuthor(getAuthor());
        sheet.setOtherAuthors(getOtherAuthors());
        
        String digitalLibraryUrl = getDigitalLibraryUrl();
        String thumbnailUrl = getThubmnailUrl(digitalLibraryUrl);
        
        sheet.setDigitalLibraryUrl(digitalLibraryUrl);
        sheet.setThumbnailUrl(thumbnailUrl);
        sheet.setVufindUrl(getVufindUrl());
        
        return Optional.of(sheet);
    }
    
    private Optional<String> getId() {
        MarcIdentifier marcId = MarcIdentifier.fromString(contentDefinition.getSheets());

        Optional<String> sheetId = getValue(marcId);
        
        if (!sheetId.isPresent()) {
            return Optional.empty();
        }
        
        String result = applyGroovyTransformation(sheetId.get(), contentDefinition.getGroupBy());
        return Optional.of(result);
    }
    
    private String applyGroovyTransformation(String value, String script) {
        Binding binding = new Binding();
        binding.setVariable("field", value);
        GroovyShell shell = new GroovyShell(binding);
        return shell.evaluate(script).toString();
    }
    
    private String getTitle() {
        MarcIdentifier marcId = new MarcIdentifier.Builder().withField("245").withSubfield("a").build();
        
        return getValue(marcId).orElse("Unknown");
    }
    
    private String getYear() {
        MarcIdentifier marcId = new MarcIdentifier.Builder().withField("490").withSubfield("v").build();
        
        String year = getValue(marcId).orElse("");
        
        if (year.contains(",")) {
            int comma = year.indexOf(',');
            year = year.substring(comma + 1).trim();
        }
        return year;
    }
    
    private String getDigitalLibraryUrl() {
        MarcIdentifier marcId = new MarcIdentifier.Builder().withField("911").withSubfield("u").build();

        return getValue(marcId).orElse("");
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
    
    private String getAuthor() {
        List<Optional<String>> authorParts = new ArrayList<>();

        MarcIdentifier marcId110a = new MarcIdentifier.Builder().withField("110").withSubfield("a").build();
        MarcIdentifier marcId110b = new MarcIdentifier.Builder().withField("110").withSubfield("b").build();

        authorParts.add(getValue(marcId110a));
        authorParts.add(getValue(marcId110b));

        String author = join(" ", authorParts);

        if (!author.isEmpty()) {
            return author;
        }

        MarcIdentifier marcId100a = new MarcIdentifier.Builder().withField("100").withSubfield("a").build();
        MarcIdentifier marcId100d = new MarcIdentifier.Builder().withField("100").withSubfield("d").build();

        authorParts.clear();
        authorParts.add(getValue(marcId100a));
        authorParts.add(getValue(marcId100d));

        return join(" ", authorParts);
    }

    private String getOtherAuthors() {
        List<Optional<String>> parts1 = new ArrayList<>();
        List<Optional<String>> parts2 = new ArrayList<>();

        MarcIdentifier marcId710a = new MarcIdentifier.Builder().withField("710").withSubfield("a").build();
        MarcIdentifier marcId710b = new MarcIdentifier.Builder().withField("710").withSubfield("b").build();
        MarcIdentifier marcId700a = new MarcIdentifier.Builder().withField("700").withSubfield("a").build();
        MarcIdentifier marcId700d = new MarcIdentifier.Builder().withField("700").withSubfield("d").build();

        parts1.add(getValue(marcId710a));
        parts1.add(getValue(marcId710b));
        parts2.add(getValue(marcId700a));
        parts2.add(getValue(marcId700d));

        return join("; ", join(" ", parts1), join(" ", parts2));
    }

    private String join(String delimeter, List<Optional<String>> parts) {
        return parts
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining(delimeter));
    }

    private String join(String delimeter, String... parts) {
        return Stream.of(parts)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(delimeter));
    }
    
    private Optional<String> getValue(MarcIdentifier id) {
        List<String> values = getValues(id);

        if (values.isEmpty()) {
            return Optional.empty();
        }

        if (values.size() > 1) {
            log.println(String.format("[WARN] record contains more than one %s fields: %s", id, marcRecord));
        }

        return Optional.of(values.get(0));
    }
    
    private List<String> getValues(MarcIdentifier id) {
        MarcIdentifier fieldId = MarcIdentifier.fromString(contentDefinition.getField());
        
        if (fieldId.getField().equals(id.getField())) {
            return marcRecord
                    .getDataFields(fieldId.getField())
                    .stream()
                    .filter(dataField -> checkIndicators(dataField, id))
                    .filter(dataField -> contentDefinition.getName().equals(dataField.getSubfield(fieldId.getSubfield()).orElse(null)))
                    .map(dataField -> dataField.getSubfield(id.getSubfield()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } else {
            return marcRecord
                    .getDataFields(id.getField())
                    .stream()
                    .filter(dataField -> checkIndicators(dataField, id))
                    .map(dataField -> dataField.getSubfield(id.getSubfield()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        }
    }

    private boolean checkIndicators(MarcDataField dataField, MarcIdentifier id) {
        boolean result = true;

        if (id.getInd1().isPresent()) {
            result = dataField.getInd1().equals(id.getInd1());
        }
        if (id.getInd2().isPresent()) {
            result = result && dataField.getInd2().equals(id.getInd2());
        }
        return result;
    }
    
}
