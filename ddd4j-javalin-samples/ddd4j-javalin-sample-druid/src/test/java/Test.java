import com.google.inject.Guice;
import io.javalin.sample.TaskModule;
import io.javalin.sample.tasks.CoinCacheTask;

public class Test {
    public static void main(String[] args) {
        var guice = Guice.createInjector(new TaskModule("app.json"));
        var roiTask = guice.getInstance(CoinCacheTask.class);
        roiTask.run();
    }
}
