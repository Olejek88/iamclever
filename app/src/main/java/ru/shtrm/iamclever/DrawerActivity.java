package ru.shtrm.iamclever;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.RecyclerViewCacheUtil;
import com.mikepenz.octicons_typeface_library.Octicons;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ru.shtrm.iamclever.db.adapters.ProfilesDBAdapter;
import ru.shtrm.iamclever.db.tables.Profiles;
import ru.shtrm.iamclever.fragments.FragmentAddUser;
import ru.shtrm.iamclever.fragments.FragmentEditUser;
import ru.shtrm.iamclever.fragments.FragmentIntro;
import ru.shtrm.iamclever.fragments.FragmentNewWords;
import ru.shtrm.iamclever.fragments.FragmentQuestion;
import ru.shtrm.iamclever.fragments.FragmentSettings;

public class DrawerActivity extends AppCompatActivity {
    private static final int PROFILE_ADD = 1;
    private static final int PROFILE_SETTING = 2;
    private static final int MAX_USER_PROFILE = 10;

    private boolean isLogged = false;
    private boolean isActive = false;
    private int ActiveUserID;
    private Timer tShow = new Timer();
    private Timer tQuest = new Timer();

    //save our header or result
    public AccountHeader headerResult = null;
    private Drawer result = null;
    private boolean opened = false;
    private ArrayList<IProfile> profile;
    private List<Profiles> profilesList;
    private int cnt = 0;
    private int users_id[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_dark_toolbar);
        profile = new ArrayList<IProfile>();
        users_id = new int[MAX_USER_PROFILE];

        if (!initDB()) finish();

        // Handle Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundResource(R.drawable.header);
        toolbar.setSubtitle("Level up you skills");

        // Create the AccountHeader
        headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .addProfiles(
                        new ProfileSettingDrawerItem().withName("Добавить аккаунт").withDescription("Добавить новый профиль пользователя").withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_insert_emoticon).actionBar().paddingDp(5).colorRes(R.color.material_drawer_primary_text)).withIdentifier(PROFILE_ADD),
                        new ProfileSettingDrawerItem().withName("Редактировать профиль").withDescription("Редактировать профиль пользователя").withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_edit).actionBar().paddingDp(5).colorRes(R.color.material_drawer_primary_text)).withIdentifier(PROFILE_SETTING)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        if (profile instanceof IDrawerItem && ((IDrawerItem) profile).getIdentifier() == PROFILE_ADD) {
                            Fragment f = FragmentAddUser.newInstance("AddProfile");
                            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();
                        }
                        if (profile instanceof IDrawerItem && ((IDrawerItem) profile).getIdentifier() == PROFILE_SETTING) {
                            Fragment f = FragmentEditUser.newInstance("EditProfile");
                            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();
                        }
                        //false if you have not consumed the event and it should close the drawer
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        fillProfileList();

        ProfilesDBAdapter users = new ProfilesDBAdapter(
                new IDatabaseContext(getApplicationContext()));
        Profiles user = users.getActiveUser();
        if (user != null) {
            ActiveUserID = (int)user.getId();
            if (user.getActive() > 0)
                isActive = true;
            else
                isActive = false;
        }

        //Create the drawer
        result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withHasStableIds(true)
                .withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Настройки").withDescription("Выбор иностранных языков для изучения").withIcon(GoogleMaterial.Icon.gmd_book).withIdentifier(1).withSelectable(false),
                        new PrimaryDrawerItem().withName("Статистика").withDescription("Текущий счет и анализ успехов").withIcon(FontAwesome.Icon.faw_list).withIdentifier(2).withSelectable(false),
                        new PrimaryDrawerItem().withName("Рейтинг").withDescription("Рейтинг среди изучающих").withIcon(FontAwesome.Icon.faw_bar_chart).withIdentifier(3).withSelectable(false),
                        new PrimaryDrawerItem().withName("Прогресс успеха").withDescription("Динамика изучения языков").withIcon(FontAwesome.Icon.faw_calendar).withIdentifier(4).withSelectable(false),
                        new PrimaryDrawerItem().withName("Обновить базу вопросов").withDescription("Загрузить с интернет-сервера").withIcon(FontAwesome.Icon.faw_download).withIdentifier(5).withSelectable(false),
                        // TODO вариант добавления новых слов
                        // TODO настройки связи с сервером
                        new DividerDrawerItem(),
                        new SecondarySwitchDrawerItem().withName("On-line профиль").withIcon(Octicons.Icon.oct_tools).withChecked(true).withOnCheckedChangeListener(onCheckedChangeListener).withIdentifier(11),
                        new SecondarySwitchDrawerItem().withName("Сделать паузу в обучении").withIcon(Octicons.Icon.oct_tools).withChecked(isActive).withOnCheckedChangeListener(onCheckedChangeListener).withIdentifier(12),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName("Выход").withDescription("Закрыть программу").withIcon(FontAwesome.Icon.faw_undo).withIdentifier(14).withSelectable(false)
                ) // add the items we want to use with our Drawer
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        //check if the drawerItem is set.
                        //there are different reasons for the drawerItem to be null
                        //--> click on the header
                        //--> click on the footer
                        //those items don't contain a drawerItem

                        if (drawerItem != null) {
                            Intent intent = null;
                            if (drawerItem.getIdentifier() == 1) {
                                Fragment f = FragmentSettings.newInstance("User Settings");
                                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();
                            } else if (drawerItem.getIdentifier() == 2) {
                            }
                            else if (drawerItem.getIdentifier() == 14) {
                                System.exit(0);
                            }
                            if (intent != null) {
                                DrawerActivity.this.startActivity(intent);
                            }
                        }

                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .withShowDrawerOnFirstLaunch(true)
                .build();

        //if you have many different types of DrawerItems you can magically pre-cache those items to get a better scroll performance
        //make sure to init the cache after the DrawerBuilder was created as this will first clear the cache to make sure no old elements are in
        RecyclerViewCacheUtil.getInstance().withCacheSize(2).init(result);

        //only set the active selection or active profile if we do not recreate the activity
        if (savedInstanceState == null) {
            // set the selection to the item with the identifier 11
            result.setSelection(21, false);

            //set the active profile
            if (profile.size()>0)   {
                for (cnt=0;cnt<profile.size();cnt++) {
                        if (ActiveUserID>0 && profile.get(cnt).getIdentifier()==ActiveUserID)
                            headerResult.setActiveProfile(profile.get(cnt));
                }
            }
        }

        Fragment f = FragmentIntro.newInstance("Demo");
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();

        if (ActiveUserID>0) {
            isLogged = true;
        } else {
            Toast.makeText(getApplicationContext(),
                    "Пожалуйста выберите профиль", Toast.LENGTH_LONG).show();
        }

        tShow.schedule(new TimerTask(){
            @Override
            public void run(){
                if (isActive) {
                    Fragment f = FragmentNewWords.newInstance("Lesson");
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();
                }
            }
        },10,150*1000);

        tQuest.schedule(new TimerTask(){
            @Override
            public void run(){
                if (isActive) {
                    //startQuestionDialog();
                }
            }
        },10,150*1000);

    }

    public void startQuestionDialog(){
        this.runOnUiThread(startDialogFunction);
    }
    private Runnable startDialogFunction = new Runnable() {
        @Override
        public void run() {
            Message msg = new Message();
            handler.sendMessage(msg);
        }
    };
    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
            if (drawerItem.getIdentifier() == 12) {
                if (isChecked)
                    isActive = true;
                else
                    isActive = false;
            }
        }
    };

    private Drawer.OnDrawerItemClickListener onDrawerItemClickListener = new Drawer.OnDrawerItemClickListener() {
        @Override
        public boolean onItemClick(View view, int i, IDrawerItem iDrawerItem) {
            ProfilesDBAdapter profileDBAdapter = new ProfilesDBAdapter(
                    new IDatabaseContext(getApplicationContext()));
            if (i>2) {
                headerResult.setActiveProfile(profile.get(i - 3));
                profileDBAdapter.setActiveUser(profilesList.get(i - 3).getLogin());
                profilesList.get(i - 3).setActive(1);
            }
            return false;
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = result.saveInstanceState(outState);
        //add the values which need to be saved from the accountHeader to the bundle
        outState = headerResult.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            moveTaskToBack(true);
            //super.onBackPressed();
        }
    }

    public void addProfile (Profiles item)
        {
            IProfile new_profile;
            String target_filename = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Android" + File.separator + "data" + File.separator + getPackageName() + File.separator + "img" + File.separator + item.getImage();
            File imgFile = new  File(target_filename);
            if(imgFile.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                new_profile = new ProfileDrawerItem().withName(item.getName()).withEmail(item.getLogin()).withIcon(myBitmap).withIdentifier((int)item.getId()).withOnDrawerItemClickListener(onDrawerItemClickListener);
            }
            else
                new_profile = new ProfileDrawerItem().withName(item.getName()).withEmail(item.getLogin()).withIcon(R.drawable.olejek).withIdentifier((int)item.getId()).withOnDrawerItemClickListener(onDrawerItemClickListener);
            profile.add(new_profile);
            headerResult.addProfile(new_profile, headerResult.getProfiles().size());
        }

    public void refreshProfileList () {
        ProfilesDBAdapter profileDBAdapter = new ProfilesDBAdapter(
                new IDatabaseContext(getApplicationContext()));
        profilesList = profileDBAdapter.getAllItems();
        cnt=0;
        for (Profiles item : profilesList) {
            users_id[cnt]=(int)item.getId();
            cnt = cnt + 1;
            if (cnt>MAX_USER_PROFILE) break;
        }
    }

    public void deleteProfile (int id)    {
        for (cnt=0;cnt<profile.size();cnt++) {
            if (users_id[cnt]==id) {
                profile.remove(cnt);
                headerResult.removeProfile(cnt);
            }
        }
        refreshProfileList();
    }

    public void fillProfileList ()    {
        ProfilesDBAdapter profileDBAdapter = new ProfilesDBAdapter(
                new IDatabaseContext(getApplicationContext()));
        profilesList = profileDBAdapter.getAllItems();

        cnt=0;
        for (Profiles item : profilesList) {
            addProfile(item);
            users_id[cnt]=(int)item.getId();
            cnt = cnt + 1;
            if (cnt>MAX_USER_PROFILE) break;
        }
    }

    public boolean initDB() {
        boolean success = false;
        DatabaseHelper helper = null;
        // создаём базу данных, в качестве контекста передаём свой, с
        // переопределёнными путями к базе
        try {
            helper = DatabaseHelper.getInstance(new IDatabaseContext(
                    getApplicationContext()));
            helper.isDBActual();
            success = true;
        } catch (Exception e) {
            Toast toast = Toast.makeText(this,
                    "Не удалось открыть/обновить базу данных!",
                    Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
        }
        return success;
    }

    protected Handler handler = new Handler(){
        public void handleMessage(Message m){
            //Intent intent = new Intent (DrawerActivity.this, DrawerActivity.class);
            //startActivity(intent);
            //intent = new Intent (DrawerActivity.this, QuestionDialog.class);
            //startActivity(intent);
            Fragment f = FragmentQuestion.newInstance("Question");
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();
        }
    };
}