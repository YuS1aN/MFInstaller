package me.kbai.mfinstaller.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import me.kbai.mfinstaller.R;


public class WaitingDialogFragment extends DialogFragment {
    public static final String TAG = WaitingDialogFragment.class.getSimpleName();
    private static final String ARGUMENT_TEXT = "ARG_TEXT";

    private String mText;

    public WaitingDialogFragment() {
    }

    public static WaitingDialogFragment newInstance(String text) {
        Bundle args = new Bundle();
        args.putString(ARGUMENT_TEXT, text);
        WaitingDialogFragment fragment = new WaitingDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_waiting_progress, container, false);
        initArguments();
        initView(view);
        return view;
    }

    private void initArguments() {
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        mText = arguments.getString(ARGUMENT_TEXT);
    }

    private void initView(View view) {
        view.<TextView>findViewById(R.id.tv_text).setText(mText);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() == null) {
            return;
        }
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        window.setLayout(displayMetrics.widthPixels, displayMetrics.heightPixels);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}
