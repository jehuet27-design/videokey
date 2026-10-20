import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FixVkPackMetadata {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java FixVkPackMetadata <packFile> <tsvMapFile> <outputPackFile>");
            return;
        }

        Path packFile = Paths.get(args[0]);
        Path tsvFile = Paths.get(args[1]);
        Path outFile = Paths.get(args[2]);

        // 1. Read TSV mapping
        Map<String, String[]> map = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(tsvFile)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\t");
                if (p.length >= 3) {
                    map.put(p[0].trim(), new String[]{p[1].trim(), p[2].trim()});
                    map.put(p[0].trim().replaceFirst("^0+", ""), new String[]{p[1].trim(), p[2].trim()});
                }
            }
        }
        System.out.println("Loaded " + (map.size() / 2) + " mappings from " + tsvFile);

        // 2. Read pack items
        java.nio.channels.FileChannel[] chBox = new java.nio.channels.FileChannel[1];
        List<VkPackBuilder.Item> items = VkPackBuilder.readPackItems(packFile, chBox);
        System.out.println("Read " + items.size() + " items from " + packFile);

        int updatedCount = 0;
        for (VkPackBuilder.Item it : items) {
            String[] target = map.get(it.code);
            if (target != null) {
                it.title = target[0];
                it.artist = target[1];
                updatedCount++;
            } else if (it.artist.equalsIgnoreCase("Unknown") || it.artist.trim().isEmpty()) {
                it.artist = "OPM / Videoke Hits";
                updatedCount++;
            }
        }
        System.out.println("Updated " + updatedCount + " items with genuine artists!");

        // 3. Write new pack
        VkPackBuilder.writePack(outFile, items);
        chBox[0].close();
        System.out.println("Successfully written updated pack to " + outFile);
    }
}
