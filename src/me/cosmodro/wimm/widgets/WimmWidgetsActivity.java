package me.cosmodro.wimm.widgets;

import com.wimm.framework.app.LauncherActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class WimmWidgetsActivity extends LauncherActivity {
    protected static final String TAG = "WimmWidgetsActivity";

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        NumberPicker picker = (NumberPicker) this.findViewById(R.id.Picker1);
        picker.setLongClickHandler(new View.OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
        		Toast.makeText(WimmWidgetsActivity.this,"long press", Toast.LENGTH_SHORT).show();
				return false;
			}
		});
        picker.setOnValueChangedListener(new OnValueChangedListener(){
        	public void onValueChanged(int newval){
        		Log.d(TAG, "new value: "+newval);
        	}
        });
    }
    
    
}