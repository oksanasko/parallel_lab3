import java.io.*;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws Exception {

        ProcessBuilder pb = new ProcessBuilder("python", "-u", "pipe_worker.py");

        Process process = pb.start();

        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream()));

        BufferedReader stdout = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        BufferedReader stderr = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));

        // Thread to print logs (stderr)
        new Thread(() -> {
            String line;
            try {
                while ((line = stderr.readLine()) != null) {
                    System.err.println("[Log from python file] " + line);
                }
            } catch (Exception ignored) {}
        }).start();

        Random rand = new Random();
        long totalLatency = 0;

        for (int i = 0; i < 10; i++) {
            long start = System.nanoTime();
            int number = rand.nextInt(1000);

            System.out.println("Sent: " + number);

            writer.write(number + "\n");
            writer.flush();

            String response = stdout.readLine();

            if (response == null) break;

            System.out.println("Received back: " + response);
            long end = System.nanoTime();

            long latency = end - start;
            totalLatency += latency;
            System.out.println("Result: " + response + " | Latency: " + latency + " ns");
        }
        System.out.println("Average latency: " + (totalLatency / 10) + " ns");

        writer.close();
        process.destroy();
    }
}