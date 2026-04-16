package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.IBinder;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;

public class ICrossProfileAppsProxy extends BinderInvocationStub {

    public ICrossProfileAppsProxy() {
        super(getSystemBinder());
    }

    private static IBinder getSystemBinder() {
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method getService = sm.getMethod("getService", String.class);
            return (IBinder) getService.invoke(null, Context.CROSS_PROFILE_APPS_SERVICE);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected Object getWho() {
        IBinder binder = getSystemBinder();
        try {
            Class<?> stubClass = Class.forName("android.content.pm.ICrossProfileApps$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            return asInterfaceMethod.invoke(null, binder);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.CROSS_PROFILE_APPS_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("canInteractAcrossProfiles")
    public static class CanInteractAcrossProfiles extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Возвращаем false, чтобы избежать SecurityException
            return false;
        }
    }

    @ProxyMethod("canRequestInteractAcrossProfiles")
    public static class CanRequestInteractAcrossProfiles extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return false;
        }
    }
}
