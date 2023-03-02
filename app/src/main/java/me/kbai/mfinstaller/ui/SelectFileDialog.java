package me.kbai.mfinstaller.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;

import me.kbai.mfinstaller.R;
import me.kbai.mfinstaller.tool.AppInstallUtils;
import me.kbai.mfinstaller.tool.AppUtils;
import me.kbai.mfinstaller.tool.CollectionUtils;
import me.kbai.mfinstaller.tool.SizeUtils;
import me.kbai.mfinstaller.tool.StringUtils;
import me.kbai.mfinstaller.tool.comparator.ComparatorManager;
import me.kbai.mfinstaller.tool.xapk.XapkInstallerFactory;

public class SelectFileDialog extends DialogFragment {

    private HorizontalScrollView mNavigationScrollView;
    private LinearLayout mNavigationLayout;
    private RecyclerView mSelectRecyclerView;
    private SelectFileAdapter mSelectAdapter;
    private final List<File> mCurrentFiles;
    private ConfirmSelectFileCallBack mConfirmImportCallback;
    private CollectionUtils.Filter<File> mFilter;
    private File mSelectedFile;
    private File mExtraObbFile;

    private final ScheduledExecutorService mIoPool;

    public SelectFileDialog() {
        mCurrentFiles = new ArrayList<>();
        mSelectedFile = null;
        mExtraObbFile = null;
        mIoPool = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setName("SelectFileDialog" + thread.getName());
            return thread;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        DisplayMetrics metrics = new DisplayMetrics();
        if (getDialog() == null || getActivity() == null) {
            return;
        }
        Window window = getDialog().getWindow();
        if (window == null) {
            return;
        }
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        //限制最小宽度
        int minWidth = (int) SizeUtils.dp2px(getActivity(), 300);
        if (minWidth > metrics.widthPixels * 0.85) {
            if (minWidth < metrics.widthPixels) {
                window.setLayout(minWidth, (int) (metrics.heightPixels * 0.85));
            } else {
                window.setLayout((int) (metrics.widthPixels * 0.95), (int) (metrics.heightPixels * 0.8));
            }
            return;
        }
        window.setLayout((int) (metrics.widthPixels * 0.85), (int) (metrics.heightPixels * 0.8));
        window.setBackgroundDrawableResource(R.drawable.bg_white_r10);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        return inflater.inflate(R.layout.dialog_select_file, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initView(view);
        initFiles();
        super.onViewCreated(view, savedInstanceState);
    }

    private void initView(View root) {
        root.findViewById(R.id.tv_cancel).setOnClickListener(v -> dismiss());
        root.findViewById(R.id.tv_confirm).setOnClickListener(v ->
                mConfirmImportCallback.confirm(SelectFileDialog.this, mSelectedFile, mExtraObbFile));
        mNavigationScrollView = root.findViewById(R.id.sv_navigation);
        mNavigationLayout = root.findViewById(R.id.ll_navigation);
        mSelectRecyclerView = root.findViewById(R.id.rv_select_file);
        mSelectAdapter = new SelectFileAdapter();
        mSelectRecyclerView.setAdapter(mSelectAdapter);
        mSelectRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false));
    }

    private void initFiles() {
        File root = Environment.getExternalStorageDirectory();
        openDir(root);
        addNavigationNode(root);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void openDir(File file) {
        File[] files = file.listFiles();
        if (files == null) {
            files = new File[0];
        }
        List<File> fileList = Arrays.asList(files);
        List<File> filtered = CollectionUtils.filter(fileList, mFilter);
//        Collections.sort(filtered, (o1, o2) ->
//                ComparatorManager.getInstance().getFileModifyTimeComparator().compare(o1, o2, false));
        Collections.sort(filtered, (o1, o2) ->
                ComparatorManager.getInstance().getStringComparator().compare(o1.getName(), o2.getName(), true));
        mCurrentFiles.clear();
        mCurrentFiles.addAll(filtered);
        mSelectAdapter.cleanLastItems(mSelectRecyclerView);
        mSelectedFile = null;
        mExtraObbFile = null;
        mSelectAdapter.mLastClickPosition.clear();
        mSelectAdapter.notifyDataSetChanged();
        mSelectRecyclerView.scrollToPosition(0);
    }

    private void addNavigationNode(final File pathFile) {
        assert getContext() != null;
        if (mNavigationLayout.getChildCount() > 0) {
            TextView arrow = new TextView(getContext());
            arrow.setText(">");
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            arrowParams.gravity = Gravity.CENTER;
            mNavigationLayout.addView(arrow);
        }
        TextView node = new TextView(getContext());
        LinearLayout.LayoutParams nodeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        node.setMinWidth((int) SizeUtils.dp2px(getContext(), 50));
        int dp10 = (int) SizeUtils.dp2px(getContext(), 10);
        node.setPadding(dp10, 0, dp10, 0);
        nodeParams.gravity = Gravity.CENTER;
        if (pathFile.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            node.setText(R.string.file_root);
        } else {
            node.setText(pathFile.getName());
        }
        mNavigationLayout.addView(node);
        final int nodeIndex = mNavigationLayout.getChildCount() - 1;
        node.setOnClickListener(v -> returnNodeTo(nodeIndex, pathFile));
        mNavigationLayout.post(() -> mNavigationScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
    }

    private void returnNodeTo(int index, File file) {
        final int count = mNavigationLayout.getChildCount();
        for (int i = count - 1; i > index; i--) {
            mNavigationLayout.removeViewAt(i);
        }
        openDir(file);
    }

    public SelectFileDialog setCallBack(ConfirmSelectFileCallBack callBack) {
        mConfirmImportCallback = callBack;
        return this;
    }

    public SelectFileDialog setFilter(CollectionUtils.Filter<File> filter) {
        mFilter = filter;
        return this;
    }


    public interface ConfirmSelectFileCallBack {
        /**
         * 确认回调
         *
         * @param dialog this
         * @param file   file
         */
        void confirm(DialogFragment dialog, File... file);
    }

    private class SelectFileAdapter extends RecyclerView.Adapter<SelectFileAdapter.SelectViewHolder> {
        private final List<Integer> mLastClickPosition;
        private final int mSelectedColor;
        private final int mDefaultTextColor;
        private final SparseArray<SelectViewHolder> mLastHolder;

        private SelectFileAdapter() {
            mLastClickPosition = new ArrayList<>();
            mSelectedColor = getResources().getColor(R.color.primary);
            mDefaultTextColor = Color.parseColor("#999999");
            mLastHolder = new SparseArray<>();
        }

        @NonNull
        @Override
        public SelectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.item_select_file, parent, false);
            return new SelectViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SelectViewHolder holder, int position) {
            final File currentFile = mCurrentFiles.get(position);
            holder.tvName.setText(currentFile.getName());
            if (currentFile.isDirectory()) {
                holder.ivType.setImageResource(R.drawable.ic_baseline_folder_24);
                holder.tvType.setText(null);
                holder.tvVersion.setText(null);
                holder.tvSize.setText(null);
                holder.itemView.setOnClickListener(v -> {
                    openDir(currentFile);
                    addNavigationNode(currentFile);
                    cleanLastItems(holder);
                });
                holder.itemView.setBackgroundColor(Color.WHITE);
            } else {
                int pointIndex = currentFile.getAbsolutePath().lastIndexOf(".");
                String type = "";
                if (pointIndex != -1) {
                    type = currentFile.getAbsolutePath().substring(pointIndex + 1);
                }
                holder.tvType.setText(getString(R.string.file_type, type));
                holder.tvSize.setText(getString(R.string.file_size, formatFileLength(currentFile.length())));
                holder.itemView.setOnClickListener(null);
                holder.ivType.setImageResource(R.mipmap.ic_launcher);
                Context context = holder.itemView.getContext();
                String path = currentFile.getAbsolutePath();
                mIoPool.execute(() -> {
                    PackageInfo packageInfo = AppUtils.getPackageInfo(context, currentFile.getAbsolutePath());
                    loadPackageInfo(holder, packageInfo, currentFile, path);
                });

                holder.itemView.setOnClickListener(v -> {
                    //已经选中 apk 文件，再去点击 obb
                    if (currentFile.getName().toLowerCase().endsWith(".obb")) {
                        if (mSelectedFile != null) {
                            if (checkObb(mSelectedFile, currentFile, holder.itemView.getContext())) {
                                selectItem(holder, position, false);
                                mExtraObbFile = currentFile;
                            }
                        }
                    }
                    //已经选中 obb 文件，再去点击 apk
                    if (mSelectedFile != null && mSelectedFile.getName().toLowerCase().endsWith(".obb")
                            && currentFile.getName().toLowerCase().endsWith(".apk")) {
                        if (checkObb(currentFile, mSelectedFile, holder.itemView.getContext())) {
                            selectItem(holder, position, false);
                            mExtraObbFile = mSelectedFile;
                            mSelectedFile = currentFile;
                        }
                    }
                    //其它情况
                    if (mExtraObbFile == null || !mExtraObbFile.equals(currentFile)) {
                        if (!checkObb(currentFile, mExtraObbFile, holder.itemView.getContext())) {
                            selectItem(holder, position, true);
                            mSelectedFile = currentFile;
                            mExtraObbFile = null;
                        }
                    }
                });
                for (int lastPosition : mLastClickPosition) {
                    if (lastPosition == position) {
                        holder.itemView.setBackgroundColor(mSelectedColor);
                    } else {
                        holder.itemView.setBackgroundColor(Color.WHITE);
                    }
                }
            }
        }

        private void loadPackageInfo(SelectViewHolder holder, PackageInfo packageInfo, File currentFile, String path) {
            View view = holder.itemView;
            if (packageInfo == null) {
                view.post(() -> holder.tvVersion.setText(getString(R.string.apk_version, "unknown")));
                if (path.toLowerCase().endsWith(".xapk")
                        || path.toLowerCase().endsWith(".apks")
                        || path.toLowerCase().endsWith(".zip")) {
                    try {
                        File iconFile = XapkInstallerFactory.INSTANCE.getXapkIcon(currentFile);
                        if (iconFile != null) {
                            Bitmap xapkIcon = BitmapFactory.decodeFile(String.valueOf(iconFile), null);
                            view.post(() -> holder.ivType.setImageBitmap(xapkIcon));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Drawable icon = AppUtils.getIcon(view.getContext(), AppUtils.getAppInfo(packageInfo, path));
                view.post(() -> {
                    if (icon != null) {
                        holder.ivType.setImageDrawable(icon);
                    }
                    holder.tvVersion.setText(getString(R.string.apk_version, packageInfo.versionName));
                });
            }
        }

        private boolean checkObb(File apkFile, File obbFile, Context context) {
            if (obbFile != null && obbFile.getName().toLowerCase().endsWith(".obb")
                    && apkFile.getName().toLowerCase().endsWith(".apk")) {
                PackageInfo packageInfo = AppUtils.getPackageInfo(context, apkFile.getAbsolutePath());
                Matcher matcher = AppInstallUtils.parseObbName(obbFile.getName());
                if (matcher != null) {
                    String versionCodeStr = matcher.group(1);
                    int obbVersionCode = Integer.parseInt(StringUtils.nonEmpty(versionCodeStr, "0"));
                    String obbPackageName = matcher.group(2);

                    return packageInfo != null
                            && TextUtils.equals(obbPackageName, packageInfo.packageName)
                            && packageInfo.versionCode == obbVersionCode;
                }
            }
            return false;
        }

        private void selectItem(SelectViewHolder holder, int position, boolean clean) {
            if (clean) {
                cleanLastItems(holder);
            }
            holder.itemView.setBackgroundColor(mSelectedColor);
            holder.tvName.setTextColor(Color.WHITE);
            holder.tvVersion.setTextColor(Color.WHITE);
            holder.tvType.setTextColor(Color.WHITE);
            holder.tvSize.setTextColor(Color.WHITE);
            mLastHolder.put(position, holder);
            mLastClickPosition.add(position);
        }

        private void cleanLastItems(RecyclerView recyclerView) {
            for (int lastPosition : mLastClickPosition) {
                if (lastPosition >= 0) {
                    SelectViewHolder lastHolder = (SelectViewHolder) recyclerView.findViewHolderForAdapterPosition(lastPosition);
                    if (lastHolder == null) {
                        lastHolder = mLastHolder.get(lastPosition);
                    }
                    if (lastHolder != null) {
                        lastHolder.itemView.setBackgroundColor(Color.WHITE);
                        lastHolder.tvName.setTextColor(mDefaultTextColor);
                        lastHolder.tvVersion.setTextColor(mDefaultTextColor);
                        lastHolder.tvType.setTextColor(mDefaultTextColor);
                        lastHolder.tvSize.setTextColor(mDefaultTextColor);
                    }
                }
            }
            mLastClickPosition.clear();
        }

        private void cleanLastItems(SelectViewHolder holder) {
            RecyclerView parent = (RecyclerView) holder.itemView.getParent();
            cleanLastItems(parent);
        }

        @Override
        public int getItemCount() {
            return mCurrentFiles.size();
        }

//        private int getDefaultTextColor(Context context) {
//            TypedArray array = context.getTheme().obtainStyledAttributes(new int[]{
//                    android.R.attr.textColorSecondary
//            });
//            int textColor = array.getColor(0, 0xFF00FF);
//            array.recycle();
//            return textColor;
//        }

        private String formatFileLength(long length) {
            DecimalFormat df = new DecimalFormat("#");
            String fileSizeString;
            if (length < 1024) {
                fileSizeString = length + " B";
            } else if (length < 1048576) {
                fileSizeString = df.format((double) length / 1024) + " KB";
            } else if (length < 1073741824) {
                fileSizeString = df.format((double) length / 1048576) + " MB";
            } else {
                fileSizeString = df.format((double) length / 1073741824) + " GB";
            }
            return fileSizeString;
        }

        private class SelectViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivType;
            private final TextView tvName;
            private final TextView tvVersion;
            private final TextView tvType;
            private final TextView tvSize;

            SelectViewHolder(@NonNull View itemView) {
                super(itemView);
                ivType = itemView.findViewById(R.id.iv_select_type);
                tvName = itemView.findViewById(R.id.tv_file_name);
                tvVersion = itemView.findViewById(R.id.tv_version);
                tvType = itemView.findViewById(R.id.tv_file_type);
                tvSize = itemView.findViewById(R.id.tv_file_size);
            }
        }
    }
}
