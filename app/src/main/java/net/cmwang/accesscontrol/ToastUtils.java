package net.cmwang.accesscontrol;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class ToastUtils {

    private Context mContext;

    public ToastUtils(Context ctx) {
        super();
        mContext = ctx;
    }

    /* Show toast message */
    private void showToast(Context ctx, String msg, int duration) {
        Toast toast = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT);
        // set toast position to center
        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    /* Show short toast message */
    public void shortMSG(String msg) {
        showToast(mContext, msg, Toast.LENGTH_SHORT);
    }

    /* Show long toast message */
    public void longMSG(String msg) {
        showToast(mContext, msg, Toast.LENGTH_SHORT);
    }
}
