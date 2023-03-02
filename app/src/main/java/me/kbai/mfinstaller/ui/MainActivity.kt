package me.kbai.mfinstaller.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kbai.mfinstaller.Constants
import me.kbai.mfinstaller.R
import me.kbai.mfinstaller.tool.AppInstallUtils
import me.kbai.mfinstaller.tool.ProviderUtils
import java.io.File

/**
 * @author Sean on 2023/3/2
 */
class MainActivity : AppCompatActivity() {
    private val permissions =
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

    private val mWaitingDialog by lazy {
        WaitingDialogFragment.newInstance(getString(R.string.installing))
            .apply { isCancelable = false }
    }

    private lateinit var mRequestPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Constants.initPaths(this)
        mRequestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                //
            }
        initView()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (AppInstallUtils.handleRequestAccessObb(this, requestCode, data)) return
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun initView() {
        val tvText = findViewById<TextView>(R.id.tv_text)
        findViewById<TextView>(R.id.btn_select).run {
            setOnClickListener { showSelectFileDialog() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                visibility = View.GONE
                tvText.visibility = View.VISIBLE
            }
        }
    }

    private fun showSelectFileDialog() {
        if (!checkManageExternalStoragePermission()) {
            return
        }
        SelectFileDialog()
            .setFilter { item: File ->
                val types = FileType.values()
                var find = false
                val name = item.name
                for (type in types) {
                    if (name.endsWith(type.value)) {
                        find = true
                        break
                    }
                }
                (item.isDirectory && !item.isHidden) || find
            }
            .setCallBack { dialog1: DialogFragment, file: Array<File?> ->
                dialog1.dismiss()
                if (file[1] != null) {
                    installWithDialog(file[0]!!.absolutePath, file[1]!!.absolutePath)
                } else if (file[0] != null) {
                    installWithDialog(file[0]!!.absolutePath)
                }
            }
            .run {
                isCancelable = false
                val manager = supportFragmentManager
                if (manager.isStateSaved) return@run
                show(manager, "SELECT_FILE")
            }
    }

    private fun checkManageExternalStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val check = checkPermission(*permissions)
            if (!check) mRequestPermissionLauncher.launch(permissions)
            return check
        }
        if (Environment.isExternalStorageManager()) return true
        val permissionIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData(Uri.parse("package:$packageName"))
        startActivity(permissionIntent)
        return false
    }

    private fun checkPermission(vararg permissions: String?): Boolean {
        var allAccess = true
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission!!
                ) == PackageManager.PERMISSION_DENIED
            ) {
                allAccess = false
                break
            }
        }
        return allAccess
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_VIEW == intent.action && intent.data != null) {
            var path: String? = ProviderUtils.getPath(this, intent.data)
            if (path.isNullOrEmpty()) {
                path = Uri.decode(intent.data!!.encodedPath)
                if (path.startsWith("/external_files")) path = path.substring(15)
            }
            if (path == null) return
            Log.i(MainActivity::class.simpleName, "Handle intent, path:$path")
            intent.data = null
            installWithDialog(path)
        }
    }

    private fun installWithDialog(path: String, vararg obbPaths: String) = lifecycleScope.launch {
        if (path.endsWith(".obb")) {
            val fileName = path.substring(path.lastIndexOf(File.separator) + 1)
            if (AppInstallUtils.parseObbName(fileName) == null) {
                showToast(R.string.wrong_obb_filename)
                return@launch
            }
            showWaitingDialog()
            val success = withContext(Dispatchers.IO) {
                AppInstallUtils.installObbOnly(path)
            }
            if (success) showToast(R.string.install_obb_success) else showToast(R.string.install_obb_fail)
            dismissWaitingDialog()
        } else {
            showWaitingDialog()
            withContext(Dispatchers.IO) {
                if (obbPaths.isEmpty()) {
                    AppInstallUtils.install(this@MainActivity, File(path))
                } else {
                    AppInstallUtils.install(this@MainActivity, File(path), *obbPaths)
                }
            }
            dismissWaitingDialog()
        }
    }

    private fun showToast(@StringRes id: Int) = Toast.makeText(this, id, Toast.LENGTH_LONG).show()

    private fun showWaitingDialog() {
        val fragmentManager = supportFragmentManager
        if (!fragmentManager.isStateSaved) {
            mWaitingDialog.show(fragmentManager, WaitingDialogFragment.TAG)
        }
    }

    private fun dismissWaitingDialog() {
        if (mWaitingDialog.isAdded) mWaitingDialog.dismissAllowingStateLoss()
    }

    private enum class FileType(val value: String) {
        APK(".apk"),
        XAPK(".xapk"),
        OBB(".obb"),
        APKM(".apkm"),
        ZIP(".zip"),
        APKS(".apks")
    }
}