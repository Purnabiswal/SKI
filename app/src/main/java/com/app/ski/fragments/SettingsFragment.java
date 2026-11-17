package com.app.ski.fragments;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.app.ski.R;

public class SettingsFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,ViewGroup c,Bundle b){
        return i.inflate(R.layout.fragment_settings,c,false);
    }
}
