package me.kbai.mfinstaller.ui;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.kbai.mfinstaller.R;
import me.kbai.mfinstaller.tool.FileUtils;
import me.kbai.mfinstaller.tool.RomUtils;

/**
 * @author sean
 */
public class InstallActivity extends AppCompatActivity {
    public static final int RESULT_CODE = 999;
    public static final int INSTALL_REQUEST_CODE = 10999;

    public static final String KEY_RESULT = "KEY_RESULT";
    public static final String KEY_DELETE_PATH = "KEY_DELETE_PATH";

    public static final String KEY_XAPK_PATH = "xapk_path";
    public static final String KEY_APK_PATHS = "apk_path";

    private String xapkPath;
    private ArrayList<String> apkPaths;
    private ExecutorService installXapkExecutor;

    private int mSessionId;

    private final PackageInstaller.SessionCallback mSessionCallback = new PackageInstaller.SessionCallback() {
        @Override
        public void onCreated(int sessionId) {
        }

        @Override
        public void onBadgingChanged(int sessionId) {
        }

        @Override
        public void onActiveChanged(int sessionId, boolean active) {
        }

        @Override
        public void onProgressChanged(int sessionId, float progress) {
        }

        @Override
        public void onFinished(int sessionId, boolean success) {
            //某些系统修改了pi, sessionId可能不一样
            if (RomUtils.isXiaomi() && success) {
                showXiaomiSuccessTips();
                return;
            }
            setResult(RESULT_OK, buildResultData(success));
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_install);

        getPackageManager().getPackageInstaller().registerSessionCallback(mSessionCallback);

        installXapkExecutor = Executors.newSingleThreadExecutor();
        initData(getIntent());
        installXapk();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra(InstallResultReceiverActivity.INTENT_EXTRA_RETRY, false)) {
            installXapk();
        }
    }

    public void initData(Intent intent) {
        String xapkPath = intent.getStringExtra(KEY_XAPK_PATH);
        if (xapkPath != null) {
            this.xapkPath = xapkPath;
        }
        ArrayList<String> apkPaths = intent.getStringArrayListExtra(KEY_APK_PATHS);
        if (apkPaths != null) {
            this.apkPaths = apkPaths;
        }
    }

    private void installXapk() {
        if (apkPaths == null || apkPaths.isEmpty()) {
            finish();
            return;
        }
        installXapkExecutor.execute(() -> {
            PackageInstaller.Session session = null;
            try {
                PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                mSessionId = packageInstaller.createSession(params);
                session = packageInstaller.openSession(mSessionId);
                if (apkPaths != null) {
                    for (String apkPath : apkPaths) {
                        addApkToInstallSession(apkPath, session);
                    }
                    commitSession(session);
                }
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
                abandonSession();
            } finally {
                if (session != null) session.close();
            }
        });
    }

    private void addApkToInstallSession(String filePath, PackageInstaller.Session session)
            throws IOException {
        // It's recommended to pass the file size to openWrite(). Otherwise installation may fail
        // if the disk is almost full.
        try (OutputStream packageInSession = session.openWrite(FileUtils.getFileName(filePath), 0, new File(filePath).length());
             InputStream is = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = is.read(buffer)) >= 0) {
                packageInSession.write(buffer, 0, n);
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void commitSession(PackageInstaller.Session session) {
        Intent intent = new Intent(this, InstallResultReceiverActivity.class);
        //intent.setAction(PACKAGE_INSTALLED_ACTION);
        intent.putExtra(KEY_XAPK_PATH, xapkPath);
        intent.putExtra(KEY_APK_PATHS, apkPaths);

        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
        IntentSender statusReceiver = pendingIntent.getIntentSender();

        // Commit the session (this will start the installation workflow).
        session.commit(statusReceiver);
    }

    private void abandonSession() {
        try {
            getPackageManager().getPackageInstaller().abandonSession(mSessionId);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            getPackageManager().getPackageInstaller().unregisterSessionCallback(mSessionCallback);
            if (installXapkExecutor != null && !installXapkExecutor.isShutdown()) {
                installXapkExecutor.shutdown();
            }
        }
    }

    private Intent buildResultData(boolean success) {
        Intent intent = new Intent().putExtra(KEY_RESULT, success);
        if (apkPaths != null && apkPaths.size() > 0) {
            File deletePath = new File(apkPaths.get(0)).getParentFile();
            if (deletePath != null && deletePath.exists()) {
                intent.putExtra(KEY_DELETE_PATH, deletePath.getAbsolutePath());
            }
        }
        return intent;
    }

    private void showXiaomiSuccessTips() {
        LinearLayout llWaiting = findViewById(R.id.ll_waiting);
        LinearLayout llTips = findViewById(R.id.ll_tips);
        Button btnRetry = findViewById(R.id.btn_retry);
        llWaiting.setVisibility(View.GONE);
        llTips.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.tv_tips)).setText(R.string.xiaomi_tips2);
        btnRetry.setText(R.string.back);
        btnRetry.setOnClickListener(v -> {
            buildResultData(true);
            finish();
        });
    }
}
