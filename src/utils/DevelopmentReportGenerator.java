package utils;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DevelopmentReportGenerator {

    private static final String SRC_PATH = "src";
    private static final String ASSET_PATH = "assets";

    private static int packageCount = 0;
    private static int javaFileCount = 0;
    private static int lineCount = 0;

    private static int entityCount = 0;
    private static int configCount = 0;
    private static int uiCount = 0;

    public static void main(String[] args) {

        try {

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm"));

            File docs = new File("docs");
            docs.mkdirs();

            File report = new File(docs,
                    "engine_report_" + timestamp + ".txt");

            FileWriter writer = new FileWriter(report);

            writer.write(generateReport());

            writer.close();

            System.out.println("Engine report generated:");
            System.out.println(report.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String generateReport() {

        StringBuilder report = new StringBuilder();

        report.append("VAJRA OVERDRIVE ENGINE REPORT\n");
        report.append("=================================\n\n");

        report.append("Generated: ")
                .append(LocalDateTime.now())
                .append("\n\n");

        File src = new File(SRC_PATH);

        report.append("PROJECT STRUCTURE\n");
        report.append("-------------------\n");

        scanDirectory(src, report, 0);

        report.append("\n");

        report.append("PROJECT STATISTICS\n");
        report.append("-------------------\n");

        report.append("Packages: ").append(packageCount).append("\n");
        report.append("Java Files: ").append(javaFileCount).append("\n");
        report.append("Lines of Code: ").append(lineCount).append("\n\n");

        report.append("SYSTEM DETECTION\n");
        report.append("-------------------\n");

        report.append("Entity Classes: ").append(entityCount).append("\n");
        report.append("Config Classes: ").append(configCount).append("\n");
        report.append("UI Classes: ").append(uiCount).append("\n\n");

        report.append("ASSET REPORT\n");
        report.append("-------------------\n");

        report.append(scanAssets());

        report.append("\n");

        report.append("END OF REPORT\n");

        return report.toString();
    }

    private static void scanDirectory(File folder, StringBuilder report, int depth) {

        if (!folder.exists()) return;

        File[] files = folder.listFiles();

        if (files == null) return;

        for (File file : files) {

            for(int i=0;i<depth;i++)
                report.append("  ");

            if (file.isDirectory()) {

                report.append("[PACKAGE] ")
                        .append(file.getName())
                        .append("\n");

                packageCount++;

                scanDirectory(file, report, depth + 1);

            } else if (file.getName().endsWith(".java")) {

                report.append("- ")
                        .append(file.getName())
                        .append("\n");

                javaFileCount++;

                countLines(file);

                detectClassType(file.getName());
            }
        }
    }

    private static void countLines(File file) {

        try(BufferedReader reader =
                    new BufferedReader(new FileReader(file))) {

            while(reader.readLine()!=null)
                lineCount++;

        } catch(Exception ignored){}
    }

    private static void detectClassType(String name) {

        if(name.contains("Entity"))
            entityCount++;

        if(name.contains("Config"))
            configCount++;

        if(name.contains("UI"))
            uiCount++;
    }

    private static String scanAssets(){

        File assets = new File(ASSET_PATH);

        if(!assets.exists())
            return "No assets folder detected\n";

        int assetCount = countFiles(assets);

        return "Total Assets: " + assetCount + "\n";
    }

    private static int countFiles(File folder){

        int count = 0;

        File[] files = folder.listFiles();

        if(files == null) return 0;

        for(File file : files){

            if(file.isDirectory())
                count += countFiles(file);

            else
                count++;
        }

        return count;
    }
}