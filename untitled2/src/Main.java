import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Random;

public class Main {

    public static void main(String[] args) throws Exception {

        RandomAccessFile file = new RandomAccessFile("shared_memory.dat", "rw");
        FileChannel channel = file.getChannel();

        // enough space for 10 integers
        MappedByteBuffer buffer = channel.map(
                FileChannel.MapMode.READ_WRITE, 0, 1024);

        Random rand = new Random();
        long totalLatency = 0;

//        // ---------------- writing ---------------- java only
//        for (int i = 0; i < 10; i++) {
//            int number = rand.nextInt(1000);
//            System.out.println("Writing: " + number);
//            buffer.putInt(i * 4, number); // offset!
//        }
//        // ---------------- reading ---------------- java only
//        for (int i = 0; i < 10; i++) {
//            int value = buffer.getInt(i * 4); // read same slot
//            System.out.println("Read from memory: " + value);
//        }
//
//        file.close(); //java only end

        //  START PYTHON EXACTLY LIKE PIPE VERSION
        ProcessBuilder pb = new ProcessBuilder(
                "python", "-u", "pythonpart.py"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader log = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        new Thread(() -> {
            String line;
            try {
                while ((line = log.readLine()) != null) {
                    System.out.println("[PY] " + line);
                }
            } catch (Exception ignored) {}
        }).start();

        for (int i = 0; i < 100; i++) {


            int number = rand.nextInt(1000);

            System.out.println("Java sends: " + number);
            // acquire lock (blocks until Python releases)
            long start = System.nanoTime();
            //FileLock lock = channel.lock();

            // write input
            buffer.putInt(0, number);

            // set flag = 1 (data ready)
            buffer.putInt(8, 1);

            // wait for Python
            while (buffer.getInt(8) != 2) {
                Thread.onSpinWait();
            }

            int result = buffer.getInt(4);

            System.out.println("Java received: " + result);
            long end = System.nanoTime();

            long latency = end - start;
            totalLatency += latency;
            System.out.println("Result: " + result + " | Latency: " + latency + " ns");

            // reset flag
            buffer.putInt(8, 0);

            Thread.sleep(1);
        }
        System.out.println("Stopping Python...");
        System.out.println("Average latency: " + (totalLatency / 100) + " ns");
        // send stop signal
        buffer.putInt(8, 9);

        process.destroy();
        file.close();

    }
}