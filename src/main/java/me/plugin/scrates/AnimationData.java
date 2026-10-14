package me.plugin.scrates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnimationData {

    private String color;
    private int tick;
    private Set<Integer> lines;
    private boolean displayNameOnly;
    private boolean displayNameAnimated;

    public AnimationData() {
        this.color = null;
        this.tick = 10;
        this.lines = new HashSet<>();
        this.displayNameOnly = false;
        this.displayNameAnimated = false;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean hasColor() {
        return color != null && !color.isEmpty();
    }

    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
    }

    public Set<Integer> getLines() {
        return lines;
    }

    public void setLines(Set<Integer> lines) {
        this.lines = lines != null ? lines : new HashSet<>();
    }

    public boolean isLineAnimated(int line) {
        return lines.contains(line);
    }

    public void addLine(int line) {
        lines.add(line);
    }

    public void removeLine(int line) {
        lines.remove(line);
    }

    public boolean isDisplayNameOnly() {
        return displayNameOnly;
    }

    public void setDisplayNameOnly(boolean displayNameOnly) {
        this.displayNameOnly = displayNameOnly;
    }

    public boolean isDisplayNameAnimated() {
        return displayNameAnimated;
    }

    public void setDisplayNameAnimated(boolean displayNameAnimated) {
        this.displayNameAnimated = displayNameAnimated;
    }

    public boolean hasAnimation() {
        return hasColor() && (!lines.isEmpty() || displayNameAnimated);
    }

    public void reset() {
        this.color = null;
        this.tick = 10;
        this.lines.clear();
        this.displayNameOnly = false;
        this.displayNameAnimated = false;
    }

    public List<Integer> getSortedLines() {
        List<Integer> sorted = new ArrayList<>(lines);
        java.util.Collections.sort(sorted);
        return sorted;
    }
}
