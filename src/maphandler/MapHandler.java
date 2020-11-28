package maphandler;

import arc.struct.Seq;

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
                if (arg.equals("false") || arg.equals("0")) {
                    System.out.println("Wont be generating a screenshot");
                    makeScreenshot = false;
                } else {
                    System.out.println("Ignoring 2nd argument");
                }
            }

            System.out.println("Reading save " + path + "\n");
            long start = System.currentTimeMillis();

            Map map = new Map(path);
            if (makeScreenshot) ImageIO.write(map.image, "png", new File(path.replaceAll("\\.msav$", ".png")));

            long end = System.currentTimeMillis();

            System.out.println(">name=" + map.name);
            System.out.println(">description=" + map.description);
            System.out.println(">author=" + map.author);

            System.out.println(">build=" + map.lastReadBuild);

            System.out.println(">width=" + map.width);
            System.out.println(">height=" + map.height);

            System.out.println(">mods=" + arrayToJson(map.mods));

            for (int i = 0; i < map.tags.size; i++) {
                String key = map.tags.keys().toSeq().get(i);
                String val = map.tags.values().toSeq().get(i);

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

    static String arrayToJson(Seq<String> arr) {
        StringBuilder json = new StringBuilder();

        for (int i = 0; i < arr.size; i++) {
            json.append("\"").append(arr.get(i).replace("\"", "\\\"")).append("\"");
            if (i != arr.size - 1) json.append(", ");

        }
        return "[" + json.toString() + "]";
    }
}
