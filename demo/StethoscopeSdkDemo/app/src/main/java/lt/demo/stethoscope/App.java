package lt.demo.stethoscope;

import android.app.ActivityManager;
import android.app.Application;

import lt.demo.stethoscope.dfu.DfuProcessService;
import lt.sdk.stethoscope.StethoscopeTool;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        StethoscopeTool.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        StethoscopeTool.unInit();
    }

    public boolean isDfuServiceRunning() {
        final ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DfuProcessService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
