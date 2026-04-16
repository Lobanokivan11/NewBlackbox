package de.robv.android.xposed;

public interface IXposedHookCmdInit extends IXposedMod {
  void initCmdApp(StartupParam paramStartupParam) throws Throwable;
  
  public static final class StartupParam {
    public String modulePath;
    
    public String startClassName;
  }
}
