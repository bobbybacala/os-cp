package phase1;
import java.io.*;

public class BasicFileIO {
    public static void main(String[] args) {
        String inputFileName = "input.txt"; // Input file name
        String outputFileName = "output.txt"; // Output file name

        // Using try-with-resources to ensure resources are closed after use
        try (
            BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))
        ) {
            String line;

            // Read each line from the input file
            while ((line = reader.readLine()) != null) {
                // Write the line to the output file
                writer.write(line);
                writer.newLine(); // Add a new line to separate lines in output file
            }

            System.out.println("Contents successfully copied from " + inputFileName + " to " + outputFileName);
        } catch (IOException e) {
            // Handle any I/O errors
            e.printStackTrace();
        }
    }
}
