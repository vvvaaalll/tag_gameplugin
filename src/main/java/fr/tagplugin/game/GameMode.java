package fr.tagplugin.game;
public enum GameMode {
    GLACON("Glacon"), LOUPS("Loups"), PATATE("Patate Chaude"),
    CHASSEUR("Chasseur"), ZOMBIE("Zombie"), BOUCLIER("Bouclier"), ROI("Roi de la Colline");
    private final String displayName;
    GameMode(String n) { this.displayName = n; }
    public String getDisplayName() { return displayName; }
}
