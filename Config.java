import java.io.FileInputStream;
import java.util.Properties;
import java.util.Arrays;

public class Config {
    private static Properties props = new Properties();

    static {
        try {
            props.load(new FileInputStream("variables.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Could not load config.properties", e);
        }
    }

    public static int getPort(String key) {
        return Integer.parseInt(props.getProperty(key));
    }

    public static int getNum(String key) {
        return Integer.parseInt(props.getProperty(key));
    }
}