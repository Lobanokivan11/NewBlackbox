package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import black.android.os.ServiceManager;
import android.content.Context;

public class ICrossProfileAppsProxy extends BinderInvocationStub {

    public ICrossProfileAppsProxy() {
        super(ServiceManager.get().getService(Context.CROSS_PROFILE_APPS_SERVICE));
    }

    @Override
    protected Object getWho() {
        try {
            return black.android.content.pm.ICrossProfileApps.Stub.asInterface.call(
                ServiceManager.get().getService(Context.CROSS_PROFILE_APPS_SERVICE)
            );
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
