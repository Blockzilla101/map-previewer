package maphandler;

import java.io.File;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public class MapHandler {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("A map file needs to be specified");
            System.exit(1);
        }
    
        try {
            String path = args[0];

            System.out.println("Reading save " + path);
            long start = System.currentTimeMillis();

            Map map = new Map(path);
            ImageIO.write(map.image, "png", new File(path.replaceAll("\\.msav$", ".png")));

            long end = System.currentTimeMillis();

            System.out.println(map.toString(">"));
            for (int i = 0; i < map.stuffs.size; i++) {
                String key = (String)map.stuffs.keys().toArray().get(i);
                String val = (String)map.stuffs.values().toArray().get(i);

                System.out.println(">" + key + "=" + val);
            }
      
            System.out.println();
            long elapsed = end - start;
            System.out.printf("Took %d.%ds%n",
                TimeUnit.MILLISECONDS.toSeconds(elapsed),
                elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed))
            );

        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }
}
