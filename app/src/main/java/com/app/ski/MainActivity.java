package com.app.ski;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.app.ski.core.BuildManager;
import com.app.ski.core.BuildStatusManager;
import com.app.ski.core.ProjectManager;
import com.app.ski.fragments.*;
import com.app.ski.fragments.ApkBuildsFragment;
import com.app.ski.fragments.DesktopBuildsFragment;
import androidx.activity.OnBackPressedCallback;

public class MainActivity extends AppCompatActivity {
    private ProjectManager manager;
    private CodeFragment codeFragment;
    private ProjectsFragment projectsFragment;
    private ConsoleFragment consoleFragment;
    private SettingsFragment settingsFragment;
    private ApkBuildsFragment apkBuildsFragment;
    private DesktopBuildsFragment desktopBuildsFragment;
    private Fragment current;
    private BuildManager buildManager;
    private BuildStatusManager buildStatusManager;

    private LinearLayout bottomBar;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        setContentView(R.layout.activity_main);

        bottomBar = findViewById(R.id.bottomBar);

        View root=findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
            Insets i=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            View c=findViewById(R.id.fragmentContainer);
            c.setPadding(c.getPaddingLeft(),i.top,c.getPaddingRight(),0);
            return insets;
        });

        manager=new ProjectManager(this);

        //fragments initialization
        codeFragment =new CodeFragment();
        projectsFragment =new ProjectsFragment();
        consoleFragment =new ConsoleFragment();
        settingsFragment =new SettingsFragment();
        apkBuildsFragment = new ApkBuildsFragment();
        desktopBuildsFragment = new DesktopBuildsFragment();


        buildManager = new BuildManager(this);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, codeFragment,"CODE")
                .add(R.id.fragmentContainer, projectsFragment,"PROJECTS").hide(projectsFragment)
                .add(R.id.fragmentContainer, consoleFragment,"CONSOLE").hide(consoleFragment)
                .add(R.id.fragmentContainer, settingsFragment,"SETTINGS").hide(settingsFragment)
                .add(R.id.fragmentContainer, apkBuildsFragment, "apk_builds")
                .add(R.id.fragmentContainer, desktopBuildsFragment, "desktop_builds")
                .hide(apkBuildsFragment)
                .hide(desktopBuildsFragment)
                .commit();

        current= codeFragment;
        findViewById(R.id.navCode).setOnClickListener(v->show(codeFragment));
        findViewById(R.id.navProjects).setOnClickListener(v->show(projectsFragment));
        findViewById(R.id.navConsole).setOnClickListener(v->show(consoleFragment));
        findViewById(R.id.navSettings).setOnClickListener(v->show(settingsFragment));

        doInitialize();
        initializeCallback();


    }


    public void doInitialize(){
        buildStatusManager =
                new BuildStatusManager();
    }



    public ProjectManager getProjectManager(){return manager;}

    public BuildManager getBuildManager() {
        return buildManager;
    }
    public BuildStatusManager getBuildStatusManager() {

        return buildStatusManager;
    }

    public void show(Fragment f) {

        if (f == current) {
            return;
        }


        getSupportFragmentManager()
                .beginTransaction()
                .hide(current)
                .show(f)
                .commit();


        current = f;


        /*
         * Normal app fragments
         * show bottom navigation.
         */

        if (f == codeFragment ||
                f == projectsFragment ||
                f == consoleFragment ||
                f == settingsFragment) {

            bottomBar.setVisibility(
                    View.VISIBLE
            );
        }


        if (f == codeFragment) {

            codeFragment.refreshProject();
        }


        if (f == projectsFragment) {

            projectsFragment.refreshProjects();
        }
    }
    public void openApkBuilds() {

        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();

        transaction.hide(codeFragment);
        transaction.hide(projectsFragment);
        transaction.hide(consoleFragment);
        transaction.hide(settingsFragment);
        transaction.hide(desktopBuildsFragment);

        transaction.show(apkBuildsFragment);

        transaction.commit();

        current = apkBuildsFragment;

        // Hide bottom navigation
        bottomBar.setVisibility(View.GONE);
    }

    public void openDesktopBuilds() {

        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();

        transaction.hide(codeFragment);
        transaction.hide(projectsFragment);
        transaction.hide(consoleFragment);
        transaction.hide(settingsFragment);
        transaction.hide(apkBuildsFragment);

        transaction.show(desktopBuildsFragment);

        transaction.commit();

        current = desktopBuildsFragment;

        // Hide bottom navigation
        bottomBar.setVisibility(View.GONE);
    }

    public void openCode(){
        show(codeFragment);
    }


    //Used for back press

    public void initializeCallback(){
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        if (current == apkBuildsFragment ||
                                current == desktopBuildsFragment) {

                            // Show bottom navigation again
                            bottomBar.setVisibility(
                                    View.VISIBLE
                            );

                            // Return to Projects
                            show(projectsFragment);

                            return;
                        }

                        // Normal Android back behavior
                        setEnabled(false);

                        getOnBackPressedDispatcher()
                                .onBackPressed();

                        setEnabled(true);
                    }
                }
        );
    }
}
