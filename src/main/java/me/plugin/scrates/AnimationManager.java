package me.plugin.scrates;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationManager {

    private sCrates plugin;
    private Map<String, AnimationTask> activeTasks;

    public AnimationManager(sCrates plugin) {
        this.plugin = plugin;
        this.activeTasks = new HashMap<>();
    }

    public void startAnimation(Crate crate) {
        stopAnimation(crate.getName());

        if (crate.getBlockLocation() == null) return;
        AnimationData data = crate.getAnimationData();
        if (data == null || !data.hasAnimation()) return;

        AnimationTask task = new AnimationTask(plugin, crate);
        task.runTaskTimer(plugin, data.getTick(), data.getTick());
        activeTasks.put(crate.getName().toLowerCase(), task);
    }

    public void stopAnimation(String crateName) {
        AnimationTask task = activeTasks.remove(crateName.toLowerCase());
        if (task != null) {
            task.stop();
        }
    }

    public void stopAnimation(Crate crate) {
        stopAnimation(crate.getName());
    }

    public void stopLineAnimation(Crate crate, int lineIndex) {
        AnimationTask task = activeTasks.get(crate.getName().toLowerCase());
        if (task != null) {
            task.requestStopLine(lineIndex);
        }
    }

    public void restartAnimation(Crate crate) {
        startAnimation(crate);
    }

    public void stopAllAnimations() {
        for (AnimationTask task : activeTasks.values()) {
            task.stop();
        }
        activeTasks.clear();
    }

    public void restoreAllAnimations() {
        for (Crate crate : plugin.getCrateManager().getCrates().values()) {
            if (crate.getBlockLocation() != null) {
                AnimationData data = crate.getAnimationData();
                if (data != null && data.hasAnimation()) {
                    startAnimation(crate);
                }
            }
        }
    }

    public boolean isLineAnimating(Crate crate, int lineIndex) {
        AnimationTask task = activeTasks.get(crate.getName().toLowerCase());
        if (task == null) return false;
        return task.isLineAnimating(lineIndex);
    }

    public static class AnimationTask extends BukkitRunnable {

        private sCrates plugin;
        private Crate crate;
        private Map<Integer, Integer> lineIndices;
        private Map<Integer, List<CharSegment>> lineSegments;
        private Map<Integer, Integer> stopRequests;
        private boolean running;

        public AnimationTask(sCrates plugin, Crate crate) {
            this.plugin = plugin;
            this.crate = crate;
            this.lineIndices = new HashMap<>();
            this.lineSegments = new HashMap<>();
            this.stopRequests = new HashMap<>();
            this.running = true;
            buildSegments();
        }

        private void buildSegments() {
            AnimationData data = crate.getAnimationData();
            List<String> hologramLines = crate.getHologramLines();

            for (int lineIdx : data.getLines()) {
                if (lineIdx >= 0 && lineIdx < hologramLines.size()) {
                    String rawLine = hologramLines.get(lineIdx);
                    List<CharSegment> segs = parseSegments(rawLine);
                    lineSegments.put(lineIdx, segs);
                    lineIndices.put(lineIdx, 0);
                }
            }

            if (data.isDisplayNameAnimated()) {
                int displayLine = findDisplayNameLine(hologramLines);
                if (displayLine >= 0 && !lineSegments.containsKey(displayLine)) {
                    String rawLine = hologramLines.get(displayLine);
                    List<CharSegment> segs = parseSegments(rawLine);
                    lineSegments.put(displayLine, segs);
                    lineIndices.put(displayLine, 0);
                }
            }
        }

        private int findDisplayNameLine(List<String> hologramLines) {
            for (int i = 0; i < hologramLines.size(); i++) {
                if (hologramLines.get(i).contains("%displayName%")) {
                    return i;
                }
            }
            return -1;
        }

        public boolean isLineAnimating(int lineIndex) {
            return lineSegments.containsKey(lineIndex);
        }

        public void requestStopLine(int lineIndex) {
            if (lineSegments.containsKey(lineIndex)) {
                stopRequests.put(lineIndex, 1);
            }
        }

        @Override
        public void run() {
            if (!running) return;
            if (crate.getBlockLocation() == null) {
                stop();
                return;
            }

            AnimationData data = crate.getAnimationData();
            if (data == null || !data.hasAnimation()) {
                stop();
                return;
            }

            List<String> hologramLines = crate.getHologramLines();
            String animColor = data.getColor();
            String locKeyStr = plugin.getHologramManager().getLocKey(crate.getBlockLocation());
            List<ArmorStand> stands = plugin.getHologramManager().getStands(locKeyStr);

            List<Integer> toRemove = new ArrayList<>();

            for (Map.Entry<Integer, List<CharSegment>> entry : new HashMap<>(lineSegments).entrySet()) {
                int lineIdx = entry.getKey();
                List<CharSegment> segs = entry.getValue();

                if (lineIdx >= hologramLines.size()) continue;

                int currentIdx = lineIndices.get(lineIdx);
                String animated = buildAnimatedLine(segs, currentIdx, animColor);

                String resolved = crate.resolveHologramLine(animated);

                if (stands != null && lineIdx < stands.size() && stands.get(lineIdx) != null) {
                    stands.get(lineIdx).setCustomName(resolved);
                }

                int nextIdx = nextAnimatableIndex(segs, currentIdx);

                if (stopRequests.containsKey(lineIdx)) {
                    if (nextIdx <= currentIdx || isLastChar(segs, currentIdx)) {
                        String originalLine = crate.resolveHologramLine(hologramLines.get(lineIdx));
                        if (stands != null && lineIdx < stands.size() && stands.get(lineIdx) != null) {
                            stands.get(lineIdx).setCustomName(originalLine);
                        }
                        toRemove.add(lineIdx);
                        data.removeLine(lineIdx);
                    }
                }

                lineIndices.put(lineIdx, nextIdx);
            }

            for (int idx : toRemove) {
                lineSegments.remove(idx);
                lineIndices.remove(idx);
                stopRequests.remove(idx);
            }

            if (lineSegments.isEmpty()) {
                stop();
            }
        }

        private boolean isLastChar(List<CharSegment> segs, int idx) {
            if (segs == null || segs.isEmpty()) return true;
            int count = 0;
            for (CharSegment seg : segs) {
                if (!seg.isSpace()) {
                    if (count == idx) {
                        return count == getAnimatableCount(segs) - 1;
                    }
                    count++;
                }
            }
            return true;
        }

        private int getAnimatableCount(List<CharSegment> segs) {
            int count = 0;
            for (CharSegment seg : segs) {
                if (!seg.isSpace()) count++;
            }
            return count;
        }

        private int nextAnimatableIndex(List<CharSegment> segs, int currentIdx) {
            int count = getAnimatableCount(segs);
            if (count == 0) return 0;
            int next = currentIdx + 1;
            if (next >= count) next = 0;
            return next;
        }

        public void stop() {
            running = false;
            try {
                cancel();
            } catch (Exception ignored) {}
            plugin.getAnimationManager().activeTasks.remove(crate.getName().toLowerCase());
        }
    }

    static List<CharSegment> parseSegments(String line) {
        List<CharSegment> segments = new ArrayList<>();
        if (line == null || line.isEmpty()) return segments;

        String currentColor = "";
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        boolean magic = false;

        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);

            if (c == '&' || c == '\u00a7') {
                if (i + 1 < line.length()) {
                    char code = Character.toLowerCase(line.charAt(i + 1));
                    if (isColorCode(code)) {
                        if (isFormatCode(code)) {
                            if (code == 'l') bold = true;
                            else if (code == 'o') italic = true;
                            else if (code == 'n') underline = true;
                            else if (code == 'm') strikethrough = true;
                            else if (code == 'k') magic = true;
                            else if (code == 'r') {
                                bold = false; italic = false; underline = false;
                                strikethrough = false; magic = false;
                                currentColor = "";
                            }
                        } else {
                            currentColor = "&" + code;
                            bold = false; italic = false; underline = false;
                            strikethrough = false; magic = false;
                        }
                        i += 2;
                        continue;
                    } else {
                        segments.add(new CharSegment(c, currentColor, bold, italic, underline, strikethrough, magic));
                        i++;
                        continue;
                    }
                } else {
                    segments.add(new CharSegment(c, currentColor, bold, italic, underline, strikethrough, magic));
                    i++;
                    continue;
                }
            }

            segments.add(new CharSegment(c, currentColor, bold, italic, underline, strikethrough, magic));
            i++;
        }

        return segments;
    }

    private static boolean isColorCode(char c) {
        return "0123456789abcdefrlonmk".indexOf(c) >= 0;
    }

    private static boolean isFormatCode(char c) {
        return "rlonmk".indexOf(c) >= 0;
    }

    static String buildAnimatedLine(List<CharSegment> segments, int highlightIdx, String animColor) {
        StringBuilder sb = new StringBuilder();
        int animatableCount = 0;
        int currentAnimatable = 0;

        for (CharSegment seg : segments) {
            if (seg.isSpace()) {
                sb.append(seg.getCharacter());
                continue;
            }

            if (currentAnimatable == highlightIdx) {
                sb.append(animColor != null ? animColor : "");
                if (seg.isBold()) sb.append("&l");
                sb.append(seg.getCharacter());
            } else {
                sb.append(seg.getFullCodes());
                sb.append(seg.getCharacter());
            }
            currentAnimatable++;
            animatableCount++;
        }

        return sb.toString();
    }

    static class CharSegment {
        private char character;
        private String color;
        private boolean bold;
        private boolean italic;
        private boolean underline;
        private boolean strikethrough;
        private boolean magic;

        public CharSegment(char character, String color, boolean bold, boolean italic, boolean underline, boolean strikethrough, boolean magic) {
            this.character = character;
            this.color = color != null ? color : "";
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.strikethrough = strikethrough;
            this.magic = magic;
        }

        public char getCharacter() {
            return character;
        }

        public boolean isSpace() {
            return character == ' ';
        }

        public boolean isBold() {
            return bold;
        }

        public String getFullCodes() {
            StringBuilder sb = new StringBuilder();
            if (!color.isEmpty()) sb.append(color);
            if (bold) sb.append("&l");
            if (italic) sb.append("&o");
            if (underline) sb.append("&n");
            if (strikethrough) sb.append("&m");
            if (magic) sb.append("&k");
            return sb.toString();
        }
    }
}
