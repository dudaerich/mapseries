package cz.mzk.mapseries.update;

import cz.mzk.mapseries.dao.SheetDAO;
import cz.mzk.mapseries.oai.marc.MarcIdentifier;
import cz.mzk.mapseries.oai.marc.MarcRecord;
import cz.mzk.mapseries.oai.marc.MarcTraversal;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.io.PrintStream;
import java.util.Optional;

/**
 *
 * @author Erich Duda <dudaerich@gmail.com>
 */
public class SheetBuilder {

    private static final String INNER_DEL = " ";
    private static final String OUTER_DEL = "; ";

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
        sheet.setPublisher(getPublisher());
        sheet.setIssue(getIssue());
        sheet.setDescription(getDescription());
        sheet.setSignature(getSignature());
        
        String digitalLibraryUrl = getDigitalLibraryUrl();
        String thumbnailUrl = getThubmnailUrl(digitalLibraryUrl);
        
        sheet.setDigitalLibraryUrl(digitalLibraryUrl);
        sheet.setThumbnailUrl(thumbnailUrl);
        sheet.setVufindUrl(getVufindUrl());
        
        return Optional.of(sheet);
    }
    
    private Optional<String> getId() {
        MarcIdentifier marcId = MarcIdentifier.fromString(contentDefinition.getSheets());
        MarcTraversal marcTraversal = createMarcTraversal(marcId);

        Optional<String> sheetId = marcTraversal.getValue(marcId);
        
        if (!sheetId.isPresent()) {
            return Optional.empty();
        }

        try {
            String result = applyGroovyTransformation(sheetId.get(), contentDefinition.getGroupBy());
            return Optional.of(result);
        } catch (Exception e) {
            log.println(String.format("[WARN] applying script on Sheet ID failed because of exception %s. ID: %s; SCRIPT: %s; MarcRecord: %s",
                    e, sheetId.get(), contentDefinition.getGroupBy(), marcRecord));
        }
        return Optional.empty();
    }
    
    private String applyGroovyTransformation(String value, String script) {
        Binding binding = new Binding();
        binding.setVariable("field", value);
        GroovyShell shell = new GroovyShell(binding);
        return shell.evaluate(script).toString();
    }
    
    private String getTitle() {
        MarcIdentifier marcId = new MarcIdentifier.Builder().withField("245").withSubfield("a").build();
        MarcTraversal marcTraversal = createMarcTraversal(marcId);
        
        return marcTraversal.getValue(marcId).orElse("Unknown");
    }
    
    private String getYear() {
        MarcIdentifier marcId = new MarcIdentifier.Builder().withField("490").withSubfield("v").build();
        MarcTraversal marcTraversal = createMarcTraversal(marcId);
        
        String year = marcTraversal.getValue(marcId).orElse("");
        
        if (year.contains(",")) {
            int comma = year.indexOf(',');
            year = year.substring(comma + 1).trim();
        }
        return year;
    }
    
    private String getDigitalLibraryUrl() {
        MarcIdentifier marcId = new MarcIdentifier.Builder().withField("911").withSubfield("u").build();
        MarcTraversal marcTraversal = createMarcTraversal(marcId);

        return marcTraversal.getValue(marcId).orElse("");
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
        MarcTraversal marcTraversal = createMarcTraversal();

        MarcIdentifier marc110 = new MarcIdentifier.Builder().withField("110").withSubfield("a").withSubfield("b").withDelimiter(INNER_DEL).build();
        MarcIdentifier marc100 = new MarcIdentifier.Builder().withField("100").withSubfield("a").withSubfield("d").withDelimiter(INNER_DEL).build();

        String author = marcTraversal.getValueAsString(OUTER_DEL, marc110).orElse("");

        if (!author.isEmpty()) {
            return author;
        }

        return marcTraversal.getValueAsString(OUTER_DEL, marc100).orElse("");
    }

    private String getOtherAuthors() {
        MarcTraversal marcTraversal = createMarcTraversal();

        MarcIdentifier marc710 = new MarcIdentifier.Builder().withField("710").withSubfield("a").withSubfield("b").withDelimiter(INNER_DEL).build();
        MarcIdentifier marc700 = new MarcIdentifier.Builder().withField("700").withSubfield("a").withSubfield("d").withDelimiter(INNER_DEL).build();

        return marcTraversal.getValueAsString(OUTER_DEL, marc710, marc700).orElse("");
    }

    private String getPublisher() {
        MarcIdentifier marcId = new MarcIdentifier.Builder()
                .withField("264")
                .withIndicator2("1")
                .withSubfield("a")
                .withSubfield("b")
                .withSubfield("c")
                .withDelimiter(INNER_DEL)
                .build();

        MarcTraversal marcTraversal = createMarcTraversal(marcId);

        String value = marcTraversal.getValueAsString(OUTER_DEL, marcId).orElse(null);

        if (value != null) {
            return value;
        }

        marcId = new MarcIdentifier.Builder()
                .withField("260")
                .withSubfield("a")
                .withSubfield("b")
                .withSubfield("c")
                .withDelimiter(INNER_DEL)
                .build();

        marcTraversal = createMarcTraversal(marcId);

        return marcTraversal.getValueAsString(OUTER_DEL, marcId).orElse("");
    }

    private String getIssue() {
        MarcIdentifier marcId = new MarcIdentifier.Builder().withField("250").withSubfield("a").build();
        MarcTraversal marcTraversal = createMarcTraversal(marcId);

        return marcTraversal.getValueAsString(OUTER_DEL, marcId).orElse("");
    }

    private String getDescription() {
        MarcIdentifier marcId = new MarcIdentifier.Builder().withField("300")
                .withSubfield("a").withSubfield("b").withSubfield("c").withDelimiter(INNER_DEL).build();
        MarcTraversal marcTraversal = createMarcTraversal(marcId);

        return marcTraversal.getValueAsString(OUTER_DEL, marcId).orElse("");
    }

    private String getSignature() {
        MarcIdentifier marcId = new MarcIdentifier.Builder().withField("910").withSubfield("b").build();
        MarcTraversal marcTraversal = createMarcTraversal(marcId);

        return marcTraversal.getValueAsString(OUTER_DEL, marcId).orElse("");
    }

    private MarcTraversal createMarcTraversal() {
        return createMarcTraversal(null);
    }

    private MarcTraversal createMarcTraversal(MarcIdentifier identifier) {
        MarcTraversal.Builder builder = new MarcTraversal.Builder()
                .withMarcRecord(marcRecord)
                .withLogHandler(log::println);

        MarcIdentifier fieldId = MarcIdentifier.fromString(contentDefinition.getField());

        if (identifier != null && fieldId.getField().equals(identifier.getField())) {
            builder.withDataFieldPredicate(dataField -> contentDefinition.getName().equals(dataField.getSubfield(fieldId.getSubfields().get(0)).orElse(null)));
        }

        return builder.build();
    }
    
}
