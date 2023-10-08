import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CanbusDataProcessor {
    public static void main(String[] args) {
        try {
            // Get the current working directory
            String currentDirectory = System.getProperty("user.dir");

            // List and prompt for available files
            List<String> availableFiles = listFiles(currentDirectory);
            Scanner scanner = new Scanner(System.in);

            // Prompt for the RAW file
            System.out.println("Available files in the current directory:");
            for (int i = 0; i < availableFiles.size(); i++) {
                System.out.println((i + 1) + " " + availableFiles.get(i));
            }
            System.out.print("Enter the number of the RAW file (before decoding): ");
            int rawFileNumber = scanner.nextInt();
            String rawFilePath = availableFiles.get(rawFileNumber - 1);

            // Prompt for the decoded CSV file
            System.out.print("Enter the number of the decoded CSV file: ");
            int decodedFileNumber = scanner.nextInt();
            String decodedFilePath = availableFiles.get(decodedFileNumber - 1);

            // Prompt for the lap_id
            System.out.print("Enter the lap_id: ");
            int lapId = scanner.nextInt();

            // Process the RAW data
            List<RawData> rawDataSet = processRawData(rawFilePath);
            System.out.println("Processing of RAW data completed.");
            String rawRandom = "Random raw line: " + rawDataSet.stream().findAny().get().getRawData();
            System.out.println(rawRandom);

            // Process the decoded CSV data
            List<DecodedData> decodedDataSet = processDecodedCsv(decodedFilePath);
            System.out.println("Processing of decoded CSV data completed.");
            String decodedRandom = "Random decoded line: " + decodedDataSet.stream().findAny().get().getData().toString();
            System.out.println(decodedRandom);

            // Now you have both datasets as objects
            // You can further work with rawDataSet and decodedDataSet here

            // After processing the raw and decoded data
            String outputPath = "output.csv"; // Specify the path for the output CSV file
            writeOutputCsv(rawDataSet, decodedDataSet, lapId, outputPath);
            System.out.println("Output CSV file created at: " + outputPath);

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private static void writeOutputCsv(List<RawData> rawDataSet, List<DecodedData> decodedDataSet, int lapId, String outputPath) throws IOException {
        // Check if the output file exists and delete it if it does
        File outputFile = new File(outputPath);
        if (outputFile.exists()) {
            if (outputFile.delete()) {
                System.out.println("Existing output CSV file deleted.");
            } else {
                System.err.println("Failed to delete existing output CSV file.");
            }
        }

        // Create the new output CSV file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // Write the header row
            List<String> headerRow = new ArrayList<>();
            headerRow.add("lap_id");
            headerRow.add("timestamp");
            headerRow.add("canbus_id");
            headerRow.add("value");
            headerRow.add("canbus_id_name");
            headerRow.add("unit");

            writer.write(String.join(",", headerRow));
            writer.newLine();

            String timestampDate = rawDataSet.stream().filter(rawData -> rawData.rawData.contains("# Time: ")).findFirst().get().rawData;

            // Define a regular expression pattern to match the date part
            Pattern pattern = Pattern.compile("\\d{8}T\\d{6}");

            // Create a Matcher to find the pattern in the timestampDate string
            Matcher matcher = pattern.matcher(timestampDate);

            if (matcher.find()) {
                timestampDate = matcher.group();
                String outputPattern = "yyyyMMdd'T'HHmmss";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(outputPattern);
                LocalDateTime dateTime = LocalDateTime.parse(timestampDate, formatter);
                System.out.println("Extracted date: " + dateTime);
            } else {
                System.out.println("Date not found in the input.");
            }
            String timestamp2 = "1696779.278161";
            double timestampValue = Double.parseDouble(timestamp2);
            System.out.println("Parsed timestamp 2: " + timestampValue);
            System.out.println("timestampDate: " + timestampDate);
            Map<String, String> finalData = new HashMap<>();


            // Write data from rawDataSet and corresponding data from decodedDataSet
            int dataSize = Math.min(rawDataSet.size(), decodedDataSet.size());
            for (int i = 0; i < dataSize; i++) {
                RawData raw = rawDataSet.get(i);
                DecodedData decoded = decodedDataSet.get(i);

                List<String> rowData = new ArrayList<>();
                rowData.add(String.valueOf(lapId));
                rowData.addAll(decoded.getData());

                // Replace "your_data_here" with the actual data you want to include
                writer.write(String.join(",", rowData));
                writer.newLine();
            }
        }
    }

    private static List<String> listFiles(String directoryPath) {
        List<String> files = new ArrayList<>();
        File directory = new File(directoryPath);
        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile()) {
                    files.add(file.getName());
                }
            }
        }
        return files;
    }

    private static List<DecodedData> processDecodedCsv(String decodedFilePath) throws IOException {
        List<DecodedData> decodedDataList = new ArrayList<>();
        Path path = Paths.get(decodedFilePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            CSVParser csvParser = CSVParser.parse(reader, CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord());

            List<String> headers = csvParser.getHeaderNames();
            System.out.println("Processing decoded CSV data from file: " + decodedFilePath);

            for (CSVRecord csvRecord : csvParser) {
                List<String> data = new ArrayList<>();
                for (String header : headers) {
                    data.add(csvRecord.get(header));
                }
                DecodedData decodedData = new DecodedData(data);
                decodedDataList.add(decodedData);
            }
        }
        return decodedDataList;
    }

    private static List<RawData> processRawData(String rawFilePath) throws IOException {
        List<RawData> rawdataList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(rawFilePath))) {
            String line;
            int lineNumber = 0;
            System.out.println("Processing RAW data from file: " + rawFilePath);

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    if (!line.isEmpty()) {
                        RawData rawData = new RawData(line);
                        rawdataList.add(rawData);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing RAW data line " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        return rawdataList;
    }

    private static class DecodedData {
        private final List<String> data;

        public DecodedData(List<String> data) {
            this.data = data;
        }

        public List<String> getData() {
            return data;
        }
    }

    private static class RawData {
        private final String rawData;

        public RawData(String rawData) {
            this.rawData = rawData;
        }

        public String getRawData() {
            return rawData;
        }
    }

}
