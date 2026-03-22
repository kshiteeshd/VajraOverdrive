package campaign;

import java.util.HashMap;
import java.util.Map;

public class RegionStoryRegistry {

    private static final Map<String, String[]> stories = new HashMap<>();

    static {
        stories.put("RED", new String[]{
                "RED SECTOR",
                "Initial patrol units detected.",
                "Standard formations only.",
                "Learn their patterns."
        });

        stories.put("YELLOW", new String[]{
                "YELLOW SECTOR",
                "Faster units scrambled ahead.",
                "Erratic movement patterns reported.",
                "Stay sharp."
        });

        stories.put("BLUE", new String[]{
                "BLUE SECTOR",
                "Heavy armour units confirmed.",
                "Standard weapons may not be enough.",
                "Aim for weak points."
        });

        stories.put("GREEN", new String[]{
                "GREEN SECTOR",
                "Long-range targeting systems active.",
                "Enemy snipers have acquired your position.",
                "Keep moving."
        });

        stories.put("WHITE", new String[]{
                "WHITE SECTOR",
                "Enemy command zone.",
                "Elite units. All types. Maximum threat.",
                "End this."
        });
    }

    public static String[] getStory(String region) {
        return stories.get(region);
    }
}
