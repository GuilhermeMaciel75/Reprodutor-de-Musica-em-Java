package support;

import java.time.Duration;

@SuppressWarnings("DuplicatedCode")
public class SecondsToString {

    public static String lengthToString(int length) {
        if (length < 0) {
            return "--:--";
        }
        if (length < 60) {
            // Up to 59 seconds
            return length + "s";
        }
        if (length < 3600) {
            // Up to 59:59
            Duration duration = Duration.ofSeconds(length);
            long MM = duration.toMinutes();
            long SS = duration.toSecondsPart();
            return String.format("%d:%02d", MM, SS);
        }
        // More than 59:59
        Duration duration = Duration.ofSeconds(length);
        long HH = duration.toHours();
        long MM = duration.toMinutesPart();
        long SS = duration.toSecondsPart();
        return String.format("%d:%02d:%02d", HH, MM, SS);
    }

    public static String currentTimeToString(int currentTime, int totalTime) {
        if (currentTime > totalTime | currentTime < 0) {
            return "--:--";
        }
        if (totalTime < 60) {
            // Up to 59 seconds
            return currentTime + "s";
        }
        if (totalTime < 3600) {
            // Up to 59:59
            Duration duration = Duration.ofSeconds(currentTime);
            long MM = duration.toMinutes();
            long SS = duration.toSecondsPart();
            return String.format("%d:%02d", MM, SS);
        }
        // More than 59:59
        Duration duration = Duration.ofSeconds(currentTime);
        long HH = duration.toHours();
        long MM = duration.toMinutesPart();
        long SS = duration.toSecondsPart();
        return String.format("%d:%02d:%02d", HH, MM, SS);
    }
}
