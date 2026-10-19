package app.famtv.home;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends Activity {

    static final int PAD = 48;          // screen edge margin, dp
    static final int GAP = 20;          // gap between tiles, dp
    static final int CONTENT_W = 960 - 2 * PAD;

    private final Handler handler = new Handler();
    private final ArgbEvaluator argb = new ArgbEvaluator();
    private final Map<String, Integer> tint = new HashMap<>();

    private AmbientView ambient;
    private ScrollView scroll;
    private LinearLayout content;
    private TextView greeting, date, panchang, heroTitle, heroHint;
    private String lastFocusedPkg;
    private ValueAnimator glowAnim;
    private boolean photoMode;

    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { buildApps(); }
    };

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            Calendar now = Calendar.getInstance();
            // "hour" extra lets you preview other times: am start -n app.famtv.home/.HomeActivity --ei hour 22
            int h = getIntent().getIntExtra("hour", -1);
            if (h < 0 || h > 23) h = now.get(Calendar.HOUR_OF_DAY);
            String hello, icon;
            if (h >= 5 && h < 12)       { hello = "Good morning";   icon = "\u2600\uFE0F"; }        // sun
            else if (h >= 12 && h < 17) { hello = "Good afternoon"; icon = "\uD83C\uDF24\uFE0F"; } // sun behind cloud
            else if (h >= 17 && h < 21) { hello = "Good evening";   icon = "\uD83C\uDF06"; }        // dusk city
            else                        { hello = "Good night";     icon = "\uD83C\uDF19"; }        // crescent moon
            String name = getString(R.string.family_name).trim();
            greeting.setText(hello + (name.isEmpty() ? "" : " " + name) + " " + icon);
            date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now.getTime()));
            if (panchang != null) {
                String t = panchangText(now);
                if (!t.contentEquals(panchang.getText())) panchang.setText(t);   // keeps the scroll going
            }
            handler.postDelayed(this, 30_000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        ambient = new AmbientView(this);
        root.addView(ambient, match());

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFocusable(false);
        scroll.setSmoothScrollingEnabled(true);
        scroll.setClipChildren(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        content.setPadding(dp(PAD), dp(topPad()), dp(PAD), dp(80));
        scroll.addView(content, new ViewGroup.LayoutParams(-1, -2));
        root.addView(scroll, match());
        setContentView(root);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(packagesChanged, f);
        photoMode = photoFile().exists();
        buildApps();
    }

    /** Set by tools/set-background.sh; delete it to go back to plain black. */
    private java.io.File photoFile() {
        java.io.File dir = getExternalFilesDir(null);
        return new java.io.File(dir != null ? dir : getFilesDir(), "background.jpg");
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(tick);
        handler.post(tick);
        java.io.File photo = photoFile();
        ambient.loadPhoto(photo);
        if (photo.exists() != photoMode) {
            photoMode = photo.exists();
            buildApps();
        }
        handler.post(this::restoreFocus);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override protected void onDestroy() {
        unregisterReceiver(packagesChanged);
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        // Home has nowhere to go back to: return to the favourites row instead.
        View v = content.findViewWithTag("fav0");
        if (v != null) v.requestFocus();
        scroll.smoothScrollTo(0, 0);
    }

    // ---------------------------------------------------------------- layout

    private void buildApps() {
        content.removeAllViews();
        // In photo mode the first screen is exactly one screen tall: top bar at the top,
        // favourites pinned to the bottom, and the photo's faces left clear in between.
        LinearLayout page = content;
        if (photoMode) {
            page = new LinearLayout(this);
            page.setOrientation(LinearLayout.VERTICAL);
            page.setClipChildren(false);
            int h = getResources().getDisplayMetrics().heightPixels - dp(topPad()) - dp(30);
            content.addView(page, new LinearLayout.LayoutParams(-1, h));
        }
        String header = getString(R.string.header_text).trim();
        if (!header.isEmpty()) {
            // A line centred at the very top, e.g. a blessing.
            TextView h = text(header, 20, 0xFFFFC46B, "sans-serif-medium");
            h.setGravity(Gravity.CENTER_HORIZONTAL);
            h.setShadowLayer(dp(10), 0, 0, 0xCCFF8A00);
            page.addView(h, new LinearLayout.LayoutParams(-1, -2));
        }
        page.addView(topBar());

        heroTitle = text("", photoMode ? 20 : 46, Color.WHITE,
                photoMode ? "sans-serif-medium" : "sans-serif-light");
        heroHint = text("", 15, 0x99FFFFFF, "sans-serif");
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.addView(heroTitle);
        if (photoMode) {
            page.addView(new View(this), new LinearLayout.LayoutParams(-1, 0, 1));
            hero.setPadding(0, 0, 0, dp(12));
        } else {
            hero.setPadding(0, dp(58), 0, dp(18));
            hero.addView(heroHint);
        }
        page.addView(hero);

        PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> apps = new HashMap<>();
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
            String p = ri.activityInfo.packageName;
            if (!p.equals(getPackageName()) && !apps.containsKey(p)) apps.put(p, ri);
        }
        List<String> hidden = Arrays.asList(getResources().getStringArray(R.array.hidden));
        List<String> favs = new ArrayList<>();
        for (String p : getResources().getStringArray(R.array.favourites)) if (apps.containsKey(p)) favs.add(p);

        int favW = (dp(CONTENT_W) - dp(GAP) * (favs.size() - 1)) / Math.max(1, favs.size());
        LinearLayout favRow = new LinearLayout(this);
        favRow.setClipChildren(false);
        favRow.setTag("favRow");
        for (int i = 0; i < favs.size(); i++) {
            View t = tile(apps.get(favs.get(i)), favW, i == 0 ? "fav0" : null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(favW, favW * 9 / 16);
            if (i > 0) lp.leftMargin = dp(GAP);
            favRow.addView(t, lp);
        }
        page.addView(favRow);

        List<ResolveInfo> rest = new ArrayList<>();
        for (Map.Entry<String, ResolveInfo> e : apps.entrySet())
            if (!favs.contains(e.getKey()) && !hidden.contains(e.getKey())) rest.add(e.getValue());
        Collections.sort(rest, (a, b) -> label(a).compareToIgnoreCase(label(b)));

        TextView heading = text("All apps", 17, 0xB3FFFFFF, "sans-serif-medium");
        heading.setPadding(0, dp(44), 0, dp(14));
        content.addView(heading);

        int cols = getResources().getInteger(R.integer.grid_columns);
        int gap = dp(16);
        int w = (dp(CONTENT_W) - gap * (cols - 1)) / cols;
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(cols);
        grid.setClipChildren(false);
        grid.setUseDefaultMargins(false);
        for (int i = 0; i < rest.size(); i++) {
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = w;
            lp.height = w * 9 / 16;
            lp.leftMargin = i % cols == 0 ? 0 : gap;
            lp.bottomMargin = gap;
            grid.addView(tile(rest.get(i), w, null), lp);
        }
        content.addView(grid);

        wireFocus(favRow, grid, cols);
        handler.post(this::restoreFocus);
    }

    /**
     * Explicit D-pad map so the remote never wanders: Up/Down move exactly one row, the ends of
     * every row are walls, Up from the favourites goes to the top bar.
     */
    private void wireFocus(LinearLayout favRow, GridLayout grid, int cols) {
        View inputs = content.findViewWithTag("pill:Inputs");
        View settings = content.findViewWithTag("pill:Settings");
        int nf = favRow.getChildCount(), ng = grid.getChildCount();
        for (View v : new View[]{inputs, settings}) if (v.getId() == View.NO_ID) v.setId(View.generateViewId());
        for (int i = 0; i < nf; i++) favRow.getChildAt(i).setId(View.generateViewId());
        for (int i = 0; i < ng; i++) grid.getChildAt(i).setId(View.generateViewId());

        inputs.setNextFocusLeftId(inputs.getId());
        inputs.setNextFocusRightId(settings.getId());
        settings.setNextFocusLeftId(inputs.getId());
        settings.setNextFocusRightId(settings.getId());
        for (View v : new View[]{inputs, settings}) {
            v.setNextFocusUpId(v.getId());
            v.setNextFocusDownId(nf > 0 ? favRow.getChildAt(0).getId() : v.getId());
        }

        for (int i = 0; i < nf; i++) {
            View v = favRow.getChildAt(i);
            v.setNextFocusLeftId(favRow.getChildAt(Math.max(0, i - 1)).getId());
            v.setNextFocusRightId(favRow.getChildAt(Math.min(nf - 1, i + 1)).getId());
            v.setNextFocusUpId(inputs.getId());
            int col = Math.min(ng - 1, Math.round(i * (cols - 1) / (float) Math.max(1, nf - 1)));
            v.setNextFocusDownId(ng > 0 ? grid.getChildAt(col).getId() : v.getId());
        }

        for (int i = 0; i < ng; i++) {
            View v = grid.getChildAt(i);
            int row = i / cols, col = i % cols;
            int rowStart = row * cols, rowEnd = Math.min(ng, rowStart + cols) - 1;
            v.setNextFocusLeftId(grid.getChildAt(Math.max(rowStart, i - 1)).getId());
            v.setNextFocusRightId(grid.getChildAt(Math.min(rowEnd, i + 1)).getId());
            if (row == 0) {
                int fav = Math.round(col * (nf - 1) / (float) Math.max(1, cols - 1));
                v.setNextFocusUpId(nf > 0 ? favRow.getChildAt(fav).getId() : inputs.getId());
            } else {
                v.setNextFocusUpId(grid.getChildAt(i - cols).getId());
            }
            int below = Math.min(ng - 1, i + cols);
            v.setNextFocusDownId(below / cols > row ? grid.getChildAt(below).getId() : v.getId());
        }
    }

    private void restoreFocus() {
        View v = lastFocusedPkg == null ? null : content.findViewWithTag(lastFocusedPkg);
        if (v == null) v = content.findViewWithTag("fav0");
        if (v != null) v.requestFocus();
    }

    /**
     * Two rows on shared edges:  greeting ........ [Inputs] [Settings]  clock
     *                            [panchangam card] ..................... date
     */
    private View topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        greeting = text("", 20, Color.WHITE, "sans-serif");
        row1.addView(greeting, new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(pill("Inputs", v -> showInputs()));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
        sp.leftMargin = dp(12);
        row1.addView(pill("Settings", v -> launch(new Intent(Settings.ACTION_SETTINGS))), sp);
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        clock.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        clock.setIncludeFontPadding(false);
        clock.setShadowLayer(dp(8), 0, dp(1), 0xE6000000);
        clock.setGravity(Gravity.END);
        clock.setMinWidth(dp(150));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
        cp.leftMargin = dp(28);
        row1.addView(clock, cp);
        bar.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        View card = panchangCard();
        if (card != null) row2.addView(card, card.getLayoutParams());
        row2.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        date = text("", 14, 0xB3FFFFFF, "sans-serif");
        date.setGravity(Gravity.END);
        row2.addView(date);
        LinearLayout.LayoutParams r2 = new LinearLayout.LayoutParams(-1, -2);
        r2.topMargin = dp(4);
        bar.addView(row2, r2);
        return bar;
    }

    /** Less room above the top bar when a header line takes the top of the screen. */
    private int topPad() {
        return getString(R.string.header_text).trim().isEmpty() ? 36 : 12;
    }

    /** The panchangam in a soft glass "cloud" card, or null when it's switched off. */
    private View panchangCard() {
        if (!getResources().getBoolean(R.bool.show_panchang)) return null;
        panchang = text("", 14, 0xF2FFFFFF, "sans-serif");
        // A single line that scrolls right-to-left forever, like a ticker.
        panchang.setSingleLine(true);
        panchang.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        panchang.setMarqueeRepeatLimit(-1);
        panchang.setHorizontalFadingEdgeEnabled(true);
        panchang.setFadingEdgeLength(dp(24));
        panchang.setSelected(true);
        panchang.setShadowLayer(0, 0, 0, 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(0x8C101018);
        bg.setStroke(dp(1), 0x33FFFFFF);
        panchang.setBackground(bg);
        panchang.setPadding(dp(16), dp(8), dp(16), dp(8));
        panchang.setLayoutParams(new LinearLayout.LayoutParams(dp(380), -2));
        return panchang;
    }

    private View pill(String label, View.OnClickListener click) {
        TextView t = text(label, 15, Color.WHITE, "sans-serif-medium");
        t.setPadding(dp(18), dp(8), dp(18), dp(8));
        t.setFocusable(true);
        t.setTag("pill:" + label);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(0x26FFFFFF);
        t.setBackground(bg);
        t.setOnClickListener(click);
        t.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? Color.WHITE : 0x26FFFFFF);
            t.setTextColor(has ? Color.BLACK : Color.WHITE);
            v.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).setDuration(150).start();
            if (has) {
                Log.d("FamTV", "focus pill " + label);
                heroTitle.setText("");
                heroHint.setText("");
                scroll.smoothScrollTo(0, 0);
            }
        });
        return t;
    }

    private View tile(ResolveInfo ri, int width, String tag) {
        String pkg = ri.activityInfo.packageName;
        String name = label(ri);
        FrameLayout t = new FrameLayout(this);
        t.setTag(tag != null ? tag : pkg);
        t.setFocusable(true);
        t.setClipToOutline(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(0xFF1C1C1E);
        t.setBackground(bg);

        Drawable banner = ri.activityInfo.loadBanner(getPackageManager());
        if (banner == null) banner = ri.activityInfo.applicationInfo.loadBanner(getPackageManager());
        if (banner != null) {
            View img = new View(this);
            img.setBackground(banner);
            t.addView(img, match());
        } else {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER);
            View icon = new View(this);
            icon.setBackground(ri.loadIcon(getPackageManager()));
            int s = width / 4;
            box.addView(icon, new LinearLayout.LayoutParams(s, s));
            TextView l = text(name, 13, Color.WHITE, "sans-serif-medium");
            l.setPadding(0, dp(6), 0, 0);
            box.addView(l);
            t.addView(box, match());
        }

        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(dp(10));
        ring.setStroke(dp(3), Color.WHITE);

        Drawable art = banner != null ? banner : ri.loadIcon(getPackageManager());
        t.setOnFocusChangeListener((v, has) -> {
            v.animate().scaleX(has ? 1.1f : 1f).scaleY(has ? 1.1f : 1f).translationZ(has ? dp(12) : 0)
                    .setDuration(180).start();
            t.setForeground(has ? ring : null);
            if (has) {
                Log.d("FamTV", "focus " + name);
                lastFocusedPkg = (String) v.getTag();
                boolean fav = "favRow".equals(((View) v.getParent()).getTag());
                // In photo mode the label sits right above the favourites, so only label those.
                heroTitle.setText(photoMode && !fav ? "" : name);
                heroHint.setText("Press OK to open");
                glowTo(colorOf(pkg, art));
                if (fav) scroll.smoothScrollTo(0, 0);
            }
        });
        t.setOnClickListener(v -> {
            Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) i = new Intent(Intent.ACTION_MAIN)
                    .setComponent(new ComponentName(pkg, ri.activityInfo.name));
            launch(i);
        });
        return t;
    }

    // ---------------------------------------------------------------- inputs

    private void showInputs() {
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        List<String> labels = new ArrayList<>();
        List<Intent> intents = new ArrayList<>();
        String liveTv = getString(R.string.live_tv_package);
        if (tim != null) {
            List<TvInputInfo> list = new ArrayList<>(tim.getTvInputList());
            Collections.sort(list, (a, b) -> a.getId().compareTo(b.getId()));
            for (TvInputInfo in : list) {
                if (in.isPassthroughInput()) {
                    CharSequence l = in.loadCustomLabel(this);
                    if (l == null) l = in.loadLabel(this);
                    boolean off = tim.getInputState(in.getId()) == TvInputManager.INPUT_STATE_DISCONNECTED;
                    labels.add(l + (off ? "   ·  nothing connected" : ""));
                    intents.add(new Intent(Intent.ACTION_VIEW,
                            TvContract.buildChannelUriForPassthroughInput(in.getId())));
                }
            }
        }
        Intent tv = getPackageManager().getLeanbackLaunchIntentForPackage(liveTv);
        if (tv != null) {
            labels.add("TV (antenna / cable)");
            intents.add(tv);
        }
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Inputs")
                .setItems(labels.toArray(new CharSequence[0]), (d, which) -> launch(intents.get(which)))
                .show();
    }

    // ---------------------------------------------------------------- helpers

    private static final String SEP = "   ✦   ";

    /** One line for the scrolling card: year · month · fortnight · tithi · nakshatram. */
    private String panchangText(Calendar now) {
        Panchang p = Panchang.at(now.getTimeInMillis());
        if (getResources().getBoolean(R.bool.panchang_telugu)) {
            return "శ్రీ " + p.samvatsaramTe() + " నామ సంవత్సరం" + SEP + p.ruthuvuTe() + " ఋతువు" + SEP + p.masamTe() + " మాసం" + SEP
                    + p.pakshamTe() + " పక్షం" + SEP
                    + "తిథి: " + p.tithiTe() + " " + untilTe(now, p.tithiEndsMs) + SEP
                    + "నక్షత్రం: " + p.nakshatramTe() + " " + untilTe(now, p.nakshatramEndsMs);
        }
        return "Sri " + p.samvatsaramName() + " nama samvatsaram" + SEP + p.ruthuvuName() + " ruthuvu" + SEP + p.masamName() + " masam" + SEP
                + p.paksham() + " paksham" + SEP
                + "Tithi: " + p.tithiName() + " till " + untilEn(now, p.tithiEndsMs) + SEP
                + "Nakshatram: " + p.nakshatramName() + " till " + untilEn(now, p.nakshatramEndsMs);
    }

    private static int daysAhead(Calendar now, Calendar end) {
        int days = end.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR);
        return days < 0 ? days + now.getActualMaximum(Calendar.DAY_OF_YEAR) : days;
    }

    /** "10:51 AM tomorrow" */
    private static String untilEn(Calendar now, long ms) {
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(ms);
        int days = daysAhead(now, end);
        return new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(end.getTime())
                + (days == 0 ? "" : days == 1 ? " tomorrow"
                : " " + new SimpleDateFormat("EEE", Locale.ENGLISH).format(end.getTime()));
    }

    /** Panchangam style: "రేపు ఉ. 10:51 వరకు" (ఉ. morning, మ. afternoon, సా. evening, రా. night). */
    private static String untilTe(Calendar now, long ms) {
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(ms);
        int h = end.get(Calendar.HOUR_OF_DAY);
        String part = h >= 4 && h < 12 ? "ఉ." : h >= 12 && h < 15 ? "మ." : h >= 15 && h < 19 ? "సా." : "రా.";
        int days = daysAhead(now, end);
        String day = days == 0 ? "" : days == 1 ? "రేపు " : "ఎల్లుండి ";
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return day + part + " " + h12 + ":" + String.format(Locale.ENGLISH, "%02d", end.get(Calendar.MINUTE)) + " వరకు";
    }

    private void launch(Intent i) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            heroHint.setText("Couldn't open that");
        }
    }

    private void glowTo(int color) {
        if (glowAnim != null) glowAnim.cancel();
        glowAnim = ValueAnimator.ofObject(argb, ambient.color, color);
        glowAnim.setDuration(450);
        glowAnim.addUpdateListener(a -> ambient.setColor((Integer) a.getAnimatedValue()));
        glowAnim.start();
    }

    private int colorOf(String pkg, Drawable d) {
        Integer cached = tint.get(pkg);
        if (cached != null) return cached;
        int c = AmbientView.dominant(d);
        tint.put(pkg, c);
        return c;
    }

    private String label(ResolveInfo ri) {
        return String.valueOf(ri.loadLabel(getPackageManager()));
    }

    private TextView text(String s, int sp, int color, String family) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(family, Typeface.NORMAL));
        t.setSingleLine(true);
        t.setShadowLayer(dp(8), 0, dp(1), 0xE6000000);   // keeps text readable over a photo
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(-1, -1);
    }
}
