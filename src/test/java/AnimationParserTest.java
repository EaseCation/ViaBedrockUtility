import com.google.gson.JsonParser;
import org.oryxel.viabedrockutility.animation.Animation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class AnimationParserTest {
    public static void main(String[] args) {
        if (args.length < 1) {
            return;
        }

        if (!args[0].endsWith("\\") && !args[0].endsWith("/")) {
            args[0] = args[0] + "\\";
        }

        final File dir = new File("test");
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }

        final Path testPath = Paths.get(args[0]);

        try (final Stream<Path> stream = Files.walk(testPath)) {
            List<File> files = stream.filter(Files::isRegularFile).map(Path::toFile).toList();

            for (final File file : files) {
                String path = file.getAbsolutePath().replace(args[0].replace("/", "\\"), "");

                if (!path.startsWith("animations\\") || !path.toLowerCase().endsWith(".json")) {
                    continue;
                }

                final String content = new String(Files.readAllBytes(file.toPath()));
                Animation.parse(JsonParser.parseString(content).getAsJsonObject()).forEach(System.out::println);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
