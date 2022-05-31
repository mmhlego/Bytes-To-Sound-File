package lt.demo.stethoscope.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;

import lt.demo.stethoscope.R;

public class ProgressDialog extends DialogFragment {

    private TextView tvMessage;
    private int mMessageText;

    public ProgressDialog() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_fragmeng_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvMessage = view.findViewById(R.id.tv_message);
        if (mMessageText > 0) {
            tvMessage.setText(mMessageText);
        }
    }

    public void setMessage(@StringRes int text) {
        this.mMessageText = text;
        if (tvMessage != null) {
            tvMessage.setText(mMessageText);
        }
    }
}
