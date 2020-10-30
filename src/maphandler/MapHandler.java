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
            boolean makeScreenshot = true;
            if (args.length >= 2) {
                String arg = args[1];
                if (arg.equals("true") || arg.equals("1")) {
                    makeScreenshot = true;
                } else if (arg.equals("false") || arg.equals("0")) {
                    makeScreenshot = false;
                } else {
                    System.out.println("Ignoring 2nd argument");
                }
            }

            System.out.println("Reading save " + path);
            long start = System.currentTimeMillis();

            Map map = new Map(path);
            if (makeScreenshot) ImageIO.write(map.image, "png", new File(path.replaceAll("\\.msav$", ".png")));

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
