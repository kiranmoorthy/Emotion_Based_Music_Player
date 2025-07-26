import ui.MusicPlayerUI;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


public class MainApp {
    public static void main(String[] args) {


        try{
            String pythonScriptPath = "python/app.py";

            ProcessBuilder pythonRunner = new ProcessBuilder("python",pythonScriptPath);
            pythonRunner.redirectErrorStream(true);

            Process process = pythonRunner.start();
            System.out.println("Python script launched successfully.");

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[PYTHON] " + line);
            }

//            int exitCode = process.waitFor();
//            System.out.println("Python script finished with exit code: " + exitCode);


        } catch (IOException e){//| InterruptedException e) {
            System.out.println("Failed to run Python script:");
            e.printStackTrace();

        }


        String filePath = "shared/current_emotion.txt";
        // Initialize emotion to a default value.
        // It's important that 'emotion' is either declared final
        // or its value is not changed after its first assignment
        // before being used in the lambda expression.
        String emotionFromFile = "Unknown"; // Default emotion if file reading fails

        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
            emotionFromFile = fileContent.trim(); // Assign the read emotion
            System.out.println("Successfully read the file.");
            System.out.println("The emotion from the file is: '" + emotionFromFile + "'");
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
            e.printStackTrace();
            // If an error occurs, emotionFromFile will retain its default value "Unknown"
        }

        // Create a final or effectively final copy of the emotion string
        // for use within the lambda expression.
        // This explicitly makes the variable immutable for the lambda's scope.
        final String finalEmotion = emotionFromFile;

        SwingUtilities.invokeLater(() -> {
            // Use the finalEmotion variable in the lambda
            MusicPlayerUI playerUI = new MusicPlayerUI(finalEmotion);
            playerUI.setVisible(true);
        });
    }
}
