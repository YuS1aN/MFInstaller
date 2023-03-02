package me.kbai.mfinstaller.tool;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.io.FilesKt;
import me.kbai.mfinstaller.Constants;
import me.kbai.mfinstaller.R;
import me.kbai.mfinstaller.tool.xapk.XapkInstaller;
import me.kbai.mfinstaller.tool.xapk.XapkInstallerFactory;
import me.kbai.mfinstaller.ui.InstallActivity;

public class AppInstallUtils {
    public static final int REQUEST_ACCESS_OBB = 20001;
    private static final Pattern OBB_PATTERN = Pattern.compile("main\\.(\\d+)\\.(.+)\\.obb");
    private static final Pattern OBB_PATCH_PATTERN = Pattern.compile("patch\\.(\\d+)\\.(.+)\\.obb");

    private static Application mApp;

    public static void initAppContext(Application app) {
        mApp = app;
    }

    @Nullable
    public static Matcher parseObbName(String obbName) {
        Matcher matcher = OBB_PATTERN.matcher(obbName);
        if (matcher.find()) {
            return matcher;
        }
        Matcher matcher1 = OBB_PATCH_PATTERN.matcher(obbName);
        if (matcher1.find()) {
            return matcher1;
        }
        return null;
    }

    /**
     * Compare the obb version
     *
     * @param name0 the first obb name
     * @param name1 the second obb name
     * @return -1 / 0 / 1  The version of the first obb is 'less than / equal to / greater than' the second
     */
    public static int compareObbVersion(String name0, String name1) {
        Matcher m0 = OBB_PATTERN.matcher(name0);
        Matcher m1 = OBB_PATTERN.matcher(name1);
        if (m0.find() && m1.find()) {
            int v0 = Integer.parseInt(StringUtils.nonEmpty(m0.group(1), "0"));
            int v1 = Integer.parseInt(StringUtils.nonEmpty(m1.group(1), "0"));
            if (v0 == v1) return 0;
            return v0 < v1 ? -1 : 1;
        } else {
            Matcher m2 = OBB_PATCH_PATTERN.matcher(name0);
            Matcher m3 = OBB_PATCH_PATTERN.matcher(name1);
            if (m2.find() && m3.find()) {
                int v0 = Integer.parseInt(StringUtils.nonEmpty(m2.group(1), "0"));
                int v1 = Integer.parseInt(StringUtils.nonEmpty(m3.group(1), "0"));
                if (v0 == v1) return 0;
                return v0 < v1 ? -1 : 1;
            }
        }
        return 0;
    }

    @WorkerThread
    @Nullable
    public static String getObbPackageName(File xapkFile) {
        try {
            ZipFile zipFile = Zip4JUtils.openZipFile(xapkFile);
            List<FileHeader> headers = zipFile.getFileHeaders();
            FileHeader header = CollectionUtils.find(headers, item -> item.getFileName().endsWith(".obb"));
            if (header == null) return null;
            String filePath = header.getFileName();
            //  Android/obb/
            return filePath.substring(12, filePath.indexOf("/", 12));
        } catch (ZipException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void install(@NonNull Activity activity, @NonNull File apkFile, String... obbPaths) {
        if (!apkFile.exists()) {
            return;
        }
        String extension = FilesKt.getExtension(apkFile).toLowerCase();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!TextUtils.equals(extension, "apk")) {
                if (!requestAccessObb(activity)) {
                    return;
                }
            }
        }

        switch (extension) {
            case "apk":
                if (obbPaths == null || obbPaths.length == 0) {
                    installApk(activity, apkFile);
                } else {
                    if (installObbOnly(obbPaths)) {
                        installApk(activity, apkFile);
                    }
                }
                break;

            case "xapk":
            case "apks":
            case "zip":
                installXApk(activity, apkFile);
                break;

            case "apkm":
                installApkm(activity, apkFile);
                break;

            default:
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private static boolean requestAccessObb(Activity activity) {
        for (UriPermission persistedUriPermission : activity.getContentResolver().getPersistedUriPermissions()) {
            if (persistedUriPermission.isWritePermission() && persistedUriPermission.getUri().toString().equals(ObbAccessUtils.OBB_TREE_URI)) {
                return true;
            }
        }
        String title = activity.getString(R.string.grant_obb_path_title);
        String msg = activity.getString(R.string.grant_obb_path_tips);
        activity.runOnUiThread(() -> {
            Dialog customDialog = new AlertDialog.Builder(activity)
                    .setMessage(msg)
                    .setTitle(title)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        DocumentFile obbDoc = ObbAccessUtils.getTreeDocumentFile(activity);
                        if (obbDoc == null) return;
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                        intent.putExtra("android.provider.extra.SHOW_ADVANCED", true);
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, obbDoc.getUri());
                        activity.startActivityForResult(intent, REQUEST_ACCESS_OBB);
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, witch) -> dialog.dismiss())
                    .create();
            customDialog.setCancelable(false);
            customDialog.show();
        });
        return false;
    }

    public static boolean installObbOnly(String... obbPaths) {
        for (String obbPath : obbPaths) {
            File obbFile = new File(obbPath);
            if (obbFile.exists()) {
                Matcher matcher = parseObbName(obbFile.getName());
                if (matcher == null) {
                    return false;
                }
                String packageName = matcher.group(2);
                String obbDest = Environment.getExternalStorageDirectory()
                        + File.separator + "Android"
                        + File.separator + "obb"
                        + File.separator + packageName
                        + File.separator + obbFile.getName();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!ObbAccessUtils.copyBySAF(mApp, obbFile.getAbsolutePath(), obbDest)) {
                        return false;
                    }
                } else {
                    if (!FileUtils.copy(obbFile.getAbsolutePath(), obbDest)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void installApk(@NonNull Activity activity, @NonNull File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(activity.getApplicationContext(),
                    activity.getPackageName() + ".fileProvider", apkFile);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        activity.startActivityForResult(intent, InstallActivity.INSTALL_REQUEST_CODE);
    }

    private static void installXApk(Activity activity, @NonNull File apkFile) {
        XapkInstaller xapkInstaller = XapkInstallerFactory.createXapkInstaller(activity, apkFile);
        if (xapkInstaller != null) {
            xapkInstaller.installXapk(activity);
        }
    }

    private static void installApkm(Activity activity, File apkFile) {
        String fileName = apkFile.getName();
        fileName = fileName.substring(0, fileName.lastIndexOf("."));
        String decryptPath = Constants.INSTANCE.getXapkUnzipPath()
                + File.separator + fileName + "_decrypt";

        if (UnApkm.isAlreadyZip(apkFile)) {
            installXApk(activity, apkFile);
        } else {
            File outputFile = new File(decryptPath);
            UnApkm.decryptFile(apkFile.getAbsolutePath(), decryptPath);
            installXApk(activity, outputFile);
        }
    }

    @SuppressLint("WrongConstant")
    public static boolean handleRequestAccessObb(FragmentActivity activity, int requestCode, Intent data) {
        if (requestCode != REQUEST_ACCESS_OBB || data == null) return false;
        Uri uri = data.getData();
        if (uri == null) return false;
        activity.getContentResolver().takePersistableUriPermission(uri,
                data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        return true;
    }
}
