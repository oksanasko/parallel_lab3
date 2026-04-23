
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public class Main {

    public static void main(String[] args) throws Exception {

        // start python process
        ProcessBuilder pb = new ProcessBuilder("python", "server.py");
        pb.inheritIO();
        pb.start();

        Thread.sleep(1000); // wait for server

        Socket socket = new Socket("127.0.0.1", 5000);

        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        Random rand = new Random();
        long totalLatency = 0;

        for (int i = 0; i < 10; i++) {

            int number = rand.nextInt(1000);
            System.out.println("Java sends: " + number);
            long start = System.nanoTime();

            // send int
            out.write(new byte[] {
                    (byte)(number >> 24),
                    (byte)(number >> 16),
                    (byte)(number >> 8),
                    (byte)(number)
            });

            // receive result
            byte[] buffer = new byte[4];
            in.read(buffer);
            int result =
                    ((buffer[0] & 0xFF) << 24) |
                            ((buffer[1] & 0xFF) << 16) |
                            ((buffer[2] & 0xFF) << 8) |
                            (buffer[3] & 0xFF);

            System.out.println("Java received: " + result);
            long end = System.nanoTime();

            long latency = end - start;
            System.out.println("Result: " + result + " | Latency: " + latency + " ns");
            totalLatency += latency;

            Thread.sleep(1);
        }
        System.out.println("Average latency: " + (totalLatency / 10) + " ns");
        socket.close();
    }
}