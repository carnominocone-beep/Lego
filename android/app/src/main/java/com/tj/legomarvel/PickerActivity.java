package com.tj.legomarvel;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.widget.Toast;
import java.io.File;

/* Launcher: asks for all-files access, lets the player pick the folder that
 * holds the game (LEGOMARVEL.exe, GAME*.DAT...), remembers it, then starts the game. */
public class PickerActivity extends Activity {
    static final String PREFS = "port", KEY = "game_dir";
    private static final int REQ_TREE = 1, REQ_ACCESS = 2;
    private boolean askedAccess;

    static String savedDir(android.content.Context c) {
        String d = c.getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY, null);
        return d != null && isGameDir(d) ? d : null;
    }

    static boolean isGameDir(String d) {
        File f = new File(d);
        if (new File(f, "LEGOMARVEL.exe").exists()) return true;
        File[] l = f.listFiles();  // case-insensitive fallback
        if (l != null) for (File x : l) if (x.getName().equalsIgnoreCase("LEGOMARVEL.exe")) return true;
        return false;
    }

    private boolean hasAccess() {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == 0
            && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == 0;
    }

    @Override protected void onResume() {
        super.onResume();
        if (!hasAccess()) {
            if (askedAccess) {
                Toast.makeText(this, "Izin akses semua file wajib supaya game bisa dibaca.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            askedAccess = true;
            if (Build.VERSION.SDK_INT >= 30) {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())), REQ_ACCESS);
            } else {
                requestPermissions(new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_ACCESS);
            }
            return;
        }
        if (savedDir(this) != null) { launch(); return; }
        if (!pickerOpen) openTree();
    }

    private boolean pickerOpen;

    private void openTree() {
        pickerOpen = true;
        Toast.makeText(this, "Pilih folder yang berisi LEGOMARVEL.exe", Toast.LENGTH_LONG).show();
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQ_TREE);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_TREE) return;
        pickerOpen = false;
        if (res != RESULT_OK || data == null || data.getData() == null) { finish(); return; }
        String path = toPath(data.getData());
        if (path == null || !isGameDir(path)) {
            Toast.makeText(this, "Folder salah: LEGOMARVEL.exe tidak ditemukan di sana.", Toast.LENGTH_LONG).show();
            openTree();
            return;
        }
        SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        e.putString(KEY, path).apply();
        launch();
    }

    /* content://...tree/primary:Lego/Game -> /storage/emulated/0/Lego/Game */
    private static String toPath(Uri tree) {
        String id = DocumentsContract.getTreeDocumentId(tree);
        int i = id.indexOf(':');
        if (i < 0) return null;
        String vol = id.substring(0, i), rel = id.substring(i + 1);
        String base = vol.equals("primary") ? Environment.getExternalStorageDirectory().getPath() : "/storage/" + vol;
        return rel.isEmpty() ? base : base + "/" + rel;
    }

    private void launch() {
        startActivity(new Intent(this, LegoMarvelActivity.class));
        finish();
    }
}
