package gt.edu.url.descensogt.model;

import lombok.Data;

@Data
public class FeatureVector {

    private String equipo;
    private double ptsRate;
    private double winRate;
    private double drawRate;
    private double lossRate;
    private double gfRate;
    private double gcRate;
    private double diffRate;
    private String label;
}