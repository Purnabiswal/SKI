package com.app.ski.fragments;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.app.ski.R;

public class ConsoleFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,ViewGroup c,Bundle b){
        return i.inflate(R.layout.fragment_console,c,false);
    }
    @Override public void onViewCreated(@NonNull View v,Bundle b){
        ((TextView)v.findViewById(R.id.consoleText)).setText("SKI Console\n────────────\nReady.\nUse Code → Run to start the local server.");
    }
}
