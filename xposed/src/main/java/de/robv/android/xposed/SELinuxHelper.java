package de.robv.android.xposed;

import de.robv.android.xposed.services.BaseService;
import de.robv.android.xposed.services.DirectAccessService;

public final class SELinuxHelper {
  private static boolean sIsSELinuxEnabled;
  
  private static BaseService sServiceAppDataFile = (BaseService)new DirectAccessService();
  
  public static BaseService getAppDataFileService() {
    BaseService baseService = sServiceAppDataFile;
    return (BaseService)((baseService != null) ? baseService : new DirectAccessService());
  }
  
  public static String getContext() {
    return null;
  }
  
  public static boolean isSELinuxEnabled() {
    return sIsSELinuxEnabled;
  }
  
  public static boolean isSELinuxEnforced() {
    return sIsSELinuxEnabled;
  }
}
