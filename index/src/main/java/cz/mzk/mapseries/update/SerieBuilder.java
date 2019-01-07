package cz.mzk.mapseries.update;

import cz.mzk.mapseries.dao.SerieDAO;

/**
 * @author Erich Duda <dudaerich@gmail.com>
 */
public class SerieBuilder {
    
    private final ContentDefinition contentDefinition;
    
    public SerieBuilder(ContentDefinition contentDefinition) {
        this.contentDefinition = contentDefinition;
    }
    
    public SerieDAO buildSerie() {
        SerieDAO serie = new SerieDAO();
        serie.setName(getSerieName());
        serie.setGrid(contentDefinition.getGrid());
        serie.setThumbnailUrl(contentDefinition.getThumbnailUrl());
        return serie;
    }
    
    private String getSerieName() {
        return contentDefinition.getName().replaceAll("[\\[\\];]", "").trim();
    }
}
