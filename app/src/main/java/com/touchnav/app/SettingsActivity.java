package com.touchnav.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends Activity {

    private SettingsManager settings;
    private ProfileManager  profiles;
    private LinearLayout    container;

    private static final int[] COLORS = {
        0xFFFFFFFF, 0xFF64B5F6, 0xFF80CBC4, 0xFFCE93D8,
        0xFFFFD54F, 0xFFEF9A9A, 0xFFA5D6A7, 0xFFFF8A65,
        0xFF90A4AE, 0xFFFFCC02, 0xFFFF7043, 0xFF26C6DA
    };

    private List<AppItem> cachedApps = null;

    static class AppItem {
        String name, pkg; Drawable icon;
        AppItem(String n, String p, Drawable d){ name=n; pkg=p; icon=d; }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);
        settings  = new SettingsManager(this);
        profiles  = new ProfileManager(this);
        L.init(this);
        container = findViewById(R.id.settings_container);
        ((TextView)findViewById(R.id.btn_back)).setOnClickListener(v -> finish());
        ((TextView)findViewById(R.id.btn_theme_toggle_s)).setOnClickListener(v -> {
            settings.setTheme((settings.getTheme()+1)%3); applyTheme(); buildCards();
        });
        applyTheme();
        buildCards();
    }

    // ── Tema ─────────────────────────────────────────────────────
    private int t()      { return settings.getTheme(); }
    private int bg()     { return t()==1?0xFFF0F0F5:t()==2?0xFF000000:0xFF0D0D14; }
    private int card()   { return t()==1?0xFFFFFFFF:t()==2?0xFF0A0A0A:0xFF1A1A2E; }
    private int text()   { return t()==1?0xFF1A1A1A:0xFFE0E0E0; }
    private int sub()    { return t()==1?0xFF777777:0xFF888899; }
    private int accent() { return 0xFF64B5F6; }
    private int border() { return t()==1?0xFFDDDDEE:0xFF2A2A3E; }

    private void applyTheme() {
        View root=findViewById(R.id.root_settings), hdr=findViewById(R.id.settings_header),
             hdiv=findViewById(R.id.header_divider);
        ((TextView)findViewById(R.id.tv_settings_title)).setTextColor(text());
        ((TextView)findViewById(R.id.tv_settings_sub)).setTextColor(sub());
        ((TextView)findViewById(R.id.btn_back)).setTextColor(text());
        TextView th=findViewById(R.id.btn_theme_toggle_s);
        th.setText(t()==0?"☀":t()==1?"🌙":"⬛");
        th.setTextColor(t()==0?0xFFFFCC44:t()==1?0xFF335577:0xFFAAAAAA);
        root.setBackgroundColor(bg()); hdr.setBackgroundColor(bg());
        hdiv.setBackgroundColor(border()); container.setBackgroundColor(bg());
    }

    // ── Kart builder ─────────────────────────────────────────────
    private LinearLayout makeCard(String title, String helpText) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(px(16),px(14),px(16),px(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(card()); bg.setCornerRadius(px(16)); bg.setStroke(px(1),border());
        card.setBackground(bg);
        if (title != null) {
            LinearLayout hr = new LinearLayout(this);
            hr.setOrientation(LinearLayout.HORIZONTAL);
            hr.setGravity(Gravity.CENTER_VERTICAL);
            hr.setPadding(0,0,0,px(12));
            TextView tv=new TextView(this); tv.setText(title);
            tv.setTextSize(12); tv.setTextColor(sub());
            tv.setTypeface(null,Typeface.BOLD); tv.setLetterSpacing(0.1f);
            hr.addView(tv, new LinearLayout.LayoutParams(0,-2,1f));
            if (helpText!=null && !helpText.isEmpty()) {
                hr.addView(makeHelpBtn(title,helpText));
            }
            card.addView(hr);
        }
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,0,0,px(12));
        container.addView(card,lp);
        return card;
    }

    private TextView makeHelpBtn(String title, String msg) {
        TextView h=new TextView(this); h.setText(" ? ");
        h.setTextSize(10); h.setTextColor(accent()); h.setGravity(Gravity.CENTER);
        GradientDrawable bg=new GradientDrawable(); bg.setCornerRadius(px(7));
        bg.setColor(t()==1?0xFFE0EDF8:0xFF1E2A3A); bg.setStroke(px(1),0x3364B5F6);
        h.setBackground(bg); h.setPadding(px(5),px(2),px(5),px(2));
        h.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle(title).setMessage(msg).setPositiveButton(L.ok(),null).show());
        return h;
    }

    // ── Slider ───────────────────────────────────────────────────
    private void addSlider(LinearLayout p, String label, String help,
                           int min, int max, int cur, String unit, OnInt cb) {
        LinearLayout hr=new LinearLayout(this); hr.setOrientation(LinearLayout.HORIZONTAL);
        hr.setGravity(Gravity.CENTER_VERTICAL);
        TextView lbl=new TextView(this); lbl.setTextColor(text()); lbl.setTextSize(13);
        lbl.setPadding(0,px(4),0,0); lbl.setText(label+": "+cur+unit);
        hr.addView(lbl, new LinearLayout.LayoutParams(0,-2,1f));
        if (help!=null) hr.addView(makeHelpBtn(label,help));
        p.addView(hr);
        SeekBar bar=new SeekBar(this); bar.setMax(max-min); bar.setProgress(cur-min);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,px(4),0,px(14)); p.addView(bar,lp);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int prog,boolean u){
                int v=prog+min; lbl.setText(label+": "+v+unit); cb.on(v); }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
    }

    private void addSliderF(LinearLayout p, String label, String help,
                            float min, float max, float step, float cur, String unit, OnInt cb) {
        int steps=Math.round((max-min)/step);
        int prog=Math.max(0,Math.min(steps,Math.round((cur-min)/step)));
        LinearLayout hr=new LinearLayout(this); hr.setOrientation(LinearLayout.HORIZONTAL);
        hr.setGravity(Gravity.CENTER_VERTICAL);
        TextView lbl=new TextView(this); lbl.setTextColor(text()); lbl.setTextSize(13);
        lbl.setPadding(0,px(4),0,0); lbl.setText(String.format("%s: %.1f%s",label,cur,unit));
        hr.addView(lbl, new LinearLayout.LayoutParams(0,-2,1f));
        if (help!=null) hr.addView(makeHelpBtn(label,help));
        p.addView(hr);
        SeekBar bar=new SeekBar(this); bar.setMax(steps); bar.setProgress(prog);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,px(4),0,px(14)); p.addView(bar,lp);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int prog2,boolean u){
                float v=min+prog2*step;
                lbl.setText(String.format("%s: %.1f%s",label,v,unit));
                cb.on(Math.round(v*1000)); }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
    }

    private View addToggle(LinearLayout p, String label, String help, boolean val, OnBool cb) {
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rLp=new LinearLayout.LayoutParams(-1,-2);
        rLp.setMargins(0,0,0,px(10)); p.addView(row,rLp);
        TextView tv=new TextView(this); tv.setText(label); tv.setTextColor(text()); tv.setTextSize(13);
        row.addView(tv, new LinearLayout.LayoutParams(0,-2,1f));
        if (help!=null && !help.isEmpty()) {
            row.addView(makeHelpBtn(label,help));
            View sp=new View(this); row.addView(sp, new LinearLayout.LayoutParams(px(6),1));
        }
        View tog=new View(this); colorToggle(tog,val);
        row.addView(tog, new LinearLayout.LayoutParams(px(46),px(26)));
        final boolean[] st={val};
        tog.setOnClickListener(v->{ st[0]=!st[0]; colorToggle(tog,st[0]); haptic(v); cb.on(st[0]); });
        return tog;
    }

    private void colorToggle(View v, boolean on) {
        GradientDrawable bg=new GradientDrawable(); bg.setCornerRadius(px(13));
        bg.setColor(on?accent():(t()==1?0xFFCCCCDD:0xFF2A2A3E));
        bg.setStroke(px(1),t()==1?0xFFBBBBCC:0xFF3A3A4E); v.setBackground(bg);
        v.setPadding(on?px(22):px(2),px(2),on?px(2):px(22),px(2)); v.invalidate();
    }

    private void addSpinner(LinearLayout p, String label, String help,
                            String[] items, int sel, OnInt cb) {
        if (label!=null) {
            LinearLayout hr=new LinearLayout(this); hr.setOrientation(LinearLayout.HORIZONTAL);
            hr.setGravity(Gravity.CENTER_VERTICAL);
            TextView lbl=new TextView(this); lbl.setText(label); lbl.setTextColor(sub());
            lbl.setTextSize(12); lbl.setPadding(0,px(4),0,px(2));
            hr.addView(lbl, new LinearLayout.LayoutParams(0,-2,1f));
            if (help!=null) hr.addView(makeHelpBtn(label,help));
            p.addView(hr);
        }
        Spinner sp=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
        sp.setSelection(Math.max(0, Math.min(sel, items.length-1)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,0,0,px(10)); p.addView(sp,lp);
        final boolean[] first={true};
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> pr,View v,int pos,long id){
                if(first[0]){first[0]=false;return;} cb.on(pos); }
            public void onNothingSelected(AdapterView<?> pr){}
        });
    }

    private void addBtn(LinearLayout p, String label, Runnable action) {
        TextView tv=new TextView(this); tv.setText(label); tv.setTextSize(12); tv.setTextColor(accent());
        tv.setPadding(px(10),px(8),px(10),px(8)); tv.setTypeface(null,Typeface.BOLD);
        GradientDrawable bg=new GradientDrawable(); bg.setCornerRadius(px(10));
        bg.setColor(t()==1?0xFFE0EDF8:0xFF1E2A3A); bg.setStroke(px(1),0x3364B5F6);
        tv.setBackground(bg); tv.setOnClickListener(v->{haptic(v);action.run();});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);
        lp.setMargins(0,0,px(8),0); p.addView(tv,lp);
    }

    // ── App listesi ──────────────────────────────────────────────
    private List<AppItem> getLauncherApps() {
        if (cachedApps!=null) return cachedApps;
        PackageManager pm=getPackageManager();
        Intent intent=new Intent(Intent.ACTION_MAIN,null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list=pm.queryIntentActivities(intent,0);
        List<AppItem> user=new ArrayList<>(), sys=new ArrayList<>();
        for (ResolveInfo ri:list) {
            String pkg=ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            ApplicationInfo ai=ri.activityInfo.applicationInfo;
            boolean isSystem=(ai.flags&ApplicationInfo.FLAG_SYSTEM)!=0;
            boolean isUpdated=(ai.flags&ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)!=0;
            AppItem item=new AppItem(ri.loadLabel(pm).toString(),pkg,ri.loadIcon(pm));
            if (!isSystem||isUpdated) user.add(item); else sys.add(item);
        }
        Collections.sort(user,(a,b)->a.name.compareToIgnoreCase(b.name));
        Collections.sort(sys,(a,b)->a.name.compareToIgnoreCase(b.name));
        cachedApps=new ArrayList<>(); cachedApps.addAll(user); cachedApps.addAll(sys);
        return cachedApps;
    }

    private void showMultiAppPicker(TextView pickerBtn) {
        List<AppItem> apps=getLauncherApps(); int count=apps.size();
        String[] names=new String[count]; boolean[] checked=new boolean[count];
        Set<String> savedSet=new HashSet<>();
        String saved=settings.getHidePackages();
        if (!saved.isEmpty()) for (String p:saved.split(",")) { String t=p.trim(); if(!t.isEmpty()) savedSet.add(t); }
        for (int i=0;i<count;i++) { names[i]=apps.get(i).name; checked[i]=savedSet.contains(apps.get(i).pkg); }
        final boolean[] sel=checked.clone();
        new AlertDialog.Builder(this).setTitle(L.hideApps())
            .setMultiChoiceItems(names,sel,(d,w,c)->sel[w]=c)
            .setPositiveButton(L.ok(),(d,w)->{
                StringBuilder sb=new StringBuilder();
                for(int i=0;i<count;i++) if(sel[i]){if(sb.length()>0)sb.append(",");sb.append(apps.get(i).pkg);}
                settings.setHidePackages(sb.toString());
                int n=0; for(boolean bv:sel) if(bv) n++;
                pickerBtn.setText(n==0?L.selectApp():n+L.appsSelected());
                pickerBtn.setTextColor(n==0?sub():accent());
            }).setNegativeButton(L.cancel(),null).show();
    }

    // ── Kartlar ───────────────────────────────────────────────────
    private void buildCards() {
        container.removeAllViews();
        buildLanguageCard();
        buildThemeCard();
        buildProfileCard();
        buildStyleCard();
        buildShapeCard();
        buildIconCard();
        buildAppearanceCard();
        buildTransparencyCard();
        buildVibrationCard();
        buildTimingCard();
        buildOptionsCard();
        buildGesturesCard();
        buildDrawCard();
        if (anyGestureIsAssistant()) buildAssistantGestureCard();
        buildKeyboardCard();
        buildBatteryCard();
        buildNotifCard();
    }

    private String[] actions() { return L.actionLabels(); }

    private void buildLanguageCard() {
        LinearLayout card=makeCard(L.language(),null);
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(row);
        String[] langs={"Türkçe","English"}; String cur=settings.getLanguage();
        for (int i=0;i<2;i++) {
            final String lc=i==0?"tr":"en";
            TextView tv=new TextView(this); tv.setText(langs[i]); tv.setTextSize(13); tv.setGravity(Gravity.CENTER);
            tv.setPadding(px(12),px(8),px(12),px(8));
            boolean sel=cur.equals(lc);
            GradientDrawable bg=new GradientDrawable(); bg.setCornerRadius(px(10));
            bg.setColor(sel?accent():card()); bg.setStroke(px(sel?2:1),sel?0xFFFFFFFF:border());
            tv.setBackground(bg); tv.setTextColor(sel?0xFF0A0A1A:text());
            tv.setOnClickListener(v->{settings.setLanguage(lc);L.init(this);haptic(v);buildCards();});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f);
            lp.setMargins(0,0,i==0?px(8):0,0); row.addView(tv,lp);
        }
    }

    private void buildThemeCard() {
        LinearLayout card=makeCard(L.cardTheme(),
            L.isTr()?"Uygulamanın renk temasını değiştirir. Midnight koyu, Açık beyaz, AMOLED tam siyah."
                    :"Changes the app color theme. Midnight=dark, Light=white, AMOLED=pure black.");
        String[] names=L.themeNames();
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); card.addView(row);
        for (int i=0;i<3;i++) {
            final int idx=i; boolean sel=settings.getTheme()==i;
            TextView tv=new TextView(this); tv.setText(names[i]); tv.setTextSize(12); tv.setGravity(Gravity.CENTER);
            tv.setPadding(px(8),px(10),px(8),px(10));
            GradientDrawable bg=new GradientDrawable(); bg.setCornerRadius(px(10));
            bg.setColor(sel?accent():card()); bg.setStroke(px(sel?2:1),sel?0xFFFFFFFF:border());
            tv.setBackground(bg); tv.setTextColor(sel?0xFF0A0A1A:text());
            tv.setOnClickListener(v->{settings.setTheme(idx);haptic(v);applyTheme();buildCards();});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f);
            lp.setMargins(0,0,i<2?px(6):0,0); row.addView(tv,lp);
        }
    }

    private void buildProfileCard() {
        LinearLayout card=makeCard(L.cardProfile(),
            L.isTr()?"Farklı ayar setlerini profil olarak kaydedip anında geçiş yapabilirsin."
                    :"Save different setting sets as profiles and switch instantly.");
        List<String> pList=profiles.getProfiles(); String[] pArr=pList.toArray(new String[0]);
        int cur=0; String curName=settings.getCurrentProfile();
        for(int i=0;i<pArr.length;i++) if(pArr[i].equals(curName)){cur=i;break;}
        addSpinner(card,L.activeProfile(),null,pArr,cur,pos->{profiles.loadProfile(pArr[pos]);sendRefresh();buildCards();});
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); card.addView(row);
        addBtn(row,L.save(),()->{profiles.saveProfile(settings.getCurrentProfile());toast(L.saved());});
        addBtn(row,L.newWord(),this::showNewProfileDialog);
        addBtn(row,L.delete(),()->{if(!profiles.deleteProfile(settings.getCurrentProfile()))toast(L.cantDelete());else buildCards();});
    }

    private void buildStyleCard() {
        LinearLayout card=makeCard(L.cardStyle(),
            L.isTr()?"Butonun görsel stilini seçer. Neon=parlak, Crystal=şeffaf dolgu, Plasma=çift halka."
                    :"Button visual style. Neon=glowing, Crystal=transparent fill, Plasma=double ring.");
        String[] names=L.styleNames();
        int perRow=4;
        int rowCount=(int)Math.ceil(names.length/(double)perRow);
        LinearLayout[] rows=new LinearLayout[rowCount];
        for(int r2=0;r2<rowCount;r2++){
            rows[r2]=new LinearLayout(this); rows[r2].setOrientation(LinearLayout.HORIZONTAL);
        }
        View[] cells=new View[names.length];
        for (int i=0;i<names.length;i++) {
            final int si=i;
            LinearLayout cell=new LinearLayout(this); cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER); cell.setPadding(px(3),px(7),px(3),px(7)); cells[i]=cell;
            boolean sel=settings.getButtonStyle()==si;
            GradientDrawable cb=new GradientDrawable(); cb.setCornerRadius(px(10));
            cb.setColor(sel?(t()==1?0xFFDDEEFF:0xFF1A2A3A):card());
            cb.setStroke(px(sel?2:1),sel?accent():border()); cell.setBackground(cb);
            cell.addView(makeStylePreview(si,settings.getButtonColor()), new LinearLayout.LayoutParams(px(38),px(38)));
            TextView nm=new TextView(this); nm.setText(names[si]); nm.setTextSize(9); nm.setGravity(Gravity.CENTER);
            nm.setTextColor(sel?accent():sub()); cell.addView(nm);
            cell.setOnClickListener(v->{
                settings.setButtonStyle(si); sendRefresh(); haptic(v);
                for(int j=0;j<names.length;j++){
                    boolean s=settings.getButtonStyle()==j;
                    GradientDrawable d=new GradientDrawable(); d.setCornerRadius(px(10));
                    d.setColor(s?(t()==1?0xFFDDEEFF:0xFF1A2A3A):card()); d.setStroke(px(s?2:1),s?accent():border());
                    cells[j].setBackground(d);
                    ((TextView)((LinearLayout)cells[j]).getChildAt(1)).setTextColor(s?accent():sub());
                }
            });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f); lp.setMargins(0,0,px(3),0);
            int rowIdx=i/perRow;
            rows[rowIdx].addView(cell,lp);
        }
        for(int r2=0;r2<rowCount;r2++){
            LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,-2);
            rlp.setMargins(0,0,0,r2<rowCount-1?px(6):0);
            card.addView(rows[r2],rlp);
        }
        // Renk
        TextView cLbl=new TextView(this); cLbl.setText(L.color()); cLbl.setTextColor(sub()); cLbl.setTextSize(12);
        cLbl.setPadding(0,px(14),0,px(8)); card.addView(cLbl);
        android.widget.HorizontalScrollView hsv=new android.widget.HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout cRow=new LinearLayout(this); cRow.setOrientation(LinearLayout.HORIZONTAL);
        cRow.setPadding(0,px(4),px(16),px(4));
        View[] dots=new View[COLORS.length];
        for(int i=0;i<COLORS.length;i++) {
            final int color=COLORS[i]; View dot=new View(this);
            boolean sel=settings.getButtonColor()==color;
            GradientDrawable dg=new GradientDrawable(); dg.setShape(GradientDrawable.OVAL); dg.setColor(color);
            dg.setStroke(px(sel?3:1),sel?0xFFFFFFFF:0x44FFFFFF); dot.setBackground(dg); dots[i]=dot;
            dot.setOnClickListener(v->{
                settings.setButtonColor(color); sendRefresh(); haptic(v);
                for(int j=0;j<COLORS.length;j++){
                    boolean s=settings.getButtonColor()==COLORS[j];
                    GradientDrawable d2=new GradientDrawable(); d2.setShape(GradientDrawable.OVAL);
                    d2.setColor(COLORS[j]); d2.setStroke(px(s?3:1),s?0xFFFFFFFF:0x44FFFFFF); dots[j].setBackground(d2);
                }
                buildCards();
            });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(px(34),px(34)); lp.setMargins(0,0,px(10),0);
            cRow.addView(dot,lp);
        }
        hsv.addView(cRow); card.addView(hsv);
        addToggle(card,L.pulseAnim(),L.isTr()?"Buton hafifçe nefes alıyor gibi büyüyüp küçülür.":"Button gently pulses in and out.",
            settings.isPulseEnabled(),v->{settings.setPulseEnabled(v);sendRefresh();});
        addToggle(card,L.actionFlash(),L.isTr()?"Aksiyon tetiklenince buton beyaza parlar.":"Button flashes white when action fires.",
            settings.isActionFlash(),v->settings.setActionFlash(v));
    }

    private void buildShapeCard() {
        LinearLayout card=makeCard(L.cardShape(),
            L.isTr()?"Butonun dış şeklini seçer: daire, yuvarlatılmış kare, damla, altıgen."
                    :"Button outer shape: circle, rounded square, drop or hexagon.");
        String[] names=L.shapeNames();
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        View[] cells=new View[4];
        for(int i=0;i<4;i++){
            final int si=i; boolean sel=settings.getButtonShape()==si;
            LinearLayout cell=new LinearLayout(this); cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER); cell.setPadding(px(4),px(8),px(4),px(8)); cells[i]=cell;
            GradientDrawable cb=new GradientDrawable(); cb.setCornerRadius(px(10));
            cb.setColor(sel?(t()==1?0xFFDDEEFF:0xFF1A2A3A):card()); cb.setStroke(px(sel?2:1),sel?accent():border());
            cell.setBackground(cb);
            cell.addView(makeShapePreview(si,settings.getButtonColor()), new LinearLayout.LayoutParams(px(40),px(40)));
            TextView nm=new TextView(this); nm.setText(names[si]); nm.setTextSize(10); nm.setGravity(Gravity.CENTER);
            nm.setTextColor(sel?accent():sub()); cell.addView(nm);
            cell.setOnClickListener(v->{
                settings.setButtonShape(si); sendRefresh(); haptic(v);
                for(int j=0;j<4;j++){
                    boolean s=settings.getButtonShape()==j;
                    GradientDrawable d=new GradientDrawable(); d.setCornerRadius(px(10));
                    d.setColor(s?(t()==1?0xFFDDEEFF:0xFF1A2A3A):card()); d.setStroke(px(s?2:1),s?accent():border());
                    cells[j].setBackground(d);
                    ((TextView)((LinearLayout)cells[j]).getChildAt(1)).setTextColor(s?accent():sub());
                }
            });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f); lp.setMargins(0,0,px(4),0);
            row.addView(cell,lp);
        }
        card.addView(row);
    }

    private void buildIconCard() {
        LinearLayout card=makeCard(L.cardIcon(),
            L.isTr()?"Butonun ortasında görünen küçük simge. Nokta, pusula veya halka."
                    :"Small icon inside the button: dot, nav compass or ring.");
        addSpinner(card,null,null,L.iconNames(),settings.getButtonIcon(),v->{settings.setButtonIcon(v);sendRefresh();});
    }

    private void buildAppearanceCard() {
        LinearLayout card=makeCard(L.cardAppearance(),
            L.isTr()?"Butonun opaklığını ve boyutunu ayarlar. Değişiklikler anlık yansır."
                    :"Adjusts button opacity and size. Changes apply live.");
        addSlider(card,L.opacity(),L.isTr()?"Düşük = daha şeffaf.":"Lower = more transparent.",
            20,100,(int)(settings.getOpacity()*100),"%",v->{settings.setOpacity(v/100f);sendRefresh();});
        addSlider(card,L.size(),L.isTr()?"Butonun ekrandaki boyutu (dp).":"Button size on screen (dp).",
            40,140,settings.getSize(),"dp",v->{settings.setSize(v);sendRefresh();});
    }

    private void buildTransparencyCard() {
        LinearLayout card=makeCard(L.cardTransparency(),
            L.isTr()?"Bir harekete 'Gizlilik' atandığında buton tamamen kaybolur, dokunulamaz. Fotoğraf çekerken kullanışlı."
                    :"When 'Privacy' is assigned to a gesture, button becomes fully invisible and untouchable.");
        addToggle(card,L.transparencyToggle(),
            L.isTr()?"Harekete atanabilmesi için açık olmalı.":"Must be on to assign to a gesture.",
            settings.isTransparencyShortcut(),v->{settings.setTransparencyShortcut(v);sendRefresh();});
        addSliderF(card,L.transparencyDur(),
            L.isTr()?"Bu kadar saniye sonra buton tekrar görünür olur.":"Button reappears after this many seconds.",
            1f,30f,1f,settings.getTransparencyDuration()/1000f,"s",v->settings.setTransparencyDuration(v));
    }

    private void buildVibrationCard() {
        LinearLayout card=makeCard(L.cardVibration(),
            L.isTr()?"Butona dokunulduğunda titreşim geri bildirimi verir. Erişilebilirlik servisi aktif olmalı."
                    :"Haptic feedback on touch. Accessibility service must be active.");
        TextView note=new TextView(this); note.setText(L.vibNote()); note.setTextColor(sub());
        note.setTextSize(11); note.setPadding(0,0,0,px(8)); card.addView(note);
        addToggle(card,L.vibActive(),L.isTr()?"Titreşim geri bildirimini açar/kapatır.":"Enables/disables haptic feedback.",
            settings.isVibrationEnabled(),v->{settings.setVibrationEnabled(v);sendRefresh();});
        // 0-100 slider — doğrudan vib_level olarak saklanır
        addSlider(card,L.vibDuration(),
            L.isTr()?"0=kısa tık, 50=orta, 100=güçlü uzun titreşim.":"0=short tap, 50=medium, 100=strong long vibration.",
            0,100,settings.getVibrationLevel(),"",
            v->settings.setVibrationLevel(v));
    }

    private void buildTimingCard() {
        LinearLayout card=makeCard(L.cardTiming(),
            L.isTr()?"Long press, geri dönüş ve ghost mod zamanlamalarını ayarlar."
                    :"Long press, return and ghost fade timings.");
        addSliderF(card,L.longPress(),L.isTr()?"Ne kadar tutunca long press sayılır.":"How long to hold for long press.",
            0.10f,1.50f,0.05f,settings.getLongPressMs()/1000f,"s",v->{settings.setLongPressMs(v);sendRefresh();});
        addSliderF(card,L.returnDelay(),
            L.isTr()?"Konum kilitliyken taşındıktan sonra ne kadar bekleyince geri döner."
                    :"Wait before returning to home when position is locked.",
            0.50f,5.00f,0.25f,settings.getTempMoveDelay()/1000f,"s",v->{settings.setTempMoveDelay(v);sendRefresh();});
    }

    private void buildOptionsCard() {
        LinearLayout card=makeCard(L.cardOptions(),
            L.isTr()?"Genel seçenekler: konum kilidi, ghost mod ve otomatik gizleme."
                    :"General: position lock, ghost mode and auto-hide.");
        addToggle(card,L.posLock(),L.isTr()?"Butonu sabit konuma kilitler, sürüklemeyi engeller.":"Locks button in place, prevents dragging.",
            settings.isLocked(),v->{settings.setLocked(v);sendRefresh();});

        // Ghost mod toggle + inline fade süresi
        final LinearLayout ghostExtra=new LinearLayout(this); ghostExtra.setOrientation(LinearLayout.VERTICAL);
        ghostExtra.setVisibility(settings.isGhostMode()?View.VISIBLE:View.GONE);
        addSliderF(ghostExtra,L.fadeDelay(),
            L.isTr()?"Bu kadar saniye hareketsiz kalınca buton soluklaşır.":"Button fades after this many idle seconds.",
            0.50f,10.00f,0.50f,settings.getIdleAlphaTime()/1000f,"s",v->{settings.setIdleAlphaTime(v);sendRefresh();});

        addToggle(card,L.ghostMode(),
            L.isTr()?"Buton boşta kalınca soluklaşır, dokunulunca normale döner.":"Button fades when idle, returns on touch.",
            settings.isGhostMode(),v->{settings.setGhostMode(v);sendRefresh();ghostExtra.setVisibility(v?View.VISIBLE:View.GONE);});
        card.addView(ghostExtra);

        // Otomatik gizle
        TextView lbl=new TextView(this); lbl.setText(L.autoHide()); lbl.setTextColor(sub());
        lbl.setTextSize(12); lbl.setPadding(0,px(8),0,px(6)); card.addView(lbl);
        String saved=settings.getHidePackages(); int cnt=saved.isEmpty()?0:saved.split(",").length;
        TextView picker=new TextView(this);
        picker.setText(cnt==0?L.selectApp():cnt+L.appsSelected());
        picker.setTextSize(13); picker.setTextColor(cnt==0?sub():accent());
        picker.setPadding(px(14),px(12),px(14),px(12));
        GradientDrawable pbg=new GradientDrawable(); pbg.setCornerRadius(px(12));
        pbg.setColor(t()==1?0xFFF0F0F8:0xFF111122); pbg.setStroke(px(1),border()); picker.setBackground(pbg);
        picker.setOnClickListener(v->{haptic(v);showMultiAppPicker(picker);}); card.addView(picker);
    }

    private void buildGesturesCard() {
        LinearLayout card=makeCard(L.cardGestures(),
            L.isTr()?"Tap, çift tap ve kaydırma hareketlerine aksiyon ata."
                    :"Assign actions to tap, double tap and swipe gestures.");
        String[] lbls={L.singleTap(),L.doubleTap(),L.swipeRight(),L.swipeLeft(),L.swipeUp(),L.swipeDown()};
        int[] vals={settings.getSingleTap(),settings.getDoubleTap(),settings.getSwipeRight(),
                    settings.getSwipeLeft(),settings.getSwipeUp(),settings.getSwipeDown()};
        String[] a=actions();
        for(int i=0;i<6;i++){final int idx=i; addSpinner(card,lbls[i],null,a,vals[i],pos->{
            switch(idx){
                case 0:settings.setSingleTap(pos);break; case 1:settings.setDoubleTap(pos);break;
                case 2:settings.setSwipeRight(pos);break; case 3:settings.setSwipeLeft(pos);break;
                case 4:settings.setSwipeUp(pos);break; case 5:settings.setSwipeDown(pos);break;
            }
            buildCards();
        });}
    }

    private boolean anyGestureIsAssistant() {
        int A=SettingsManager.ACTION_ASSISTANT;
        return settings.getSingleTap()==A||settings.getDoubleTap()==A||
               settings.getSwipeRight()==A||settings.getSwipeLeft()==A||
               settings.getSwipeUp()==A||settings.getSwipeDown()==A||
               settings.getDrawGestureL()==A||settings.getDrawGestureZ()==A;
    }

    private void buildAssistantGestureCard() {
        LinearLayout card=makeCard(L.cardAssistant(),
            L.isTr()?"Harekete atanan asistan aksiyonu hangi uygulamayı açacak?"
                    :"Which app should the Assistant gesture action open?");
        // Seçili uygulama butonu
        String savedPkg=settings.getAssistantApp();
        String btnText;
        if (savedPkg!=null&&!savedPkg.isEmpty()) {
            try {
                btnText=getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(savedPkg,0)).toString();
            } catch (Exception e) { btnText=savedPkg; }
        } else {
            btnText=L.isTr()?"Sistem asistanı (varsayılan)":"System assistant (default)";
        }
        TextView btn=new TextView(this);
        btn.setText(btnText);
        btn.setTextColor(savedPkg!=null&&!savedPkg.isEmpty()?accent():sub());
        btn.setTextSize(13); btn.setPadding(0,px(8),0,px(4));
        btn.setOnClickListener(v->{haptic(v);showAssistantPicker(btn);});
        card.addView(btn);
        // Sıfırla butonu
        if (savedPkg!=null&&!savedPkg.isEmpty()) {
            TextView reset=new TextView(this);
            reset.setText(L.isTr()?"Sıfırla (sistem asistanı)":"Reset (system assistant)");
            reset.setTextColor(0xFFEF5350); reset.setTextSize(12); reset.setPadding(0,px(4),0,0);
            reset.setOnClickListener(v->{haptic(v);settings.setAssistantApp("");buildCards();});
            card.addView(reset);
        }
    }

    private void showAssistantPicker(TextView btn) {
        List<AppItem> apps=getLauncherApps(); int count=apps.size();
        String[] names=new String[count+1];
        names[0]=L.isTr()?"Sistem asistanı (varsayılan)":"System assistant (default)";
        for(int i=0;i<count;i++) names[i+1]=apps.get(i).name;
        new AlertDialog.Builder(this).setTitle(L.assistantApp())
            .setItems(names,(d,w)->{
                if(w==0){
                    settings.setAssistantApp("");
                    btn.setText(names[0]); btn.setTextColor(sub());
                } else {
                    String pkg=apps.get(w-1).pkg;
                    settings.setAssistantApp(pkg);
                    btn.setText(apps.get(w-1).name); btn.setTextColor(accent());
                }
                buildCards();
            }).setNegativeButton(L.cancel(),null).show();
    }

    private void buildDrawCard() {
        LinearLayout card=makeCard(L.cardDraw(),
            L.isTr()?"Butona basarken parmağınızla L veya Z şekli çizin. Aksiyon tetiklenir."
                    :"Draw L or Z shape with your finger while pressing the button to trigger an action.");
        addToggle(card,L.isTr()?"Çizim hareketi aktif":"Draw gesture active",
            L.isTr()?"Bu özelliği açınca şekil çizerek aksiyon tetikleyebilirsin.":"Draw shapes to trigger actions.",
            settings.isDrawGestureEnabled(),v->{settings.setDrawGestureEnabled(v);sendRefresh();buildCards();});
        if (!settings.isDrawGestureEnabled()) return;
        String[] a=actions();
        addSpinner(card,L.lShape(),null,a,settings.getDrawGestureL(),v->{settings.setDrawGestureL(v);buildCards();});
        addSpinner(card,L.zShape(),null,a,settings.getDrawGestureZ(),v->{settings.setDrawGestureZ(v);buildCards();});
    }

    private void buildKeyboardCard() {
        LinearLayout card=makeCard(L.cardKeyboard(),
            L.isTr()?"Klavye açıkken buton otomatik küçülür, kapanınca normale döner. Erişilebilirlik gerekir."
                    :"Button shrinks when keyboard opens and returns to normal when closed. Requires Accessibility.");
        addToggle(card,L.kbShrink(),L.isTr()?"Klavye açıkken otomatik küçültme.":"Auto-shrink when keyboard opens.",
            settings.isKeyboardShrink(),v->{settings.setKeyboardShrink(v);sendRefresh();buildCards();});
        if (!settings.isKeyboardShrink()) return;
        addSlider(card,L.kbSize(),L.isTr()?"Klavye açıkken butonun boyutu.":"Button size when keyboard is open.",
            20,80,settings.getKeyboardShrinkSize(),"dp",v->{settings.setKeyboardShrinkSize(v);sendRefresh();});
    }

    private void buildBatteryCard() {
        LinearLayout card=makeCard(L.cardBattery(),
            L.isTr()?"Pil eşiğin altına düşünce buton kırmızıya döner. Önizleme aşağıda gösterilir."
                    :"Button turns red when battery drops below threshold. Preview shown below.");
        addToggle(card,L.lowBattAlert(),L.isTr()?"Pil uyarısını açar.":"Enables battery alert.",
            settings.isLowBatteryAlert(),v->{settings.setLowBatteryAlert(v);sendRefresh();buildCards();});
        if (!settings.isLowBatteryAlert()) return;
        addSlider(card,L.battThreshold(),L.isTr()?"Bu yüzdenin altında buton kırmızıya döner.":"Button turns red below this percentage.",
            5,40,settings.getLowBatteryThreshold(),"%",v->{settings.setLowBatteryThreshold(v);sendRefresh();});
        // Kırmızı önizleme
        LinearLayout prev=new LinearLayout(this); prev.setOrientation(LinearLayout.HORIZONTAL);
        prev.setGravity(Gravity.CENTER_VERTICAL); prev.setPadding(0,px(8),0,0);
        TextView lbl=new TextView(this); lbl.setText(L.isTr()?"Düşük pil görünümü:":"Low battery preview:");
        lbl.setTextColor(sub()); lbl.setTextSize(12);
        prev.addView(lbl, new LinearLayout.LayoutParams(0,-2,1f));
        prev.addView(makeStylePreview(settings.getButtonStyle(),0xFFFF3333), new LinearLayout.LayoutParams(px(44),px(44)));
        card.addView(prev);
    }

    private void buildNotifCard() {
        LinearLayout card=makeCard(L.cardNotif(),
            L.isTr()?"Okunmamış bildirim varken buton üzerinde küçük kırmızı nokta gösterir. Bildirim erişimi gerekir."
                    :"Shows a red dot on button for unread notifications. Requires notification access.");
        addToggle(card,L.notifBadgeToggle(),L.isTr()?"Bildirim rozeti özelliğini açar.":"Enables notification badge.",
            settings.isNotifBadge(),v->{settings.setNotifBadge(v);sendRefresh();});
        if (!settings.isNotifBadge()) return;
        TextView note=new TextView(this);
        note.setText(L.isTr()
            ?"İzin: Ayarlar → Uygulamalar → Özel erişim → Bildirim erişimi → TouchNav"
            :"Permission: Settings → Apps → Special access → Notification access → TouchNav");
        note.setTextColor(sub()); note.setTextSize(11); note.setPadding(0,px(8),0,0);
        card.addView(note);
    }

    // ── Önizleme çizimleri ─────────────────────────────────────────
    private View makeStylePreview(int style, int color) {
        final int c=color;
        return new View(this){
            private final Paint rP=new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint fP=new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint dP=new Paint(Paint.ANTI_ALIAS_FLAG);
            {setLayerType(LAYER_TYPE_SOFTWARE,null); setBackground(null);
             rP.setStyle(Paint.Style.STROKE); rP.setColor(c);
             fP.setStyle(Paint.Style.FILL);
             dP.setStyle(Paint.Style.STROKE); dP.setColor(c);}
            @Override protected void onDraw(Canvas cv){
                float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-3f;
                if(r<=0) return;
                Path clip=new Path(); clip.addCircle(cx,cy,r,Path.Direction.CW); cv.clipPath(clip);
                switch(style){
                    case 0:{rP.setStrokeWidth(1.8f);rP.setMaskFilter(null);rP.setAlpha(130);cv.drawCircle(cx,cy,r-1,rP);break;}
                    case 1:{fP.setColor(0x22FFFFFF);cv.drawCircle(cx,cy,r,fP);rP.setStrokeWidth(2f);rP.setMaskFilter(null);rP.setAlpha(190);cv.drawCircle(cx,cy,r-1,rP);break;}
                    case 2:{rP.setMaskFilter(new BlurMaskFilter(r*.35f,BlurMaskFilter.Blur.OUTER));rP.setStrokeWidth(2.5f);rP.setAlpha(200);cv.drawCircle(cx,cy,r*.7f,rP);rP.setMaskFilter(null);rP.setStrokeWidth(1.5f);rP.setAlpha(160);cv.drawCircle(cx,cy,r-1,rP);break;}
                    case 3:{float d=r*.3f;dP.setPathEffect(new DashPathEffect(new float[]{d,d*.55f},0));dP.setStrokeWidth(2.2f);dP.setAlpha(190);cv.drawCircle(cx,cy,r-1,dP);break;}
                    case 4:{for(int k=0;k<3;k++){rP.setMaskFilter(new BlurMaskFilter(r*(0.6f-k*.15f),BlurMaskFilter.Blur.NORMAL));rP.setStrokeWidth(3f-k*.5f);rP.setAlpha(200-k*40);cv.drawCircle(cx,cy,r-1,rP);}rP.setMaskFilter(null);rP.setStrokeWidth(1f);rP.setAlpha(255);rP.setColor(0xFFFFFFFF);cv.drawCircle(cx,cy,r-1,rP);break;}
                    case 5:{fP.setColor(Color.argb(40,Color.red(c),Color.green(c),Color.blue(c)));cv.drawCircle(cx,cy,r,fP);rP.setStrokeWidth(2f);rP.setMaskFilter(null);rP.setAlpha(200);cv.drawCircle(cx,cy,r-1,rP);fP.setColor(0x18FFFFFF);cv.drawCircle(cx,cy-r*.18f,r*.42f,fP);break;}
                    case 6:{rP.setMaskFilter(new BlurMaskFilter(r*.22f,BlurMaskFilter.Blur.NORMAL));rP.setStrokeWidth(2f);rP.setAlpha(180);cv.drawCircle(cx,cy,r*.9f,rP);rP.setMaskFilter(null);rP.setAlpha(220);cv.drawCircle(cx,cy,r*.55f,rP);break;}
                    case 7:{fP.setColor(Color.argb(70,Color.red(c),Color.green(c),Color.blue(c)));cv.drawCircle(cx,cy,r,fP);rP.setMaskFilter(null);rP.setStrokeWidth(2f);rP.setAlpha(200);cv.drawCircle(cx,cy,r-1,rP);break;}
                    case 8:{rP.setStyle(Paint.Style.STROKE);rP.setMaskFilter(null);rP.setStrokeWidth(2.5f);rP.setAlpha(255);cv.drawCircle(cx,cy,r*.55f,rP);rP.setStrokeWidth(1.5f);rP.setAlpha(160);cv.drawCircle(cx,cy,r*.78f,rP);rP.setMaskFilter(new BlurMaskFilter(r*.18f,BlurMaskFilter.Blur.NORMAL));rP.setStrokeWidth(1f);rP.setAlpha(90);cv.drawCircle(cx,cy,r-1,rP);break;}
                    case 9:{rP.setStyle(Paint.Style.STROKE);rP.setMaskFilter(null);rP.setStrokeWidth(3f);rP.setAlpha(230);RectF ov=new RectF(cx-r+2f,cy-r+2f,cx+r-2f,cy+r-2f);cv.drawArc(ov,-50f,170f,false,rP);rP.setAlpha(120);cv.drawArc(ov,130f,170f,false,rP);fP.setColor(c);fP.setAlpha(200);cv.drawCircle(cx,cy,r*.1f,fP);break;}
                    case 10:{fP.setColor(c);int dots=12;float dotR=r*.09f;for(int d2=0;d2<dots;d2++){double ang=(2*Math.PI*d2/dots)-Math.PI/2;float dx=(float)(cx+(r-dotR*2)*Math.cos(ang));float dy=(float)(cy+(r-dotR*2)*Math.sin(ang));fP.setAlpha(d2%2==0?230:100);cv.drawCircle(dx,dy,dotR,fP);}break;}
                    case 11:{fP.setColor(Color.argb(50,Color.red(c),Color.green(c),Color.blue(c)));cv.drawCircle(cx,cy,r,fP);rP.setStyle(Paint.Style.STROKE);rP.setMaskFilter(new BlurMaskFilter(r*.55f,BlurMaskFilter.Blur.NORMAL));rP.setStrokeWidth(4f);rP.setAlpha(255);cv.drawCircle(cx,cy,r*.75f,rP);rP.setMaskFilter(null);rP.setStrokeWidth(1.5f);rP.setAlpha(255);rP.setColor(0xFFFFFFFF);cv.drawCircle(cx,cy,r-1,rP);break;}
                    case 12:{rP.setStyle(Paint.Style.STROKE);rP.setMaskFilter(null);rP.setStrokeWidth(1.5f);rP.setAlpha(180);cv.drawCircle(cx,cy,r-1,rP);fP.setColor(c);fP.setAlpha(220);cv.drawCircle(cx,cy,r*.08f,fP);Paint lp2=new Paint(Paint.ANTI_ALIAS_FLAG);lp2.setColor(c);lp2.setAlpha(220);lp2.setStyle(Paint.Style.STROKE);lp2.setStrokeWidth(2f);lp2.setStrokeCap(Paint.Cap.ROUND);float tk=r*.28f;cv.drawLine(cx,cy-r+3f,cx,cy-r+3f+tk,lp2);cv.drawLine(cx,cy+r-3f-tk,cx,cy+r-3f,lp2);cv.drawLine(cx-r+3f,cy,cx-r+3f+tk,cy,lp2);cv.drawLine(cx+r-3f-tk,cy,cx+r-3f,cy,lp2);break;}
                }
            }
        };
    }

    private View makeShapePreview(int shape, int color) {
        final int c=color;
        return new View(this){
            private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            {setLayerType(LAYER_TYPE_SOFTWARE,null);setBackground(null);
             p.setStyle(Paint.Style.STROKE);p.setColor(c);p.setStrokeWidth(2f);p.setAlpha(200);}
            @Override protected void onDraw(Canvas cv){
                float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-4f;
                if(r<=0) return;
                switch(shape){
                    case 0:cv.drawCircle(cx,cy,r,p);break;
                    case 1:cv.drawRoundRect(new RectF(cx-r,cy-r,cx+r,cy+r),r*.35f,r*.35f,p);break;
                    case 2:{Path dp=new Path();float cr=r*.75f;dp.addOval(new RectF(cx-cr,cy-r*.8f,cx+cr,cy+cr*.4f),Path.Direction.CW);dp.moveTo(cx-cr*.55f,cy+cr*.2f);dp.lineTo(cx,cy+r);dp.lineTo(cx+cr*.55f,cy+cr*.2f);dp.close();cv.drawPath(dp,p);break;}
                    case 3:{Path hp=new Path();for(int i=0;i<6;i++){double a=Math.PI/6+i*Math.PI/3;float x=(float)(cx+r*Math.cos(a)),y=(float)(cy+r*Math.sin(a));if(i==0)hp.moveTo(x,y);else hp.lineTo(x,y);}hp.close();cv.drawPath(hp,p);break;}
                }
            }
        };
    }

    // ── Yardımcılar ──────────────────────────────────────────────
    private void sendRefresh() {
        Intent i=new Intent(FloatingService.ACTION_REFRESH);
        i.setPackage(getPackageName()); sendBroadcast(i);
    }

    private void showNewProfileDialog() {
        EditText input=new EditText(this); input.setHint(L.profileName()); input.setPadding(px(16),px(12),px(16),px(12));
        new AlertDialog.Builder(this).setTitle(L.newProfile()).setView(input)
            .setPositiveButton(L.save(),(d,w)->{String name=input.getText().toString().trim();
                if(!name.isEmpty()){profiles.saveProfile(name);settings.setCurrentProfile(name);sendRefresh();buildCards();}
            }).setNegativeButton(L.cancel(),null).show();
    }

    private void toast(String msg){ android.widget.Toast.makeText(this,msg,android.widget.Toast.LENGTH_SHORT).show(); }
    private void haptic(View v){ v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING); }
    private int px(int dp){ return (int)(dp*getResources().getDisplayMetrics().density); }

    interface OnInt  { void on(int v); }
    interface OnBool { void on(boolean v); }
}
