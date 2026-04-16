package de.robv.android.xposed;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import de.robv.android.xposed.services.FileResult;

public final class XSharedPreferences implements SharedPreferences {
    private static final String TAG = "XSharedPreferences";
    private final File mFile;
    private final String mFilename;
    private long mFileSize;
    private long mLastModified;
    private boolean mLoaded = false;
    private Map<String, Object> mMap;

    public XSharedPreferences(File paramFile) {
        this.mFile = paramFile;
        this.mFilename = paramFile.getAbsolutePath();
        startLoadFromDisk();
    }

    public XSharedPreferences(String packageName) {
        this(packageName, packageName + "_preferences");
    }

    public XSharedPreferences(String packageName, String prefFileName) {
        File dataDir = Environment.getDataDirectory();
        this.mFile = new File(dataDir, "data/" + packageName + "/shared_prefs/" + prefFileName + ".xml");
        this.mFilename = mFile.getAbsolutePath();
        startLoadFromDisk();
    }

    private void startLoadFromDisk() {
        synchronized (this) {
            mLoaded = false;
        }
        new Thread("XSharedPreferences-load") {
            @Override
            public void run() {
                synchronized (XSharedPreferences.this) {
                    loadFromDiskLocked();
                }
            }
        }.start();
    }

    private Map<String, Object> readMap(java.io.InputStream is) {
        try {
            Class<?> xmlUtils = Class.forName("com.android.internal.util.XmlUtils");
            java.lang.reflect.Method readMapXml = xmlUtils.getMethod("readMapXml", java.io.InputStream.class);
            return (Map<String, Object>) readMapXml.invoke(null, is);
        } catch (Exception e) {
            Log.e(TAG, "Error reading map XML", e);
            return new HashMap<>();
        }
    }

    private void loadFromDiskLocked() {
        if (mLoaded) return;

        Map<String, Object> map = null;
        FileResult result = null;
        try {
            result = SELinuxHelper.getAppDataFileService().getFileInputStream(mFilename, mFileSize, mLastModified);
            if (result.stream != null) {
                map = readMap(result.stream);
                result.stream.close();
            } else {
                map = mMap;
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "File not found: " + mFilename);
        } catch (Exception e) {
            Log.w(TAG, "getSharedPreferences", e);
        } finally {
            mLoaded = true;
            if (map != null) {
                mMap = map;
                if (result != null) {
                    mLastModified = result.mtime;
                    mFileSize = result.size;
                }
            } else if (mMap == null) {
                mMap = new HashMap<>();
            }
            notifyAll();
        }
    }

    private void awaitLoadedLocked() {
        while (!mLoaded) {
            try {
                wait();
            } catch (InterruptedException interruptedException) {}
        }
    }

    @Override
    public Map<String, ?> getAll() {
        synchronized (this) {
            awaitLoadedLocked();
            return new HashMap<>(mMap);
        }
    }

    @Override
    public String getString(String key, String defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            String v = (String) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        synchronized (this) {
            awaitLoadedLocked();
            Set<String> v = (Set<String>) mMap.get(key);
            return v != null ? v : defValues;
        }
    }

    @Override
    public int getInt(String key, int defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Integer v = (Integer) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    @Override
    public long getLong(String key, long defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Long v = (Long) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    @Override
    public float getFloat(String key, float defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Float v = (Float) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Boolean v = (Boolean) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    @Override
    public boolean contains(String key) {
        synchronized (this) {
            awaitLoadedLocked();
            return mMap.containsKey(key);
        }
    }

    @Override
    public Editor edit() {
        throw new UnsupportedOperationException("XSharedPreferences is read-only");
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException("XSharedPreferences is read-only");
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException("XSharedPreferences is read-only");
    }

    public boolean makeWorldReadable() {
        if (!SELinuxHelper.getAppDataFileService().hasDirectFileAccess()) {
            return false;
        }
        return mFile.exists() && mFile.setReadable(true, false);
    }

    public void reload() {
        synchronized (this) {
            mLoaded = false;
            loadFromDiskLocked();
        }
    }
}
