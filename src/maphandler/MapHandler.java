package maphandler;

import javax.imageio.*;
import java.io.*;

public class MapHandler {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("A map file needs to be specified");
            System.exit(1);
        }

        try {
            String path = args[0];

            Map map = new Map(path);
            ImageIO.write(map.image, "png", new File(path.replaceAll("\\.msav$", ".png")));

        } catch (IOException ignored) {

        }
    }
}
