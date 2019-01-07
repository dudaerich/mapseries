package cz.mzk.mapseries.update;

import cz.mzk.mapseries.managers.UpdateTaskManager;
import cz.mzk.mapseries.github.GithubService;
import cz.mzk.mapseries.github.GithubServiceUnauthorized;
import cz.mzk.mapseries.jsf.beans.Configuration;
import cz.mzk.mapseries.oai.marc.MarcRecord;
import cz.mzk.mapseries.oai.marc.OaiMarcXmlReader;
import cz.mzk.mapseries.dao.SerieDAO;
import cz.mzk.mapseries.dao.SheetDAO;
import cz.mzk.mapseries.dao.UpdateTaskDAO;
import cz.mzk.mapseries.oai.marc.MarcIdentifier;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Lock;
import static javax.ejb.LockType.READ;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.jms.TextMessage;
import org.jboss.logging.Logger;

/**
 * @author Erich Duda <dudaerich@gmail.com>
 */
@Singleton
public class UpdateEJB {
    
    private static final Logger LOG = Logger.getLogger(UpdateEJB.class);
    
    private static final String TASK_ID_KEY = "taskId";
    
    private volatile UpdateTaskDAO runningTask = null;
    
    @EJB
    private UpdateTaskManager updateTaskManager;
    
    @Inject
    private JMSContext context;
    
    @Inject
    private GithubService githubService;
    
    @Inject
    private GithubServiceUnauthorized githubServiceUnauthorized;

    @Resource(lookup = "java:/jms/queue/UpdateTasks")
    private Queue queue;
    
    private PrintStream log = null;
    
    @Lock(READ)
    public void scheduleUpdateTask() throws Exception {
        
        List<UpdateTaskDAO> unfinishedTasks = updateTaskManager.getUnfinishedTasks();
        if (unfinishedTasks.size() >= 2) {
            return;
        }
        
        UpdateTaskDAO updateTaskDAO = new UpdateTaskDAO();
        updateTaskManager.persistTask(updateTaskDAO);
        
        TextMessage msg = context.createTextMessage(githubService.loadFile("/" + Configuration.CONTENT_DEFINITION_PATH));
        msg.setLongProperty(TASK_ID_KEY, updateTaskDAO.getId());
        context.createProducer().send(queue, msg);
    }
    
    @Schedule(second = "0", minute = "0", hour = "3", persistent = false)
    public void scheduledAutomatically() {
        try {
            List<UpdateTaskDAO> unfinishedTasks = updateTaskManager.getUnfinishedTasks();
            if (unfinishedTasks.size() >= 2) {
                return;
            }

            UpdateTaskDAO updateTaskDAO = new UpdateTaskDAO();
            updateTaskManager.persistTask(updateTaskDAO);

            TextMessage msg = context.createTextMessage(githubServiceUnauthorized.loadFile(Configuration.CONTENT_DEFINITION_PATH));
            msg.setLongProperty(TASK_ID_KEY, updateTaskDAO.getId());
            context.createProducer().send(queue, msg);
        } catch (Exception e) {
            LOG.error("Error thrown when automatically scheduled the task.", e);
            throw new RuntimeException("Error thrown when automatically scheduled the task.", e);
        }
    }
    
    @Lock(READ)
    public synchronized void runUpdateTask(UpdateTaskDAO updateTaskDAO, String definitionJson, List<Object> output) {
        
        runningTask = updateTaskDAO;
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        log = new PrintStream(os);
        
        try {
            
            doUpdate(definitionJson, output);
            
            updateTaskDAO.setResult(true);
            
        } catch (Exception e) {
            
            updateTaskDAO.setResult(false);
            log.println("[TASK FAILED] " + e);
            e.printStackTrace(log);
            
        } finally {
            try {
                updateTaskDAO.setLog(os.toString("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                LOG.error("Storing of logs failed because of encoding exception.", e);
            }
            runningTask = null;
            log = null;
        }
    }
    
    @Lock(READ)
    public UpdateTaskDAO getRunningTask() {
        return runningTask;
    }
    
    private void doUpdate(String definitionJson, List<Object> output) throws Exception {
        log.println("Starting update.");
        
        List<ContentDefinition> definitions = ContentDefinition.readFromJSONArray(definitionJson);
        Map<ContentDefinition, SerieDAO> series = new HashMap<>();
        
        OaiMarcXmlReader oaiMarcXmlReader = new OaiMarcXmlReader("http://aleph.mzk.cz/OAI", "MZK01-MAPY");
        
        for (MarcRecord marcRecord : oaiMarcXmlReader) {

            Optional<ContentDefinition> definition = findDefinitionForRecord(definitions, marcRecord);
            if (!definition.isPresent()) {
                continue;
            }
            
            SerieDAO serie = series.get(definition.get());
            if (serie == null) {
                SerieBuilder builder = new SerieBuilder(definition.get());
                serie = builder.buildSerie();
                series.put(definition.get(), serie);
                output.add(serie);
            }
            
            SheetBuilder sheetBuilder = new SheetBuilder(definition.get(), marcRecord, log);
            Optional<SheetDAO> optSheetDAO = sheetBuilder.buildSheet();
            
            if (!optSheetDAO.isPresent()) {
                continue;
            }
            
            SheetDAO sheetDAO = optSheetDAO.get();
            sheetDAO.setSerie(serie);
            
            output.add(sheetDAO);
        }
        
        log.println("Update finished successfully");
    }
    
    private Optional<ContentDefinition> findDefinitionForRecord(List<ContentDefinition> definitions, MarcRecord marcRecord) throws Exception {
        
        for (ContentDefinition definition : definitions) {
            if (isDefinitionSuitableForRecord(definition, marcRecord)) {
                return Optional.of(definition);
            }
        }
        
        return Optional.empty();
    }
    
    private boolean isDefinitionSuitableForRecord(ContentDefinition definition, MarcRecord marcRecord) throws Exception {
        String field = definition.getField();
        MarcIdentifier marcId = MarcIdentifier.fromString(field);
        return marcRecord
                .getDataFields(marcId.getField())
                .stream()
                .map(dataField -> dataField.getSubfield(marcId.getSubfield()))
                .anyMatch(subfield -> subfield.isPresent() && subfield.get().equals(definition.getName()));
    }
    
}
