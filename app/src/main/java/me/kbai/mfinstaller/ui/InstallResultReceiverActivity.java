package me.kbai.mfinstaller.ui;

import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import me.kbai.mfinstaller.R;
import me.kbai.mfinstaller.tool.RomUtils;

/**
 * @author sean on 2022/1/10
 */
public class InstallResultReceiverActivity extends AppCompatActivity {
    public static final String INTENT_EXTRA_RETRY = "INTENT_EXTRA_RETRY";

    private static final String TAG = "InstallResReceiver";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_install);

        handleResult(getIntent());
    }

    private void handleResult(Intent intent) {
        int status = -100;
        String message = "";
        Bundle extras = intent.getExtras();
        if (extras != null) {
            status = extras.getInt(PackageInstaller.EXTRA_STATUS);
            message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
        }
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                // This test app isn't privileged, so the user has to confirm the install.
                Intent confirmIntent = (Intent) extras.get(Intent.EXTRA_INTENT);
                ResolveInfo resolveInfo = getPackageManager().resolveActivity(confirmIntent, PackageManager.MATCH_DEFAULT_ONLY);
                if (resolveInfo != null) {
                    try {
                        startActivity(confirmIntent);
                        finish();
                    } catch (SecurityException e) {
                        textTips(status, message, R.string.installer_not_found_tips, false, true);
                    }
                } else {
                    textTips(status, message, R.string.installer_not_found_tips, false, true);
                }
                break;

            case PackageInstaller.STATUS_SUCCESS:
                Toast.makeText(this, R.string.installed, Toast.LENGTH_SHORT).show();
                //setResult(RESULT_CODE, buildResultData(true));
                if (RomUtils.isXiaomi()) {
                    textTips(status, message, R.string.xiaomi_tips2, false, false);
                    return;
                }
                finish();
                break;

            case PackageInstaller.STATUS_FAILURE:
            case PackageInstaller.STATUS_FAILURE_ABORTED:
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
            case PackageInstaller.STATUS_FAILURE_INVALID:
                Toast.makeText(this, R.string.fail_to_install, Toast.LENGTH_SHORT).show();

                if (RomUtils.isXiaomi()) {
                    textTips(status, message, R.string.xiaomi_tips, true, true);
                    return;
                } else if (RomUtils.isOppo()) {
                    textTips(status, message, R.string.oppo_permission_tips, false, true);
                    return;
                }
                installFail(status, message);
                break;

            case PackageInstaller.STATUS_FAILURE_STORAGE:
                Toast.makeText(this, R.string.not_enough_storage, Toast.LENGTH_SHORT).show();
                installFail(status, message);
                break;

            default:
                Toast.makeText(this, R.string.ins_unknown_error, Toast.LENGTH_SHORT).show();
                installFail(status, message);
        }
    }

    private void textTips(int status, String message, @StringRes int textRes, boolean retry, boolean fail) {
        LinearLayout llWaiting = findViewById(R.id.ll_waiting);
        LinearLayout llTips = findViewById(R.id.ll_tips);
        Button btnRetry = findViewById(R.id.btn_retry);
        if (retry && textRes != R.string.xiaomi_tips2) {
            llWaiting.setVisibility(View.GONE);
            llTips.setVisibility(View.VISIBLE);
            btnRetry.setOnClickListener(v -> {
                Intent intent1 = new Intent(this, InstallActivity.class);
                intent1.putExtra(INTENT_EXTRA_RETRY, true);
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    intent1.putExtras(extras);
                }
                startActivity(intent1);
                finish();
            });
        } else {
            llWaiting.setVisibility(View.GONE);
            llTips.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tv_tips)).setText(textRes);
            btnRetry.setText(R.string.back);
            btnRetry.setOnClickListener(v -> {
                if (fail) {
                    installFail(status, message);
                } else {
                    finish();
                }
            });
        }
    }

    private void installFail(int status, String message) {
        finish();
        Log.d(TAG, "Install failed! " + status + ", " + message);
    }

//    private Intent buildResultData(boolean success) {
//        Intent intent = new Intent().putExtra(KEY_RESULT, success);
//        if (apkPaths != null && apkPaths.size() > 0) {
//            File deletePath = new File(apkPaths.get(0)).getParentFile();
//            if (deletePath.exists()) {
//                intent.putExtra(KEY_DELETE_PATH, deletePath.getAbsolutePath());
//            }
//        }
//        return intent;
//    }
}
