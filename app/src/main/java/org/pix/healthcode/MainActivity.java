package org.pix.healthcode;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements Handler.Callback, SharedPreferences.OnSharedPreferenceChangeListener, PopupMenu.OnMenuItemClickListener {
    private Handler handler = null;
    private Timer timer = null;
    private SharedPreferences sharedPrefs;
    private PrefsConfig cfg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler(this);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        cfg = new PrefsConfig(this);
        cfg.load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setLayout();
        updateUI();
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(0);
            }
        };
        timer = new Timer();
        timer.schedule(tt, 0, 66);
    }

    @Override
    protected void onPause() {
        timer.cancel();
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public boolean handleMessage(Message msg) {
        updateDateTimeView();
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        int changedUserIndex = key.charAt(key.length()-1) - '0';
        if(changedUserIndex == cfg.getUserIndex()) {
            String sKey = key.substring(0, key.length()-1);
            switch(sKey) {
                case "KEY_COLOR":
                    updateQrcode();
                    updateCheckpoints();
                    break;
                case "KEY_PROVINCE":
                    updateCheckpoints();
                    break;
                case "KEY_CITY":
                    updateCityViews();
                    break;
                case "KEY_HOTLINE":
                    updateHotlineView();
                    break;
                case "KEY_NAME":
                    updateNameView();
                    break;
                case "KEY_ID":
                    updateIdView();
                    break;
                case "KEY_CONTENT":
                    updateQrcode();
                    break;
                default:
                    break;
            }
        }
    }

    private void updateUI() {
        updateCityViews();
        updateNameView();
        updateIdView();
        updateQrcode();
        updateCheckpoints();
        updateHotlineView();
    }
    private void updateCheckpoints() {
        String province = cfg.getProvince();
        String colorName = cfg.getColorName();
        String checkpoint = cfg.getCheckpoint();
        String text = String.format(checkpoint, colorName, province);
        text = text.concat(getString(R.string.notes));
        TextView view = findViewById(R.id.txtCheckpoints);
        view.setText(text);
    }
    private void updateCityViews() {
        String city = cfg.getCity();
        TextView view = findViewById(R.id.txtTitleCity);
        view.setText(city);
        if(!cfg.isHangzhou()) {
            view = findViewById(R.id.txtIdCity);
            if(view != null) {
                view.setText(city);
            }
        }
    }
    private void updateDateTimeView() {
        Date date = new Date();
        date.setTime(System.currentTimeMillis());
        if(cfg.isHangzhou()) {
            DateFormat fmt = new SimpleDateFormat("M", Locale.getDefault());
            TextView txtMonth = findViewById(R.id.txtMonth);
            txtMonth.setText(fmt.format(date));
            fmt = new SimpleDateFormat("dd", Locale.getDefault());
            TextView txtDay = findViewById(R.id.txtDay);
            txtDay.setText(fmt.format(date));
            fmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            TextView txtTime = findViewById(R.id.txtTime);
            txtTime.setText(fmt.format(date));
        } else {
            DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            TextView txtDateTime = findViewById(R.id.txtDateTime);
            txtDateTime.setText(fmt.format(date));
        }
    }
    private void updateHotlineView() {
        String hotline = cfg.getHotline();
        TextView view = findViewById(R.id.txtHotline);
        view.setText(hotline);
    }
    private void updateNameView() {
        String name = cfg.getUserName();
        TextView view = findViewById(R.id.txtUserName);
        view.setText(name);
    }
    private void updateIdView() {
        if(cfg.isHangzhou()) {
            return;
        }
        ToggleButton btnIdVisibility = findViewById(R.id.btnIdVisibility);
        boolean visible = btnIdVisibility.isChecked();
        String userId = cfg.getUserId();
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<userId.length(); i++) {
            sb.append(visible ? userId.charAt(i) : '*');
            if(i % 4 == 3) {
                sb.append(' ');
            }
        }
        TextView view = findViewById(R.id.txtIdentityId);
        view.setText(sb.toString());
    }
    private void updateQrcode() {
        ImageView imgQrcode = findViewById(R.id.imgQrcode);
        String codeContent = cfg.getCodeContent();
        int colorValue = cfg.getColorValue();
        Bitmap bmp = genQrcode(codeContent, colorValue);
        if(bmp != null) {
            imgQrcode.setImageBitmap(bmp);
        }

        if(cfg.isHangzhou()) {
            TextView hospitleTipView = findViewById(R.id.txtHospitleTip);
            hospitleTipView.setTextColor(colorValue);
            RelativeLayout layout = findViewById(R.id.bgHealthColor);
            layout.setBackgroundColor(colorValue);
            ImageView frameImageView = findViewById(R.id.hz_qrcode_frame);
            GradientDrawable frameShape = (GradientDrawable) frameImageView.getBackground();
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            frameShape.setStroke((int) (6*dm.density), colorValue);
        }
    }

    private void setLayout() {
        if(cfg.isHangzhou()) {
            setBrightness(0.618f);
            setContentView(R.layout.activity_hz);
        } else {
            setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
            setContentView(R.layout.activity_default);
        }
    }

    public void quit(View view) {
        finish();
    }

    public void showError(View view) {
        Toast t = Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        t.show();
    }

    public void toggleIdNumber(View view) {
        updateIdView();
    }

    public void switchUser(View view) {
        int userIndex = cfg.getUserIndex();
        userIndex = (userIndex + 1) % 2;
        cfg.setUserIndex(userIndex);
        setLayout();
        updateUI();
    }

    public void showOptions(View view) {
        PopupMenu pm = new PopupMenu(this, view);
        Menu menu = pm.getMenu();
        getMenuInflater().inflate(R.menu.menu, menu);
        SubMenu sm = menu.getItem(0).getSubMenu();
        for(int i=0; i<2; i++) {
            String title = String.format(getString(R.string.menu_edit_user), (i+1));
            sm.getItem(i).setTitle(title);
        }
        pm.setOnMenuItemClickListener(this);
        pm.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.id_cust1:
            case R.id.id_cust2:
                Intent intent = new Intent(this, ConfigActivity.class);
                intent.putExtra("INDEX", item.getItemId()==R.id.id_cust1 ? 0 : 1);
                startActivity(intent);
                break;
            case R.id.id_help_and_suggestion:
                WebViewActivity.startActivity(this, R.string.menu_help_and_suggestion, "file:////android_asset/help.html");
                break;
            case R.id.id_app_info:
                WebViewActivity.startActivity(this, R.string.menu_app_info, "file:////android_asset/appinfo.html");
                break;
            case R.id.id_privacy:
                WebViewActivity.startActivity(this, R.string.menu_privacy, "https://sites.google.com/view/morrowindxie-privacy-policy");
                break;
            default:
                break;
        }
        return false;
    }

    private Bitmap genQrcode(String text, int color) {
        HashMap hints = new HashMap();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 0);
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 640, 640, hints);
            int w = bitMatrix.getWidth();
            int h = bitMatrix.getHeight();
            int[] pixels = new int[w * h];
            for(int y=0; y<h; y++) {
                for(int x=0; x<w; x++) {
                    pixels[y*w+x] = bitMatrix.get(x, y) ? color : Color.TRANSPARENT;
                }
            }
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bmp.setPixels(pixels, 0, w, 0, 0, w, h);
            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setBrightness(float brightness) {
        Window window = this.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = brightness;
        window.setAttributes(lp);
    }
}
